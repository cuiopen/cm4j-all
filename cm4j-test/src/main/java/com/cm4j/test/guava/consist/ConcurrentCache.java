package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.loader.CacheValueLoader;
import com.cm4j.test.guava.consist.loader.PrefixMappping;
import com.cm4j.test.guava.consist.queue.AQueueEntry;
import com.cm4j.test.guava.consist.queue.FIFOAccessQueue;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <pre>
 * 读写分离：
 * 读取：{@link ConcurrentCache}提供get or load、put、expire操作
 *
 * 写入：是由{@link CacheEntry#changeDbState(DBState)}控制对象状态
 * 同时{@link ConcurrentCache}独立维护了一份写入队列，独立于缓存操作
 *
 * 使用流程：
 * 1.定义缓存描述信息{@link com.cm4j.test.guava.consist.loader.CacheDefiniens}
 * 2.定义映射{@link PrefixMappping}
 * </pre>
 *
 * @author Yang.hao
 * @since 2013-1-30 上午11:25:47
 */
public class ConcurrentCache {

    final Constants constants = new Constants();

    private static class HOLDER {
        private static final ConcurrentCache instance = new ConcurrentCache(new CacheValueLoader());
    }

    public static ConcurrentCache getInstance() {
        return HOLDER.instance;
    }

    /* ---------------- Fields -------------- */
    final CacheLoader<String, AbsReference> loader;
    final int segmentMask;
    final int segmentShift;
    final Segment[] segments;

    private final ConcurrentLinkedQueue<CacheEntryInUpdateQueue> updateQueue;
    private final ScheduledExecutorService service;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /* ---------------- Small Utilities -------------- */
    private static int rehash(int h) {
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    final Segment segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    /* ---------------- Inner Classes -------------- */
    private final static class HashEntry extends AQueueEntry<AbsReference> {
        // HashEntery内对象的final不变性来降低读操作对加锁的需求
        final String key;
        final int hash;
        final HashEntry next;
        volatile AbsReference value;

        HashEntry(String key, int hash, HashEntry next, AbsReference value) {
            super(value);
            this.key = key;
            this.hash = hash;
            this.next = next;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public int getHash() {
            return hash;
        }

        public HashEntry getNext() {
            return next;
        }

        static final AtomicReferenceArray<HashEntry> newArray(int i) {
            return new AtomicReferenceArray<HashEntry>(i);
        }

        volatile long accessTime = Long.MAX_VALUE;

        public long getAccessTime() {
            return accessTime;
        }

        public void setAccessTime(long time) {
            this.accessTime = time;
        }
    }

    static final class Segment extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        // 作为位操作的mask，必须是(2^n)-1
        static final int DRAIN_THRESHOLD = 0x3F;
        final ConcurrentCache map;

        transient volatile int count;
        transient int modCount;
        transient int threshold;
        transient volatile AtomicReferenceArray<HashEntry> table;
        final float loadFactor;

        // 缓存读取，写入的对象都需要放入recencyQueue
        final Queue<HashEntry> recencyQueue = new ConcurrentLinkedQueue<HashEntry>();
        // 真正中缓存的访问顺序
        // accessQueue的大小应该与缓存的size一样大
        final FIFOAccessQueue<HashEntry> accessQueue = new FIFOAccessQueue();
        final AtomicInteger readCount = new AtomicInteger();

        Segment(ConcurrentCache map, int initialCapacity, float lf) {
            this.map = map;
            loadFactor = lf;
            setTable(HashEntry.newArray(initialCapacity));
        }

        static final Segment[] newArray(int i) {
            return new Segment[i];
        }

        void setTable(AtomicReferenceArray<HashEntry> newTable) {
            threshold = (int) (newTable.length() * loadFactor);
            table = newTable;
        }

        HashEntry getFirst(int hash) {
            AtomicReferenceArray<HashEntry> tab = table;
            return tab.get(hash & (tab.length() - 1));
        }

        HashEntry getEntry(String key, int hash) {
            for (HashEntry e = getFirst(hash); e != null; e = e.next) {
                if (e.hash == hash && key.equals(e.key)) {
                    return e;
                }
            }
            return null;
        }

        AbsReference getLiveValue(String key, int hash, long now) {
            HashEntry e = getLiveEntry(key, hash, now);
            if (e != null) {
                return e.getValue();
            }
            return null;
        }

        /**
         * 查找存活的Entry，包含Persist检测
         *
         * @param key
         * @param hash
         * @param now
         * @return
         */
        HashEntry getLiveEntry(String key, int hash, long now) {
            HashEntry e = getEntry(key, hash);
            if (e == null) {
                return null;
            } else if (map.isExpired(e, now)) {
                // 如果状态不是P，则会延迟生命周期
                tryExpireEntries(now);
                // 非精准查询，如果延长生命周期，这里依然返回null，get()调用时需在有锁情况下做二次检测
                if (e.getValue().isAllPersist()) {
                    return null;
                }
            }
            return e;
        }

        AbsReference get(String key, int hash, CacheLoader<String, AbsReference> loader, boolean isLoad) {
            final StopWatch watch = new Slf4JStopWatch();
            try {
                if (count != 0) { // read-volatile
                    HashEntry e = getEntry(key, hash);
                    if (e != null) {
                        // 这里只是一次无锁情况的快速尝试查询，如果未查询到，会在有锁情况下再查一次
                        AbsReference value = getLiveValue(key, hash, now());
                        watch.lap("cache.getLiveValue()");
                        if (value != null) {
                            recordAccess(e);
                            return value;
                        }
                    }
                }
                if (isLoad) {
                    // at this point e is either null or expired;
                    AbsReference ref = lockedGetOrLoad(key, hash, loader);
                    watch.lap("cache.lockedGetOrLoad()");
                    return ref;
                }
            } finally {
                postReadCleanup();
                watch.stop("cache.get()");
            }
            return null;
        }

        AbsReference lockedGetOrLoad(String key, int hash, CacheLoader<String, AbsReference> loader) {
            HashEntry e;
            AbsReference value;

            lock();
            try {
                // re-read ticker once inside the lock
                long now = now();
                preWriteCleanup(now);

                int newCount = this.count - 1;
                AtomicReferenceArray<HashEntry> table = this.table;
                int index = hash & (table.length() - 1);
                HashEntry first = table.get(index);

                for (e = first; e != null; e = e.next) {
                    String entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null && entryKey.equals(key)) {
                        value = e.getValue();

                        if (value != null && !(map.isExpired(e, now) && !value.isAllPersist())) {
                            recordAccess(e);
                            return value;
                        }

                        // immediately reuse invalid entries
                        accessQueue.remove(e);
                        this.count = newCount; // write-volatile
                        break;
                    }
                }

                // 获取且保存
                StopWatch watch = new Slf4JStopWatch();
                value = loader.load(key);
                watch.stop("cache.loadFromDB()");

                if (value != null) {
                    put(key, hash, value, false);
                } else {
                    map.logger.debug("cache[{}] not found", key);
                }
                return value;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        AbsReference refresh(String key, int hash, CacheLoader<String, AbsReference> loader) {
            lock();
            try {
                // re-read ticker once inside the lock
                long now = now();
                preWriteCleanup(now);

                // 获取且保存
                StopWatch watch = new Slf4JStopWatch();
                AbsReference value = loader.load(key);
                watch.stop("cache.loadFromDB()");

                if (value != null) {
                    put(key, hash, value, false);
                } else {
                    map.logger.debug("cache[{}] not found", key);
                }
                return value;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        boolean containsKey(String key, int hash) {
            if (count != 0) { // read-volatile
                long now = now();

                lock();
                try {
                    HashEntry e = getLiveEntry(key, hash, now);
                    return e != null;
                } finally {
                    unlock();
                }
            }
            return false;
        }

        AbsReference put(String key, int hash, AbsReference value, boolean onlyIfAbsent) {
            final StopWatch watch = new Slf4JStopWatch();
            lock();
            try {
                preWriteCleanup(now());

                int c = count;
                if (c++ > threshold) { // ensure capacity
                    rehash();
                }
                // 为什么要创建tab指向table
                // 这是因为table变量是volatile类型，多次读取volatile类型的开销要比非volatile开销要大，而且编译器也无法优化
                AtomicReferenceArray<HashEntry> tab = table;
                int index = hash & (tab.length() - 1);
                HashEntry first = tab.get(index);
                HashEntry e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                AbsReference oldValue;
                if (e != null) {
                    oldValue = e.value;
                    if (!onlyIfAbsent) {
                        e.value = value;
                        recordAccess(e);
                    }
                } else {
                    oldValue = null;
                    ++modCount;
                    e = new HashEntry(key, hash, first, value);
                    tab.set(index, e);
                    count = c; // write-volatile

                    accessQueue.add(e);
                    recordAccess(e);
                }

                // 在put的时候对value设置所属key
                value.setAttachedKey(key);

                // 返回旧值
                return oldValue;
            } finally {
                unlock();
                postWriteCleanup();
                watch.stop("cache.put()");
            }
        }

        void rehash() {
            StopWatch watch = new Slf4JStopWatch();
            AtomicReferenceArray<HashEntry> oldTable = table;
            int oldCapacity = oldTable.length();
            if (oldCapacity >= Constants.MAXIMUM_CAPACITY)
                return;

            int newCount = count;
            AtomicReferenceArray<HashEntry> newTable = HashEntry.newArray(oldCapacity << 1);
            threshold = (int) (newTable.length() * loadFactor);
            int newMask = newTable.length() - 1;
            for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
                // We need to guarantee that any existing reads of old Map can
                // proceed. So we cannot yet null out each bin.
                HashEntry head = oldTable.get(oldIndex);

                if (head != null) {
                    HashEntry next = head.next;
                    int headIndex = head.getHash() & newMask;

                    // next为空代表这个链表就只有一个元素，直接把这个元素设置到新数组中
                    if (next == null) {
                        newTable.set(headIndex, head);
                    } else {
                        // 有多个元素时
                        HashEntry tail = head;
                        int tailIndex = headIndex;
                        // 从head开始，一直到链条末尾，找到最后一个下标与head下标不一致的元素
                        for (HashEntry e = next; e != null; e = e.next) {
                            int newIndex = e.getHash() & newMask;
                            // 这里的找到后没有退出循环，继续找下一个不一致的下标
                            if (newIndex != tailIndex) {
                                tailIndex = newIndex;
                                tail = e;
                            }
                        }
                        // 找到的是最后一个不一致的，所以tail往后的都是一致的下标
                        newTable.set(tailIndex, tail);

                        // 在这之前的元素下标有可能一样，也有可能不一样，所以把前面的元素重新复制一遍放到新数组中
                        for (HashEntry e = head; e != tail; e = e.next) {
                            int newIndex = e.getHash() & newMask;
                            HashEntry newNext = newTable.get(newIndex);
                            HashEntry newFirst = copyEntry(e, newNext);
                            if (newFirst != null) {
                                newTable.set(newIndex, newFirst);
                            } else {
                                accessQueue.remove(e);
                                newCount--;
                            }
                        }
                    }
                }
            }
            table = newTable;
            this.count = newCount;
            watch.stop("cache.rehash()");
        }

        /**
         * Remove; match on key only if value null, else match both.
         */
        AbsReference remove(String key, int hash, AbsReference value) {
            lock();
            try {
                preWriteCleanup(now());

                HashEntry e = getEntry(key, hash);
                AbsReference oldValue = null;
                if (e != null) {
                    AbsReference v = e.value;
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        removeEntry(e, e.getHash());
                    }
                }
                return oldValue;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        void clear() {
            if (count != 0) {
                lock();
                try {
                    AtomicReferenceArray<HashEntry> tab = table;
                    for (int i = 0; i < tab.length(); i++)
                        tab.set(i, null);

                    accessQueue.clear();
                    ++modCount;
                    count = 0; // write-volatile
                } finally {
                    unlock();
                }
            }
        }

        <R> R doInSegmentUnderLock(String key, int hash, SegmentLockHandler<R> handler) {
            lock();
            preWriteCleanup(now());
            try {
                HashEntry e = getFirst(hash);
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                return handler.doInSegmentUnderLock(this, e);
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        // expiration，过期相关业务

        /**
         * Cleanup expired entries when the lock is available.
         */
        void tryExpireEntries(long now) {
            if (tryLock()) {
                try {
                    expireEntries(now);
                } finally {
                    unlock();
                }
            }
        }

        // 调用方都有锁
        void expireEntries(long now) {
            drainRecencyQueue();

            // 如果缓存isExpire但是没有保存db，这时会不会死循环？
            // 应该会的，因为peek每次都是获取的第一个。如果第一个没移除下次循环还是它，就死循环了
            // 所以直接把缓存的生命周期后移，同时如果在遍历时再碰到此对象，则退出遍历
            HashEntry e;
            HashEntry firstEntry = null;
            while ((e = accessQueue.peek()) != null && map.isExpired(e, now)) {
                if (firstEntry == null) {
                    // 第一次循环，设置first
                    firstEntry = e;
                } else if (e == firstEntry) {
                    // 第N此循环，又碰到e，代表已经完成了一次循环，这样可防止无限循环
                    break;
                }

                // accessQueue大小应该与count一致
                Preconditions.checkArgument(accessQueue.size() == count, "个数不一致：accessQueue:" + accessQueue.size() + ",count:" + count);

                if (e.getValue().isAllPersist()) {
                    removeEntry(e, e.getHash());
                } else {
                    recordAccess(e);
                }
            }
        }

        void removeEntry(HashEntry entry, int hash) {
            int c = count - 1;
            AtomicReferenceArray<HashEntry> tab = table;
            int index = hash & (tab.length() - 1);
            HashEntry first = tab.get(index);

            for (HashEntry e = first; e != null; e = e.next) {
                if (e == entry) {
                    // 是否remove后面的，再remove前面的 access 是copied 的？
                    ++modCount;
                    // 从链表1中删除元素entry，且返回链表2的头节点
                    HashEntry newFirst = removeEntryFromChain(first, entry);
                    // 将链表2的新的头节点设置到segment的table中
                    tab.set(index, newFirst);
                    count = c; // write-volatile

                    map.logger.warn("缓存[{}]被移除", entry.key);
                    return;
                }
            }
        }

        HashEntry removeEntryFromChain(HashEntry first, HashEntry entry) {
            if (!accessQueue.contains(entry)) {
                throw new RuntimeException("被移除数据不在accessQueue中,key:" + entry.key);
            }
            HashEntry newFirst = entry.next;
            // 从链条1的头节点first开始迭代到需要删除的节点entry
            for (HashEntry e = first; e != entry; e = e.next) {
                // 拷贝e的属性，并作为链条2的临时头节点
                newFirst = copyEntry(e, newFirst);
            }
            // 从队列移除
            accessQueue.remove(entry);
            return newFirst;
        }

        /**
         * 返回新的对象，拷贝original的数据，并设置next为newNext
         *
         * @param original
         * @param newNext
         * @return
         */
        HashEntry copyEntry(HashEntry original, HashEntry newNext) {
            HashEntry newEntry = new HashEntry(original.getKey(), original.getHash(), newNext, original.value);
            copyAccessEntry(original, newEntry);
            return newEntry;
        }

        void copyAccessEntry(HashEntry original, HashEntry newEntry) {
            newEntry.setAccessTime(original.getAccessTime());

            accessQueue.connectAccessOrder(original.getPreviousInAccessQueue(), newEntry);
            accessQueue.connectAccessOrder(newEntry, original.getNextInAccessQueue());

            accessQueue.nullifyAccessOrder(original);
        }

        /**
         * Performs routine cleanup following a read. Normally cleanup happens
         * during writes. If cleanup is not observed after a sufficient number
         * of reads, try cleaning up from the read thread.
         */
        void postReadCleanup() {
            if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
                cleanUp();
            }
        }

        /**
         * Performs routine cleanup prior to executing a write. This should be
         * called every time a write thread acquires the segment lock,
         * immediately after acquiring the lock.
         * <p/>
         * <p/>
         * Post-condition: expireEntries has been run.
         */
        void preWriteCleanup(long now) {
            runLockedCleanup(now);
        }

        /**
         * Performs routine cleanup following a write.
         */
        void postWriteCleanup() {
            runUnlockedCleanup();
        }

        void cleanUp() {
            runLockedCleanup(now());
            runUnlockedCleanup();
        }

        void runLockedCleanup(long now) {
            tryExpireEntries(now);
        }

        void runUnlockedCleanup() {
            // REMOVE 通知。。。
            if (!isHeldByCurrentThread()) {
                // map.processPendingNotifications();
            }
        }

        /**
         * 1.记录访问时间<br>
         * 2.增加到访问对列的尾部
         */
        void recordAccess(HashEntry e) {
            e.setAccessTime(now());
            recencyQueue.add(e);
        }

        void drainRecencyQueue() {
            HashEntry e;
            while ((e = recencyQueue.poll()) != null) {
                // An entry may be in the recency queue despite it being removed from
                // the map . This can occur when the entry was concurrently read while a
                // writer is removing it from the segment or after a clear has removed
                // all of the segment's entries.

                // accessQueue有存在对象，则把他加入到accessQueue的尾部
                // accessQueue的add() 另外一个地方在put()的时候
                if (accessQueue.contains(e)) {
                    accessQueue.add(e);
                }
            }
        }
    }

	/* ---------------- Public operations -------------- */

    private ConcurrentCache(CacheLoader<String, AbsReference> loader) {
        this(Constants.DEFAULT_INITIAL_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, Constants.DEFAULT_CONCURRENCY_LEVEL, loader);
    }

    private ConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel,
                            CacheLoader<String, AbsReference> loader) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0 || loader == null)
            throw new IllegalArgumentException();

        this.loader = loader;

        if (concurrencyLevel > Constants.MAX_SEGMENTS)
            concurrencyLevel = Constants.MAX_SEGMENTS;

        // Find power-of-two sizes best matching arguments
        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrencyLevel) {
            ++sshift;
            ssize <<= 1;
        }
        segmentShift = 32 - sshift;
        segmentMask = ssize - 1;
        this.segments = Segment.newArray(ssize);

        if (initialCapacity > Constants.MAXIMUM_CAPACITY)
            initialCapacity = Constants.MAXIMUM_CAPACITY;
        int c = initialCapacity / ssize;
        if (c * ssize < initialCapacity)
            ++c;
        int cap = 1;
        while (cap < c)
            cap <<= 1;

        for (int i = 0; i < this.segments.length; ++i) {
            this.segments[i] = new Segment(this, cap, loadFactor);
        }

        // 更新队列
        updateQueue = new ConcurrentLinkedQueue<CacheEntryInUpdateQueue>();

        // 定时处理器
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                consumeUpdateQueue(false);
            }
        }, 1, Constants.CHECK_UPDATE_QUEUE_INTERVAL, TimeUnit.SECONDS);
    }

    public int size() {
        final Segment[] segments = this.segments;
        long sum = 0;
        long check = 0;
        int[] mc = new int[segments.length];
        // Try a few times to get accurate count. On failure due to
        // continuous async changes in table, resort to locking.
        for (int k = 0; k < Constants.RETRIES_BEFORE_LOCK; ++k) {
            check = 0;
            sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    check += segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        check = -1; // force retry
                        break;
                    }
                }
            }
            if (check == sum)
                break;
        }
        if (check != sum) { // Resort to locking all segments
            sum = 0;
            for (int i = 0; i < segments.length; ++i)
                segments[i].lock();
            for (int i = 0; i < segments.length; ++i)
                sum += segments[i].count;
            for (int i = 0; i < segments.length; ++i)
                segments[i].unlock();
        }
        if (sum > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else
            return (int) sum;
    }

    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V get(CacheDefiniens<V> desc) {
        String key = desc.getKey();
        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).get(key, hash, loader, true);
    }

    /**
     * 存在即获取
     *
     * @return 不存在时返回的reference为null
     */
    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V getIfPresent(CacheDefiniens<V> desc) {
        String key = desc.getKey();
        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).get(key, hash, loader, false);
    }

    /**
     * 重新从db加载数据
     *
     * @param <V>
     * @param desc
     * @return
     */
    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V refresh(CacheDefiniens<V> desc) {
        String key = desc.getKey();
        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).refresh(key, hash, loader);
    }

    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V put(CacheDefiniens<V> desc, AbsReference value) {
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");
        Preconditions.checkNotNull(value);

        String key = desc.getKey();
        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).put(key, hash, value, false);
    }

    /**
     * 不存在时放入缓存
     */
    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V putIfAbsent(CacheDefiniens<V> desc, AbsReference value) {
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");
        Preconditions.checkNotNull(value);

        String key = desc.getKey();
        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).put(key, hash, value, true);
    }

    /**
     * 先持久化再移除
     *
     * @param desc
     * @param isRemove 是否移除
     */
    public void persistAndRemove(CacheDefiniens<? extends AbsReference> desc, boolean isRemove) {
        persistAndRemove(desc.getKey(), isRemove);
    }

    /**
     * 持久化
     *
     * @param key
     * @param isRemove 是否移除
     */
    public void persistAndRemove(String key, final boolean isRemove) {
        Preconditions.checkNotNull(key, "key不能为null");
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");

        final int hash = rehash(key.hashCode());
        segmentFor(hash).doInSegmentUnderLock(key, hash, new SegmentLockHandler<Void>() {
            @Override
            public Void doInSegmentUnderLock(Segment segment, HashEntry e) {
                if (e != null && e.value != null && !e.value.isAllPersist()) {
                    // deleteSet数据保存
                    e.value.persistDeleteSet();
                    // 非deleteSet数据保存
                    e.value.persistDB();
                    if (isRemove) {
                        segment.removeEntry(e, hash);
                    }
                }
                return null;
            }
        });
    }

    public AbsReference remove(String key) {
        int hash = rehash(key.hashCode());
        return segmentFor(hash).remove(key, hash, null);
    }

    public boolean remove(String key, AbsReference value) {
        int hash = rehash(key.hashCode());
        if (value == null)
            return false;
        return segmentFor(hash).remove(key, hash, value) != null;
    }

    public void remove(CacheDefiniens<? extends AbsReference> desc) {
        remove(desc.getKey());
    }

    public void clear() {
        for (int i = 0; i < segments.length; ++i)
            segments[i].clear();
    }

    public boolean containsKey(String key) {
        int hash = rehash(key.hashCode());
        return segmentFor(hash).containsKey(key, hash);
    }

    public boolean contains(CacheDefiniens<? extends AbsReference> cacheDesc) {
        return containsKey(cacheDesc.getKey());
    }

    public boolean isEmpty() {
        final Segment[] segments = this.segments;
        int[] mc = new int[segments.length];
        int mcsum = 0;
        for (int i = 0; i < segments.length; ++i) {
            if (segments[i].count != 0)
                return false;
            else
                mcsum += mc[i] = segments[i].modCount;
        }
        if (mcsum != 0) {
            for (int i = 0; i < segments.length; ++i) {
                if (segments[i].count != 0 || mc[i] != segments[i].modCount)
                    return false;
            }
        }
        return true;
    }

    public void persist() {
        // TODO 立即保存所有对象 待实现
    }

	/* ---------------- 内部方法 -------------- */

    /**
     * 在没有修改的时候，即entry.getNumInUpdateQueue().get() == 0时调用<br>
     * 它会在有锁的情况下检测修改数量是否为0，如果为0则修改为P，否则代表有其他线程修改了，不更改为P
     *
     * @param entry
     * @return 是否成功修改
     */
    boolean changeDbStatePersist(final CacheEntry entry) {
        StopWatch watch = new Slf4JStopWatch();
        Preconditions.checkNotNull(entry.ref(), "CacheEntry中ref不允许为null");

        final String key = entry.ref().getAttachedKey();
        int hash = rehash(key.hashCode());
        boolean result = segmentFor(hash).doInSegmentUnderLock(key, hash, new SegmentLockHandler<Boolean>() {
            @Override
            public Boolean doInSegmentUnderLock(Segment segment, HashEntry e) {
                if (e != null && e.value != null && !isExipredAndAllPersist(e, now())) {
                    // recheck，不等于0代表有其他线程修改了，所以不能改为P状态
                    if (entry.getNumInUpdateQueue().get() != 0) {
                        return false;
                    }
                    if (e.value.changeDbState(entry, DBState.P)) {
                        return true;
                    }
                }
                // 不存在或过期
                throw new RuntimeException("缓存中不存在此对象[" + key + "]，无法更改状态");
            }
        });
        watch.stop("cache.changeDbStatePersist()");
        return result;
    }

    /**
     * 更改db状态并发送到更新队列，缓存不应直接调用此方法<br>
     * 注意：entry必须是在缓存中存在的，且entry.attachedKey都不能为null
     *
     * @param entry
     * @param dbState U or D,不允许P
     */
    void changeDbState(final CacheEntry entry, final DBState dbState) {
        StopWatch watch = new Slf4JStopWatch();
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");

        Preconditions.checkNotNull(entry.ref(), "CacheEntry中ref不允许为null");
        Preconditions.checkNotNull(dbState, "DbState不允许为null");
        Preconditions.checkState(DBState.P != dbState, "DbState不允许为持久化");

        final String key = entry.ref().getAttachedKey();
        int hash = rehash(key.hashCode());
        segmentFor(hash).doInSegmentUnderLock(key, hash, new SegmentLockHandler<Void>() {
            @Override
            public Void doInSegmentUnderLock(Segment segment, HashEntry e) {
                if (e != null && e.value != null && !isExipredAndAllPersist(e, now())) {
                    // 更改CacheEntry的状态
                    if (e.value.changeDbState(entry, dbState)) {
                        return null;
                    } else {
                        throw new RuntimeException("缓存[" + key + "]更改状态失败");
                    }
                }
                throw new RuntimeException("缓存中不存在此对象[" + key + "]，无法更改状态");
            }
        });
        watch.stop("cache.changeDbState()");
    }

	/* ---------------- expiration ---------------- */

    private static long now() {
        return System.nanoTime();
    }

    private boolean isExpired(HashEntry entry, long now) {
        if (now - entry.getAccessTime() > constants.expireAfterAccessNanos) {
            return true;
        }
        return false;
    }

    /**
     * 过期且所有对象已存储
     *
     * @param entry
     * @param now
     * @return
     */
    private boolean isExipredAndAllPersist(HashEntry entry, long now) {
        if (isExpired(entry, now) && entry.getValue().isAllPersist()) {
            return true;
        }
        return false;
    }

    static interface SegmentLockHandler<R> {
        R doInSegmentUnderLock(Segment segment, HashEntry e);
    }

	/*
     * ================== utils =====================
	 */

    /**
     * 发送到更新队列
     *
     * @param entry
     */
    void sendToUpdateQueue(CacheEntry entry) {
        StopWatch watch = new Slf4JStopWatch();
        entry.getNumInUpdateQueue().incrementAndGet();
        updateQueue.add(new CacheEntryInUpdateQueue(entry));
        watch.stop("cache.sendToUpdateQueue()");
    }

    // 缓存关闭标识
    private final AtomicBoolean stop = new AtomicBoolean();

    /**
     * 关闭并写入db
     */
    public void stop() {
        logger.error("stop()启动，缓存被关闭，等待写入线程完成...");
        Stopwatch watch = new Stopwatch().start();
        stop.set(true);
        Future<?> future = service.submit(new Runnable() {
            @Override
            public void run() {
                consumeUpdateQueue(true);
            }
        });
        try {
            // TODO 测试用，临时改为min
            // 阻塞等待线程完成, 90s时间
            future.get(90, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }
        service.shutdown();

        watch.stop();
        logger.error("stop()运行时间:{}ms", watch.elapsedMillis());
    }

    /**
     * 读取计数器:引用原对象<br>
     * 读取写入对象：本类中dbState<br>
     * 写入数据：本类中copied
     */
    private static final class CacheEntryInUpdateQueue {
        private final CacheEntry reference;
        private final DBState dbState;
        private final IEntity entity;

        private CacheEntryInUpdateQueue(CacheEntry reference) {
            this.reference = reference;
            this.dbState = reference.getDbState();

            IEntity parseEntity = reference.parseEntity();
            StopWatch watch = new Slf4JStopWatch();
            if (reference instanceof IEntity && (reference != parseEntity)) {
                // 内存地址不同，创建了新对象
                this.entity = parseEntity;
                watch.lap("cache.entry.new_object()");
            } else {
                // 其他情况，属性拷贝
                try {
                    this.entity = parseEntity.getClass().newInstance();
                    BeanUtils.copyProperties(reference, this.entity);
                    watch.lap("cache.entry.property_copy()");
                } catch (Exception e) {
                    throw new RuntimeException("CacheEntry[" + reference.ref() + "]不能被PropertyCopy", e);
                }
            }
            watch.stop("cache.entry.init_finish()");
        }

        /**
         * 引用原对象
         */
        public CacheEntry getReference() {
            return reference;
        }

        /**
         * 数据，是CacheEntry的数据备份
         */
        public IEntity getEntity() {
            return entity;
        }

        public DBState getDbState() {
            return dbState;
        }
    }

    /**
     * 更新队列消费计数器
     */
    private long counter = 0L;

    /**
     * 将更新队列发送给db存储<br>
     *
     * @param doNow 是否立即写入
     */
    private void consumeUpdateQueue(boolean doNow) {
        logger.error("定时检测：缓存存储数据队列大小：[{}]", updateQueue.size());
        if (doNow || updateQueue.size() >= Constants.MAX_UNITS_IN_UPDATE_QUEUE || (counter++) % Constants.PERSIST_CHECK_INTERVAL == 0) {
            if (updateQueue.size() == 0) {
                return;
            }
            logger.debug("缓存存储数据开始");

            CacheEntryInUpdateQueue wrapper = null;

            List<CacheEntryInUpdateQueue> toBatch = new ArrayList<CacheEntryInUpdateQueue>();
            while ((wrapper = updateQueue.poll()) != null) {
                final StopWatch watch = new Slf4JStopWatch();

                CacheEntry reference = wrapper.getReference();
                int num = reference.getNumInUpdateQueue().decrementAndGet();
                // 删除或者更新的num为0
                if (num == 0) {
                    IEntity entity = wrapper.getEntity();
                    if (entity != null && DBState.P != wrapper.getDbState()) {
                        toBatch.add(wrapper);
                    }
                }

                // 达到批处理提交条件或者更新队列为空，则执行批处理
                if (toBatch.size() > 0 && (toBatch.size() % Constants.BATCH_TO_COMMIT == 0 || updateQueue.size() == 0)) {
                    try {
                        logger.debug("批处理大小：{}", toBatch.size());
                        batchPersistData(toBatch);
                    } catch (Exception e) {
                        logger.error("缓存批处理异常", e);
                    }
                    toBatch.clear();
                }
                watch.stop("cache.loopUpdateQueue()");
            }
        }
    }

    /**
     * 批处理写入数据
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void batchPersistData(Collection<CacheEntryInUpdateQueue> entities) {
        HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        Session session = hibernate.getSession();
        Transaction tx = session.beginTransaction();
        try {
            int idx = 0;

            for (CacheEntryInUpdateQueue wrapper : entities) {
                DBState dbState = wrapper.getDbState();
                IEntity entity = wrapper.getEntity();

                if (DBState.U == dbState) {
                    session.merge(entity);
                } else if (DBState.D == dbState) {
                    // 为什么会有相同ID的对象进行删除，会报错
                    // 是否是对象被删除了，然后又新建了一个，然后又被删除了
                    // 因此在下面的exception中有此处理
                    session.delete(entity);
                }
                if ((++idx) % 50 == 0) {
                    session.flush(); // 清理缓存，执行批量插入20条记录的SQL insert语句
                    session.clear(); // 清空缓存中的Customer对象
                }
            }
            // 提交
            tx.commit();
            for (CacheEntryInUpdateQueue wrapper : entities) {
                // recheck,有可能又有其他线程更新了对象，此时也不能重置为P
                changeDbStatePersist(wrapper.getReference());
            }
        } catch (HibernateException exception) {
            tx.rollback();
            if (!(exception instanceof NonUniqueObjectException)) {
                // 在删除时，如果同一个key的不同对象删除，会报NonUniqueObjectException，但这种情况比较少
                // 可以参考：http://stackoverflow.com/questions/6518567/org-hibernate-nonuniqueobjectexception
                logger.error("缓存批处理写入DB异常", exception);
            }
            for (CacheEntryInUpdateQueue wrapper : entities) {
                try {
                    DBState dbState = wrapper.getDbState();
                    IEntity entity = wrapper.getEntity();

                    if (DBState.U == dbState) {
                        hibernate.saveOrUpdate(entity);
                    } else if (DBState.D == dbState) {
                        hibernate.delete(entity);
                    }
                    // recheck,有可能又有其他线程更新了对象，此时也不能重置为P
                    changeDbStatePersist(wrapper.getReference());
                } catch (DataAccessException e1) {
                    logger.error("批处理失败，单条更新失败", e1);
                }
            }
        } finally {
            try {
                session.close();
            } catch (HibernateException e) {
                logger.error("批处理失败，session.close()异常", e);
            }
        }
    }
}
