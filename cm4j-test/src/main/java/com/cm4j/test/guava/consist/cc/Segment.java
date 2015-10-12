package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.constants.Constants;
import com.cm4j.test.guava.consist.fifo.FIFOAccessQueue;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yanghao on 14-3-31.
 */
final class Segment extends ReentrantLock implements Serializable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = 2249069246763182397L;
    // 作为位操作的mask，必须是(2^n)-1
    private static final int DRAIN_THRESHOLD = 0x3F;

    transient volatile int count;
    transient int modCount;
    transient int threshold;
    transient volatile AtomicReferenceArray<HashEntry> table;
    final float loadFactor;

    // 真正中缓存的访问顺序
    // accessQueue的大小应该与缓存的size一样大
    final FIFOAccessQueue<HashEntry> accessQueue = new FIFOAccessQueue();

    // inner

    static final Segment[] newArray(int i) {
        return new Segment[i];
    }

    private final String name;

    Segment(int initialCapacity, float lf, String name) {
        loadFactor = lf;
        this.name = name;
        setTable(HashEntry.newArray(initialCapacity));
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
        for (HashEntry e = getFirst(hash); e != null; e = e.getNext()) {
            if (e.getHash() == hash && key.equals(e.getKey())) {
                return e;
            }
        }
        return null;
    }

    AbsReference getNotExipredValue(HashEntry e, long now) {
        // 直接返回未过期的对象
        boolean expired = isExpired(e, now);
        if (e != null && !expired) {
            return e.getQueueEntry();
        }
        if (expired) {
            tryExpireEntries(now);
        }
        return null;
    }

    /**
     * 查找存活的Entry，包含Persist检测
     *
     * @param e
     * @param now
     * @return
     */
    HashEntry getLiveEntry(HashEntry e, long now) {
        if (e == null) {
            return null;
        } else if (isExpired(e, now)) {
            // 如果状态不是P，则会延迟生命周期
            tryExpireEntries(now);
            if (e.getQueueEntry().isAllPersist()) {
                return null;
            }
        }
        return e;
    }

    // called methods

    AbsReference get(CacheDefiniens definiens, int hash, boolean isLoad) {
        final StopWatch watch = new Slf4JStopWatch();
        final String key = definiens.getKey();
        try {
            if (count != 0) { // read-volatile
                HashEntry e = getEntry(key, hash);
                if (e != null) {
                    // 这里只是一次无锁情况的快速尝试查询，如果未查询到，会在有锁情况下再查一次
                    AbsReference value = getNotExipredValue(e, CCUtils.now());
                    if (value != null) {
                        watch.lap("get.缓存获取到");
                        recordAccess(e);
                        return value;
                    }
                }
            }
            if (isLoad) {
                // at this point e is either null or expired;
                AbsReference ref = lockedGetOrLoad(definiens, hash);
                watch.lap("get.锁定获取");
                return ref;
            }
        } finally {
            postReadCleanup();
            watch.stop("get()完成");
        }
        return null;
    }

    AbsReference lockedGetOrLoad(CacheDefiniens definiens, int hash) {
        HashEntry e;
        AbsReference ref;

        final String key = definiens.getKey();
        lock();
        try {
            // re-read ticker once inside the lock
            long now = CCUtils.now();
            preWriteCleanup(now);

            int newCount = this.count - 1;
            AtomicReferenceArray<HashEntry> table = this.table;
            int index = hash & (table.length() - 1);
            HashEntry first = table.get(index);

            for (e = first; e != null; e = e.getNext()) {
                String entryKey = e.getKey();
                if (e.getHash() == hash && entryKey != null && entryKey.equals(key)) {
                    ref = e.getQueueEntry();

                    if (ref != null && !(isExpired(e, now) && ref.isAllPersist())) {
                        recordAccess(e);
                        return ref;
                    }

                    // ref为空或者 过期全保存，会走到这里。[检测过后，到这里过期了。此时就会走到这里]
                    // 这里移除不移除 accessQueue.remove(e) 都没关系，因为下面会重新put
                    // TODO 但count对不对？？待测
                    accessQueue.remove(e);
                    this.count = newCount; // write-volatile
                    break;
                }
            }

            // 获取且保存
            ref = definiens.load();
            // 放入缓存
            put(key, hash, ref, false);
            definiens.afterLoad(ref);
            // 加载完之后调用
            return ref;
        } finally {
            unlock();
        }
    }

    boolean containsKey(String key, int hash) {
        if (count != 0) { // read-volatile

            lock();
            try {
                return getLiveEntry(getEntry(key, hash), CCUtils.now()) != null;
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
            preWriteCleanup(CCUtils.now());

            int c = count;
            if (c++ > threshold) { // ensure capacity
                expand();
            }
            // 为什么要创建tab指向table
            // 这是因为table变量是volatile类型，多次读取volatile类型的开销要比非volatile开销要大，而且编译器也无法优化
            AtomicReferenceArray<HashEntry> tab = table;
            int index = hash & (tab.length() - 1);
            HashEntry first = tab.get(index);
            HashEntry e = first;
            while (e != null && (e.getHash() != hash || !key.equals(e.getKey())))
                e = e.getNext();

            AbsReference oldValue;
            if (e != null) {
                oldValue = e.getQueueEntry();
                if (!onlyIfAbsent) {
                    e.setQueueEntry(value);
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
            watch.stop("put()完成");
        }
    }

    void persistAndRemove(String key, int hash ,boolean isRemove) {
        lock();
        try {
            // persistAndRemove()以缓存内对象为准，因为缓存内对象是最新的
            // persistMap中有，则persistQueue也一定有
            // 因为persistQueue执行完成之后，需要移除persistMap

            HashEntry e = getEntry(key, hash);

            // 缓存中存在对象
            AbsReference ref;
            if (e != null && (ref = e.getQueueEntry()) != null) {
                // 数据保存
                Map<String, PersistValue> persistMap = ref.getPersistMap();
                Collection<PersistValue> values = persistMap.values();
                if (!values.isEmpty()) {
                    Map<String,CacheMirror> mirrors = Maps.newHashMap();
                    for (PersistValue value : values) {
                        mirrors.put(value.getEntry().getID(), value.getEntry().mirror(value.getDbState(), value.getVersion()));
                    }
                    this.persistQueue.persistImmediatly(mirrors);

                    // 清空persistMap
                    persistMap.clear();
                }
                if (isRemove) {
                    // 是否应该把里面所有元素的ref都设为null，这样里面元素则不能update
                    removeEntry(e, hash);
                }
            }
        } finally {
            unlock();
        }
    }

    /**
     * 这个方法真的应该存在么？
     */
    @Deprecated
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

                // todo 是否该放到这里??? 清除PersistQueue
                getPersistQueue().getMap().clear();
            } finally {
                unlock();
            }
        }
    }

    /**
     * 扩容
     */
    void expand() {
        StopWatch watch = new Slf4JStopWatch();
        AtomicReferenceArray<HashEntry> oldTable = table;
        int oldCapacity = oldTable.length();
        if (oldCapacity >= Constants.MAXIMUM_CAPACITY)
            return;

        logger.error("segment[{}] expand...", this);

        int newCount = count;
        AtomicReferenceArray<HashEntry> newTable = HashEntry.newArray(oldCapacity << 1);
        threshold = (int) (newTable.length() * loadFactor);
        int newMask = newTable.length() - 1;
        for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
            // We need to guarantee that any existing reads of old Map can
            // proceed. So we cannot yet null out each bin.
            HashEntry head = oldTable.get(oldIndex);

            if (head != null) {
                HashEntry next = head.getNext();
                int headIndex = head.getHash() & newMask;

                // next为空代表这个链表就只有一个元素，直接把这个元素设置到新数组中
                if (next == null) {
                    newTable.set(headIndex, head);
                } else {
                    // 有多个元素时
                    HashEntry tail = head;
                    int tailIndex = headIndex;
                    // 从head开始，一直到链条末尾，找到最后一个下标与head下标不一致的元素
                    for (HashEntry e = next; e != null; e = e.getNext()) {
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
                    for (HashEntry e = head; e != tail; e = e.getNext()) {
                        int newIndex = e.getHash() & newMask;
                        HashEntry newNext = newTable.get(newIndex);
                        HashEntry newFirst = copyEntry(e, newNext);
                        if (newFirst != null) {
                            newTable.set(newIndex, newFirst);
                        } else {
                            // todo 为什么要移除accessQueue ?
                            // 貌似是因为 copyEntry可能返回null，因为e被回收了。 需要和源码进行比对
                            accessQueue.remove(e);
                            newCount--;
                        }
                    }
                }
            }
        }
        table = newTable;
        this.count = newCount;
        watch.stop("expand()完成");
    }

    // expiration，过期相关业务

    /**
     * Cleanup expired entries when the lock is available.
     */
    void tryExpireEntries(long now) {
        if (tryLock()) {
            try {
                drainRecencyQueue();

                List<HashEntry> toRemoved = Lists.newArrayList();
                Iterator<HashEntry> iterator = accessQueue.iterator();
                while (iterator.hasNext()) {
                    HashEntry e = iterator.next();
                    // 碰到第一个未过期的，则退出
                    if (!isExpired(e, now)) {
                        break;
                    }

                    // accessQueue大小应该与count一致
                    int accessQueueSize = accessQueue.size();
                    Preconditions.checkArgument(accessQueueSize == count, "个数不一致：accessQueue:" + accessQueueSize + ",count:" + count);

                    AbsReference ref = e.getQueueEntry();
                    if (ref.isAllPersist()) {
                        toRemoved.add(e);
                    } else {
                        // 延长有效期
                        recordAccess(e);

                        // todo 这里会不会重复发送？？？
                        // todo 如果重复发送会不会出现那里移除，这里又发了一遍？
                        // 发送当前数据到persistQueue
                        Map<String, PersistValue> persistMap = e.getQueueEntry().getPersistMap();
                        if (!persistMap.isEmpty()) {
                            this.persistQueue.sendToPersistQueue(persistMap.values());
                        }
                    }
                }
                // 注意：不要在iter的时候移除，这样HashEntry会被置为NullEntry，导致迭代异常
                for (HashEntry e : toRemoved) {
                    logger.warn("缓存[{}-{}]过期", new Object[]{e.getKey(), e.getQueueEntry()});
                    removeEntry(e, e.getHash());
                }
            } finally {
                unlock();
            }
        } else {
            // 有无锁的地方调此方法，比如说getLiveEntry()，如果此时有锁被占用，就走到这里
            logger.error("tryLock failed,can not tryExpireEntries...");
        }
    }

    /**
     * 把所有未持久化对象都发送到persistQueue用于保存
     */
    void drainAllToPersistQueue() {
        lock();
        try{
            drainRecencyQueue();

            Iterator<HashEntry> iterator = accessQueue.iterator();
            while (iterator.hasNext()) {
                HashEntry e = iterator.next();
                Map<String, PersistValue> persistMap = e.getQueueEntry().getPersistMap();
                if (!persistMap.isEmpty()) {
                    this.persistQueue.sendToPersistQueue(persistMap.values());
                }
            }
        } finally {
            unlock();
        }
    }

    /**
     * 从CC中移除entry，这里并没有从persistQueue中移除
     * @param entry
     * @param hash
     */
    void removeEntry(HashEntry entry, int hash) {
        int c = count - 1;
        AtomicReferenceArray<HashEntry> tab = table;
        int index = hash & (tab.length() - 1);
        HashEntry first = tab.get(index);

        for (HashEntry e = first; e != null; e = e.getNext()) {
            if (e == entry) {
                // 是否remove后面的，再remove前面的 access 是copied 的？
                ++modCount;
                // 从链表1中删除元素entry，且返回链表2的头节点
                HashEntry newFirst = removeEntryFromChain(first, entry);
                // 将链表2的新的头节点设置到segment的table中
                tab.set(index, newFirst);
                count = c; // write-volatile

                logger.warn("缓存[{}-{}]被移除",
                        new Object[]{entry.getKey(), entry.getQueueEntry()});
                return;
            }
        }
    }

    HashEntry removeEntryFromChain(HashEntry first, HashEntry entry) {
        Preconditions.checkArgument(accessQueue.contains(entry),"被移除数据不在accessQueue中,key:" + entry.getKey());

        HashEntry newFirst = entry.getNext();
        // 从链条1的头节点first开始迭代到需要删除的节点entry
        for (HashEntry e = first; e != entry; e = e.getNext()) {
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
        // todo 这个和源码不一样？  源码里面有返回null值
        HashEntry newEntry = new HashEntry(original.getKey(), original.getHash(), newNext, original.getQueueEntry());
        copyAccessEntry(original, newEntry);
        return newEntry;
    }

    void copyAccessEntry(HashEntry original, HashEntry newEntry) {
        newEntry.setAccessTime(original.getAccessTime());

        accessQueue.connectAccessOrder(original.getPreviousInAccessQueue(), newEntry);
        accessQueue.connectAccessOrder(newEntry, original.getNextInAccessQueue());

        accessQueue.nullifyAccessOrder(original);
    }

    final AtomicInteger readCount = new AtomicInteger();

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
     * <p>
     * <p>
     * Post-condition: expireEntries has been run.
     */
    void preWriteCleanup(long now) {
        runLockedCleanup(now);
    }

    void cleanUp() {
        runLockedCleanup(CCUtils.now());
    }

    void runLockedCleanup(long now) {
        tryExpireEntries(now);
    }


    // 缓存读取，写入的对象都需要放入recencyQueue
    // 为什么要有recencyQueue？
    // 因为accessQueue是非线程安全的，如果直接在recordAccess()[可能多线程调用]里面调用accessQueue，则线程不安全
    // 因此增加一个线程安全的recencyQueue来保证线程的安全性，
    final Queue<HashEntry> recencyQueue = new ConcurrentLinkedQueue<HashEntry>();

    /**
     * 1.记录访问时间<br>
     * 2.增加到访问对列的尾部
     */
    void recordAccess(HashEntry e) {
        e.setAccessTime(CCUtils.now());
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
            if (accessQueue.contains(e)) {
                accessQueue.add(e);
            }
        }
    }

    private boolean isExpired(HashEntry entry, long now) {
        return (now - entry.getAccessTime() > Constants.expireAfterAccessNanos);
    }

    // 持久化队列，用于替换DBPersistQueue
    private final DBPersistQueue persistQueue = new DBPersistQueue(this);

    public DBPersistQueue getPersistQueue() {
        return persistQueue;
    }

    // ----------------------- lock handler -----------------------

    <R> R doInSegmentUnderLock(String key, int hash, CCUtils.SegmentLockHandler<R> handler) {
        lock();
        preWriteCleanup(CCUtils.now());
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.getHash() != hash || !key.equals(e.getKey())))
                e = e.getNext();

            AbsReference ref = e != null ? e.getQueueEntry() : null;
            return handler.doInSegmentUnderLock(this, e, ref);
        } finally {
            unlock();
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}
