package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.loader.CacheValueLoader;
import com.cm4j.test.guava.consist.loader.PrefixMappping;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.AbstractSequentialIterator;
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
import java.util.*;
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
 * 1.定义缓存描述信息{@link CacheDescriptor}
 * 2.定义映射{@link PrefixMappping}
 * </pre>
 *
 * @author Yang.hao
 * @since 2013-1-30 上午11:25:47
 */
public class ConcurrentCache {

    private static class HOLDER {
        private static final ConcurrentCache instance = new ConcurrentCache(new CacheValueLoader());
    }

    public static ConcurrentCache getInstance() {
        return HOLDER.instance;
    }

    /* ---------------- Constants -------------- */
    // TODO 默认为16
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    // TODO 默认设为16
    static final int DEFAULT_CONCURRENCY_LEVEL = 2;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final int MAX_SEGMENTS = 1 << 16; // slightly conservative
    static final int RETRIES_BEFORE_LOCK = 2;

    // TODO 默认过期纳秒，完成时需更改为较长时间过期，50ms 用于并发测试
    final long expireAfterAccessNanos = TimeUnit.SECONDS.toNanos(6000);
    /**
     * TODO 更新队列检测间隔，单位s
     */
    private static final int CHECK_UPDATE_QUEUE_INTERVAL = 3;
    /**
     * 间隔多少次检查，可持久化，总间隔时间也就是 5 * 60s = 5min
     */
    private static final int PERSIST_CHECK_INTERVAL = 5;
    /**
     * 达到多少个对象，可持久化
     */
    private static final int MAX_UNITS_IN_UPDATE_QUEUE = 50000;
    /**
     * 达到多少条则提交给批处理
     */
    private static final int BATCH_TO_COMMIT = 300;

    // TODO debug模式
    boolean isDebug = true;

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
    private final static class HashEntry implements ReferenceEntry {
        // HashEntery内对象的final不变性来降低读操作对加锁的需求
        final String key;
        final int hash;
        final HashEntry next;
        volatile AbsReference value;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public AbsReference getValue() {
            return value;
        }

        @Override
        public int getHash() {
            return hash;
        }

        @Override
        public ReferenceEntry getNext() {
            return next;
        }

        HashEntry(String key, int hash, HashEntry next, AbsReference value) {
            this.key = key;
            this.hash = hash;
            this.next = next;
            this.value = value;
        }

        static final AtomicReferenceArray<HashEntry> newArray(int i) {
            return new AtomicReferenceArray<HashEntry>(i);
        }

        volatile long accessTime = Long.MAX_VALUE;

        @Override
        public long getAccessTime() {
            return accessTime;
        }

        @Override
        public void setAccessTime(long time) {
            this.accessTime = time;
        }

        ReferenceEntry nextAccess = nullEntry();

        @Override
        public ReferenceEntry getNextInAccessQueue() {
            return nextAccess;
        }

        @Override
        public void setNextInAccessQueue(ReferenceEntry next) {
            this.nextAccess = next;
        }

        ReferenceEntry previousAccess = nullEntry();

        @Override
        public ReferenceEntry getPreviousInAccessQueue() {
            return previousAccess;
        }

