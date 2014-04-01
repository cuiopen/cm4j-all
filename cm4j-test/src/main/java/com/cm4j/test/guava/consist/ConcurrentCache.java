package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.loader.CacheValueLoader;
import com.cm4j.test.guava.consist.loader.PrefixMappping;
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
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final Segment segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
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
    private boolean changeDbStatePersist(final CacheEntry entry) {
        StopWatch watch = new Slf4JStopWatch();
        Preconditions.checkNotNull(entry.ref(), "CacheEntry中ref不允许为null");

        final String key = entry.ref().getAttachedKey();
        int hash = rehash(key.hashCode());
        boolean result = segmentFor(hash).doInSegmentUnderLock(key, hash, new SegmentLockHandler<Boolean>() {
            @Override
            public Boolean doInSegmentUnderLock(Segment segment, HashEntry e) {
                if (e != null && e.value != null && !isExipredAndAllPersist(e, Segment.now())) {
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
                if (e != null && e.value != null && !isExipredAndAllPersist(e, Segment.now())) {
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
