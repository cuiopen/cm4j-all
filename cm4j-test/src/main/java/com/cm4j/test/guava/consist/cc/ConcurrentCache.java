package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.constants.Constants;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.loader.CacheValueLoader;
import com.cm4j.test.guava.consist.loader.PrefixMappping;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <pre>
 * 读写分离：
 * 读取：{@link ConcurrentCache}提供get or load、put、expire操作
 *
 * 写入：。。。。
 *
 * 使用流程：
 * 1.定义缓存描述信息{@link com.cm4j.test.guava.consist.loader.CacheDefiniens}
 * 2.定义映射{@link PrefixMappping}
 * </pre>
 *
 * @author yanghao
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
    final int segmentMask;
    final int segmentShift;
    final Segment[] segments;

    private final ScheduledExecutorService service;

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
            this.segments[i] = new Segment(cap, loadFactor, "segment-" + (i + 1));
        }

        // 定时处理器
        service = Executors.newScheduledThreadPool(segments.length);
        for (final Segment segment : segments) {
            service.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    segment.getPersistQueue().consumePersistQueue(false);
                }
            }, 1, Constants.CHECK_UPDATE_QUEUE_INTERVAL, TimeUnit.SECONDS);
        }
    }

    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V get(CacheDefiniens<V> definiens) {
        int hash = CCUtils.rehash(definiens.getKey().hashCode());
        return (V) segmentFor(hash).get(definiens, hash, true);
    }

    /**
     * 存在即获取
     *
     * @return 不存在时返回的reference为null
     */
    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V getIfPresent(CacheDefiniens<V> definiens) {
        int hash = CCUtils.rehash(definiens.getKey().hashCode());
        return (V) segmentFor(hash).get(definiens, hash, false);
    }

    @SuppressWarnings("unchecked")
    public <V extends AbsReference> V put(CacheDefiniens<V> desc, AbsReference value) {
        Preconditions.checkArgument(!stop.get(), "缓存已关闭，无法写入缓存");
        Preconditions.checkNotNull(value);

        String key = desc.getKey();
        int hash = CCUtils.rehash(key.hashCode());
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
        int hash = CCUtils.rehash(key.hashCode());
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

        final int hash = CCUtils.rehash(key.hashCode());
        segmentFor(hash).persistAndRemove(key, hash, isRemove);
    }

    public void clear() {
        for (int i = 0; i < segments.length; ++i)
            segments[i].clear();
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

    public boolean containsKey(String key) {
        int hash = CCUtils.rehash(key.hashCode());
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
        // TODO 立即保存所有对象待实现
        throw new RuntimeException("not implemented");
    }

	/* ---------------- 内部方法 -------------- */

    void doUnderLock(final String cacheKey,CCUtils.SegmentLockHandler handler) {
        int hash = CCUtils.rehash(cacheKey.hashCode());
        segmentFor(hash).doInSegmentUnderLock(cacheKey, hash, handler);
    }

    /**
     * 缓存保存之后需要从persistMap中移除此对象
     * @param mirror
     */
    void removeAfterPersist(final CacheMirror mirror) {
        doUnderLock(mirror.getCacheKey(), new CCUtils.SegmentLockHandler() {
            @Override
            public Object doInSegmentUnderLock(Segment segment, HashEntry e, AbsReference ref) {
                if (ref != null) {
                    Map<String, PersistValue> persistMap = ref.getPersistMap();
                    String dbKey = mirror.getDbKey();
                    PersistValue persistValue = persistMap.get(dbKey);
                    // 需比对版本号，以防止其他线程修改了此对象
                    if (persistValue != null && mirror.getVersion() == persistValue.getVersion()) {
                        persistMap.remove(dbKey);
                        logger.debug("persistValue[{}] is removed from persistMap",
                                new Object[]{persistValue.getEntry().getID()});
                    }
                    /*else {
                        logger.debug("persistValue[{}] is null or version[{},{}] is not match",
                                new Object[]{persistValue == null ? null : persistValue.getEntry().getID(), mirror.getVersion(), persistValue.getVersion()});
                    }*/
                }
                return null;
            }
        });
    }

    // 缓存关闭标识
    private final AtomicBoolean stop = new AtomicBoolean();

    /**
     * 关闭并写入db
     */
    public void stop() {
        logger.error("stop()启动，缓存被关闭，等待写入线程完成...");
        StopWatch watch = new StopWatch();
        watch.start();
        stop.set(true);

        for (final Segment segment : segments) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    segment.drainAllToPersistQueue();
                    segment.getPersistQueue().consumePersistQueue(true);
                }
            });
        }

        try {
            service.shutdown();

            // 阻塞等待线程完成, 90s时间
            // TODO 测试数值，一般不用写这么久数据
            service.awaitTermination(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }

        watch.stop();
        logger.error("stop()运行时间:{}ms", watch.getTime());
    }
}