        @Override
        public void setPreviousInAccessQueue(ReferenceEntry previous) {
            this.previousAccess = previous;
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

        final Queue<ReferenceEntry> recencyQueue = new ConcurrentLinkedQueue<ReferenceEntry>();
        // accessQueue的大小应该与缓存的size一样大
        final AccessQueue accessQueue = new AccessQueue();
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
            if (oldCapacity >= MAXIMUM_CAPACITY)
                return;

            int newCount = count;
            AtomicReferenceArray<HashEntry> newTable = HashEntry.newArray(oldCapacity << 1);
            threshold = newTable.length() * 3 / 4;
            int newMask = newTable.length() - 1;
            for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
                // We need to guarantee that any existing reads of old Map can
                // proceed. So we cannot yet null out each bin.
                HashEntry head = oldTable.get(oldIndex);

                if (head != null) {
                    HashEntry next = head.next;
                    int headIndex = head.getHash() & newMask;

                    // Single node on list
                    if (next == null) {
                        newTable.set(headIndex, head);
                    } else {
                        HashEntry tail = head;
                        int tailIndex = headIndex;
                        for (HashEntry e = next; e != null; e = e.next) {
                            int newIndex = e.getHash() & newMask;
                            if (newIndex != tailIndex) {
                                // The index changed. We'll need to copy the
                                // previous entry.
                                tailIndex = newIndex;
                                tail = e;
                            }
                        }
                        newTable.set(tailIndex, tail);

                        // Clone nodes leading up to the tail.
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

            ReferenceEntry e;
            int firstHash = 0;
            while ((e = accessQueue.peek()) != null && map.isExpired(e, now)) {
                if (firstHash == 0) {
                    // 第一次循环，设置first
                    firstHash = e.getHash();
                } else if (e.getHash() == firstHash) {
                    // 第N此循环，又碰到e，代表已经完成了一次循环，这样可防止无限循环
                    break;
                }

                if (map.isDebug) {
                    // access
                    // 注意：这里的无限循环
                    map.logger.trace("segment.accessQueue个数:{},size:{}", accessQueue.size(), count);
                }

                if (map.isDebug && accessQueue.size() != count) {
                    throw new RuntimeException("个数不一致：accessQueue:" + accessQueue.size() + ",count:" + count);
                }

                if (e.getValue().isAllPersist()) {
                    removeEntry((HashEntry) e, e.getHash());

                    if (map.isDebug && accessQueue.size() != count) {
                        throw new RuntimeException("个数不一致：accessQueue:" + accessQueue.size() + ",count:" + count);
                    }
                } else {
                    recordAccess(e);

                    if (map.isDebug && accessQueue.size() != count) {
                        throw new RuntimeException("个数不一致：accessQueue:" + accessQueue.size() + ",count:" + count);
                    }
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
                    HashEntry newFirst = removeEntryFromChain(first, entry);
                    tab.set(index, newFirst);
                    count = c; // write-volatile
                    if (map.isDebug) {
                        map.logger.warn("缓存[{}]被移除", entry.key);
                    }
                    return;
                }
            }
        }

        HashEntry removeEntryFromChain(HashEntry first, HashEntry entry) {
            if (map.isDebug && !accessQueue.contains(entry)) {
                throw new RuntimeException("被移除数据不在accessQueue中,key:" + entry.key);
            }
            HashEntry newFirst = entry.next;
            for (HashEntry e = first; e != entry; e = e.next) {
                newFirst = copyEntry(e, newFirst);
            }
            // 从队列移除
            accessQueue.remove(entry);
            return newFirst;
        }

        HashEntry copyEntry(HashEntry original, HashEntry newNext) {
            HashEntry newEntry = new HashEntry(original.getKey(), original.getHash(), newNext, original.value);
            copyAccessEntry(original, newEntry);
            return newEntry;
        }

        void copyAccessEntry(HashEntry original, HashEntry newEntry) {
            newEntry.setAccessTime(original.getAccessTime());

            connectAccessOrder(original.getPreviousInAccessQueue(), newEntry);
            connectAccessOrder(newEntry, original.getNextInAccessQueue());

            nullifyAccessOrder(original);
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
         *
         * <p>
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
        void recordAccess(ReferenceEntry e) {
            e.setAccessTime(now());
            recencyQueue.add(e);
        }

        void drainRecencyQueue() {
            ReferenceEntry e;
            while ((e = recencyQueue.poll()) != null) {
                if (accessQueue.contains(e)) {
                    accessQueue.add(e);
                }
            }
        }
    }

	/* ---------------- Public operations -------------- */

    private ConcurrentCache(CacheLoader<String, AbsReference> loader) {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, loader);
    }

    private ConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel,
                            CacheLoader<String, AbsReference> loader) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0 || loader == null)
            throw new IllegalArgumentException();

        this.loader = loader;

