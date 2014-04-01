package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.Constants;
import com.cm4j.test.guava.consist.DBState;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.loader.CacheValueLoader;
import com.cm4j.test.guava.consist.loader.PrefixMappping;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <pre>
 * 读写分离：
 * 读取：{@link ConcurrentCache}提供get or load、put、expire操作
 *
 * 写入：是由{@link CacheEntry#changeDbState(com.cm4j.test.guava.consist.DBState)}控制对象状态
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    private final ScheduledExecutorService service;
    private final DBPersistQueue persistQueue;

    /* ---------------- Small Utilities -------------- */
    private static int rehash(int h) {
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    private final Segment segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    /* ---------------- Public operations -------------- */

    private ConcurrentCache(CacheLoader<String, AbsReference> loader) {
        this(Constants.DEFAULT_INITIAL_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, Constants.DEFAULT_CONCURRENCY_LEVEL, loader);
    }

    private ConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel,
                            CacheLoader<String, AbsReference> loader) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0 || loader == null) {
            throw new IllegalArgumentException();
        }

        this.loader = loader;
        this.persistQueue = new DBPersistQueue();

        if (concurrencyLevel > Constants.MAX_SEGMENTS) {
            concurrencyLevel = Constants.MAX_SEGMENTS;
        }

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

        // 定时处理器
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                persistQueue.consumePersistQueue(false);
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
                if (e != null && e.getValue() != null && !e.getValue().isAllPersist()) {
                    // deleteSet数据保存
                    e.getValue().persistDeleteSet();
                    // 非deleteSet数据保存
                    e.getValue().persistDB();
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
                if (e != null && e.getValue() != null && !isExipredAndAllPersist(e, Segment.now())) {
                    // recheck，不等于0代表有其他线程修改了，所以不能改为P状态
                    if (entry.getNumInUpdateQueue().get() != 0) {
                        return false;
                    }
                    if (e.getValue().changeDbState(entry, DBState.P)) {
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
                if (e != null && e.getValue() != null && !isExipredAndAllPersist(e, Segment.now())) {
                    // 更改CacheEntry的状态
                    if (e.getValue().changeDbState(entry, dbState)) {
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

    private boolean isExpired(HashEntry entry, long now) {
        if (now - entry.getAccessTime() > Constants.expireAfterAccessNanos) {
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
        persistQueue.sendToPersistQueue(entry);
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
                persistQueue.consumePersistQueue(true);
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
     * 将更新队列发送给db存储<br>
     *
     * @param doNow 是否立即写入
     */
    private void consumeUpdateQueue(boolean doNow) {
        persistQueue.consumePersistQueue(doNow);
    }
}
