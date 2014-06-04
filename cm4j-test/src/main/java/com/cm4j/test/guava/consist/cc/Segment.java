package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.constants.Constants;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.fifo.FIFOAccessQueue;
import com.google.common.base.Preconditions;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Queue;
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

    // 缓存读取，写入的对象都需要放入recencyQueue
    // 为什么要有recencyQueue？
    // 因为accessQueue是非线程安全的，如果直接在recordAccess()里面调用accessQueue，则线程不安全
    // 因此增加一个线程安全的recencyQueue来保证线程的安全性，
    final Queue<HashEntry> recencyQueue = new ConcurrentLinkedQueue<HashEntry>();
    // 真正中缓存的访问顺序
    // accessQueue的大小应该与缓存的size一样大
    final FIFOAccessQueue<HashEntry> accessQueue = new FIFOAccessQueue();
    final AtomicInteger readCount = new AtomicInteger();

    Segment(int initialCapacity, float lf) {
        loadFactor = lf;
        setTable(HashEntry.newArray(initialCapacity));
    }

    static final Segment[] newArray(int i) {
        return new Segment[i];
    }

    public static long now() {
        return System.nanoTime();
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

    AbsReference getLiveValue(String key, int hash, long now) {
        HashEntry e = getLiveEntry(key, hash, now);
        if (e != null) {
            return e.getQueueEntry();
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
        } else if (isExpired(e, now)) {
            // 如果状态不是P，则会延迟生命周期
            tryExpireEntries(now);
            // 非精准查询，如果延长生命周期，这里依然返回null，get()调用时需在有锁情况下做二次检测
            if (e.getQueueEntry().isAllPersist()) {
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
                    if (value != null) {
                        watch.lap("get.缓存获取到");
                        recordAccess(e);
                        return value;
                    }
                }
            }
            if (isLoad) {
                // at this point e is either null or expired;
                AbsReference ref = lockedGetOrLoad(key, hash, loader);
                watch.lap("get.锁定获取");
                return ref;
            }
        } finally {
            postReadCleanup();
            watch.stop("get()完成");
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

            for (e = first; e != null; e = e.getNext()) {
                String entryKey = e.getKey();
                if (e.getHash() == hash && entryKey != null && entryKey.equals(key)) {
                    value = e.getQueueEntry();

                    if (value != null && !(isExpired(e, now) && !value.isAllPersist())) {
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
            value = loader.load(key);

            if (value != null) {
                put(key, hash, value, false);
            } else {
                logger.debug("cache[{}] not found", key);
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
                logger.debug("cache[{}] not found", key);
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
            postWriteCleanup();
            watch.stop("put()完成");
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
                            accessQueue.remove(e);
                            newCount--;
                        }
                    }
                }
            }
        }
        table = newTable;
        this.count = newCount;
        watch.stop("rehash()完成");
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
                AbsReference v = e.getQueueEntry();
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

    <R> R doInSegmentUnderLock(String key, int hash, ConcurrentCache.SegmentLockHandler<R> handler) {
        lock();
        preWriteCleanup(now());
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.getHash() != hash || !key.equals(e.getKey())))
                e = e.getNext();

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
        while ((e = accessQueue.peek()) != null && isExpired(e, now)) {
            if (firstEntry == null) {
                // 第一次循环，设置first
                firstEntry = e;
            } else if (e == firstEntry) {
                // 第N此循环，又碰到e，代表已经完成了一次循环，这样可防止无限循环
                break;
            }

            // accessQueue大小应该与count一致
            Preconditions.checkArgument(accessQueue.size() == count, "个数不一致：accessQueue:" + accessQueue.size() + ",count:" + count);

            if (e.getQueueEntry().isAllPersist()) {
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

        for (HashEntry e = first; e != null; e = e.getNext()) {
            if (e == entry) {
                // 是否remove后面的，再remove前面的 access 是copied 的？
                ++modCount;
                // 从链表1中删除元素entry，且返回链表2的头节点
                HashEntry newFirst = removeEntryFromChain(first, entry);
                // 将链表2的新的头节点设置到segment的table中
                tab.set(index, newFirst);
                count = c; // write-volatile

                logger.warn("缓存[{}]被移除", entry.getKey());
                return;
            }
        }
    }

    HashEntry removeEntryFromChain(HashEntry first, HashEntry entry) {
        if (!accessQueue.contains(entry)) {
            throw new RuntimeException("被移除数据不在accessQueue中,key:" + entry.getKey());
        }
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
            if (accessQueue.contains(e)) {
                accessQueue.add(e);
            }
        }
    }

    private boolean isExpired(HashEntry entry, long now) {
        return (now - entry.getAccessTime() > Constants.expireAfterAccessNanos);
    }
}