        if (concurrencyLevel > MAX_SEGMENTS)
            concurrencyLevel = MAX_SEGMENTS;

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

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
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
        }, 1, CHECK_UPDATE_QUEUE_INTERVAL, TimeUnit.SECONDS);
    }

    public int size() {
        final Segment[] segments = this.segments;
        long sum = 0;
        long check = 0;
        int[] mc = new int[segments.length];
        // Try a few times to get accurate count. On failure due to
        // continuous async changes in table, resort to locking.
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
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
    public <V extends AbsReference> V get(CacheDescriptor<V> desc) {
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
    public <V extends AbsReference> V getIfPresent(CacheDescriptor<V> desc) {
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
    public <V extends AbsReference> V refresh(CacheDescriptor<V> desc) {
        String key = desc.getKey();
        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).refresh(key, hash, loader);
    }

    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V put(CacheDescriptor<V> desc, AbsReference value) {
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");
        Preconditions.checkNotNull(value);

        String key = desc.getKey();
        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).put(key, hash, value, false);
    }

    // TODO 临时方法 ,用于test测试
    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V put(String key, AbsReference value) {
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");
        Preconditions.checkNotNull(value);

        int hash = rehash(key.hashCode());
        return (V) segmentFor(hash).put(key, hash, value, false);
    }

    /**
     * 不存在时放入缓存
     */
    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V putIfAbsent(CacheDescriptor<V> desc, AbsReference value) {
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
     * @param isRemove
     *         是否移除
     */
    public void persistAndRemove(CacheDescriptor<? extends AbsReference> desc, boolean isRemove) {
        persistAndRemove(desc.getKey(), isRemove);
    }

    /**
     * 持久化
     *
     * @param key
     * @param isRemove
     *         是否移除
     */
    public void persistAndRemove(String key, final boolean isRemove) {
        Preconditions.checkNotNull(key, "key不能为null");
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");

        final int hash = rehash(key.hashCode());
        segmentFor(hash).doInSegmentUnderLock(key, hash, new SegmentLockHandler<Void>() {
            @Override
            public Void doInSegmentUnderLock(Segment segment, HashEntry e) {
                if (e != null && e.value != null && !e.value.isAllPersist()) {
                    e.value.persistDB();
                    if (isRemove) {
                        segment.removeEntry(e, hash);
                    }
                }
                return null;
            }
        });
    }

    public boolean replace(String key, final AbsReference oldValue, final AbsReference newValue) {
        if (oldValue == null || newValue == null)
            throw new NullPointerException();
        int hash = rehash(key.hashCode());
        return segmentFor(hash).doInSegmentUnderLock(key, hash, new SegmentLockHandler<Boolean>() {
            @Override
            public Boolean doInSegmentUnderLock(Segment segment, HashEntry e) {
                boolean replaced = false;
                if (e != null && oldValue.equals(e.value)) {
                    replaced = true;
                    e.value = newValue;
                }
                return replaced;
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

    public void remove(CacheDescriptor<? extends AbsReference> desc) {
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

    public boolean contains(CacheDescriptor<? extends AbsReference> cacheDesc) {
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
     * @param dbState
     *         U or D,不允许P
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

    private boolean isExpired(ReferenceEntry entry, long now) {
        if (now - entry.getAccessTime() > expireAfterAccessNanos) {
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
    private boolean isExipredAndAllPersist(ReferenceEntry entry, long now) {
        if (isExpired(entry, now) && entry.getValue().isAllPersist()) {
            return true;
        }
        return false;
    }

	/* ---------------- Queue -------------- */

    interface ReferenceEntry {
        /*
		 * Used by entries that use access order. Access entries are maintained
		 * in a doubly-linked list. New entries are added at the tail of the
		 * list at write time; stale entries are expired from the head of the
		 * list.
		 * 
		 * 插入到尾部，过期从首部
		 */

        String getKey();

        AbsReference getValue();

        int getHash();

        ReferenceEntry getNext();

        /**
         * Returns the time that this entry was last accessed, in ns.
         */
        long getAccessTime();

        /**
         * Sets the entry access time in ns.
         */
        void setAccessTime(long time);

        /**
         * Returns the next entry in the access queue.
         */
        ReferenceEntry getNextInAccessQueue();

        /**
         * Sets the next entry in the access queue.
         */
        void setNextInAccessQueue(ReferenceEntry next);

        /**
         * Returns the previous entry in the access queue.
         */
        ReferenceEntry getPreviousInAccessQueue();

        /**
         * Sets the previous entry in the access queue.
         */
        void setPreviousInAccessQueue(ReferenceEntry previous);

    }

    private enum NullEntry implements ReferenceEntry {
        INSTANCE;

        @Override
        public String getKey() {
            return null;
        }

        @Override
        public AbsReference getValue() {
            return null;
        }

        @Override
        public ReferenceEntry getNext() {
            return null;
        }

        @Override
        public int getHash() {
            return 0;
        }

        ;

        @Override
        public long getAccessTime() {
            return 0;
        }

        @Override
        public void setAccessTime(long time) {
        }

        @Override
        public ReferenceEntry getNextInAccessQueue() {
            return this;
        }

        @Override
        public void setNextInAccessQueue(ReferenceEntry next) {
        }

        @Override
        public ReferenceEntry getPreviousInAccessQueue() {
            return this;
        }

        @Override
        public void setPreviousInAccessQueue(ReferenceEntry previous) {
        }

    }

    // impl never uses a parameter or returns any non-null value
    static ReferenceEntry nullEntry() {
        return (ReferenceEntry) NullEntry.INSTANCE;
    }

    static final class AccessQueue extends AbstractQueue<ReferenceEntry> {
        final ReferenceEntry head = new ReferenceEntry() {

            @Override
            public String getKey() {
                return null;
            }

            @Override
            public AbsReference getValue() {
                return null;
            }

            @Override
            public ReferenceEntry getNext() {
                return nullEntry();
            }

            public int getHash() {
                return 0;
            }

            ;

            @Override
            public long getAccessTime() {
                return Long.MAX_VALUE;
            }

            @Override
            public void setAccessTime(long time) {
            }

            ReferenceEntry nextAccess = this;

            @Override
            public ReferenceEntry getNextInAccessQueue() {
                return nextAccess;
            }

            @Override
            public void setNextInAccessQueue(ReferenceEntry next) {
                this.nextAccess = next;
            }

            ReferenceEntry previousAccess = this;

            @Override
            public ReferenceEntry getPreviousInAccessQueue() {
                return previousAccess;
            }

            @Override
            public void setPreviousInAccessQueue(ReferenceEntry previous) {
                this.previousAccess = previous;
            }
        };

        // implements Queue

        @Override
        public boolean offer(ReferenceEntry entry) {
            // unlink
            connectAccessOrder(entry.getPreviousInAccessQueue(), entry.getNextInAccessQueue());

            // add to tail
            connectAccessOrder(head.getPreviousInAccessQueue(), entry);
            connectAccessOrder(entry, head);

            return true;
        }

        @Override
        public ReferenceEntry peek() {
            ReferenceEntry next = head.getNextInAccessQueue();
            return (next == head) ? null : next;
        }

        @Override
        public ReferenceEntry poll() {
            ReferenceEntry next = head.getNextInAccessQueue();
            if (next == head) {
                return null;
            }

            remove(next);
            return next;
        }

        @Override
        public boolean remove(Object o) {
            ReferenceEntry e = (ReferenceEntry) o;
            ReferenceEntry previous = e.getPreviousInAccessQueue();
            ReferenceEntry next = e.getNextInAccessQueue();
            connectAccessOrder(previous, next);
            nullifyAccessOrder(e);

            return next != NullEntry.INSTANCE;
        }

        @Override
        public boolean contains(Object o) {
            ReferenceEntry e = (ReferenceEntry) o;
            return e.getNextInAccessQueue() != NullEntry.INSTANCE;
        }

        @Override
        public boolean isEmpty() {
            return head.getNextInAccessQueue() == head;
        }

        @Override
        public int size() {
            int size = 0;
            for (ReferenceEntry e = head.getNextInAccessQueue(); e != head; e = e.getNextInAccessQueue()) {
                size++;
            }
            return size;
        }

        @Override
        public void clear() {
            ReferenceEntry e = head.getNextInAccessQueue();
            while (e != head) {
                ReferenceEntry next = e.getNextInAccessQueue();
                nullifyAccessOrder(e);
                e = next;
            }

            head.setNextInAccessQueue(head);
            head.setPreviousInAccessQueue(head);
        }

        @Override
        public Iterator<ReferenceEntry> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry>(peek()) {
                @Override
                protected ReferenceEntry computeNext(ReferenceEntry previous) {
                    ReferenceEntry next = previous.getNextInAccessQueue();
                    return (next == head) ? null : next;
                }
            };
        }
    }

    static void connectAccessOrder(ReferenceEntry previous, ReferenceEntry next) {
        previous.setNextInAccessQueue(next);
        next.setPreviousInAccessQueue(previous);
    }

    static void nullifyAccessOrder(ReferenceEntry nulled) {
        ReferenceEntry nullEntry = nullEntry();
        nulled.setNextInAccessQueue(nullEntry);
        nulled.setPreviousInAccessQueue(nullEntry);
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
            if (reference instanceof IEntity && ((IEntity) reference != parseEntity)) {
                // 内存地址不同，创建了新对象
                this.entity = parseEntity;
                watch.lap("cache.entry.new_object()");
            } else {
                // 其他情况，属性拷贝
                try {
                    this.entity = parseEntity.getClass().newInstance();
                    BeanUtils.copyProperties(reference, this.entity);
                    // this.entity = new
                    // DeepCopyUtil().deepCopy(reference.parseEntity());
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
     * @param doNow
     *         是否立即写入
     */
    private void consumeUpdateQueue(boolean doNow) {
        logger.error("定时检测：缓存存储数据队列大小：[{}]", updateQueue.size());
        if (doNow || updateQueue.size() >= MAX_UNITS_IN_UPDATE_QUEUE || (counter++) % PERSIST_CHECK_INTERVAL == 0) {
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
                if (toBatch.size() > 0 && (toBatch.size() % BATCH_TO_COMMIT == 0 || updateQueue.size() == 0)) {
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
