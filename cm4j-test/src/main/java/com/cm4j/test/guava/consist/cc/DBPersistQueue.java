package com.cm4j.test.guava.consist.cc;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.constants.Constants;
import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import javax.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yanghao on 14-4-1.
 */
public class DBPersistQueue {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final HibernateDao hibernate;
    private final Segment segment;

    private final ConcurrentHashMap<String, CacheMirror> map;
    // 写入锁[persistAndRemove需要用到写入锁]
    private final Lock writeLock = new ReentrantLock();
    /**
     * 更新队列消费计数器
     */
    public long counter = 0L;

    public DBPersistQueue(Segment segment) {
        this.segment = segment;
        map = new ConcurrentHashMap<>();
        hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
    }

    /**
     * 发送到更新队列
     *
     * @param mirror
     */
    public void sendToPersistQueue(CacheMirror mirror) {
        // 重置persist信息
        map.put(mirror.getDbKey(), mirror);
    }

    public void persistImmediately(Collection<CacheEntry> del, Collection<CacheEntry> up) {
        HashSet<Object[]> delSet = Sets.newHashSet();
        HashSet<Object[]> upSet = Sets.newHashSet();

        HashSet<String> removeKey = Sets.newHashSet();
        for (CacheEntry e : del) {
            CacheMirror mirror = e.mirror();
            delSet.add(new Object[]{mirror.getEntity(), DBState.D});
            removeKey.add(mirror.getDbKey());
        }
        for (CacheEntry e : up) {
            CacheMirror mirror = e.mirror();
            upSet.add(new Object[]{mirror.getEntity(), DBState.U});
            removeKey.add(mirror.getDbKey());
        }

        writeLock.lock();
        try {
            for (String key : removeKey) {
                // 重置persist信息
                map.remove(key);
            }

            batchPersistData(delSet);
            batchPersistData(upSet);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 获取N个对象
     *
     * @param size
     * @return
     */
    public Set<CacheMirror> drain(int size) {
        Enumeration<String> keys = map.keys();
        int i = 0;
        Set result = Sets.newHashSet();
        while (keys.hasMoreElements() && i++ < size) {
            String key = keys.nextElement();
            CacheMirror value = map.remove(key);

            // 过滤为P的对象
            if (value != null && DBState.P != value.getDbState()) {
                result.add(value);
            }
        }

        return result;
    }

    /**
     * 修改队列内数据持久化
     * <p/>
     * 条件 或 关系：
     * 1.doNow=true
     * 2.修改队列内数据个数达到 Constants.MAX_UNITS_IN_UPDATE_QUEUE
     * 3.持久化次数达到 Constants.PERSIST_CHECK_INTERVA
     *
     * @param doNow 是否立即写入
     */
    public void consumePersistQueue(boolean doNow) {
        int persistNum = 0;

        long currentCounter = counter++;
        int queueSize = map.size();
        logger.error(this.segment + ":定时[{}]检测：缓存存储数据队列大小：[{}]，doNow：[{}]", new Object[]{currentCounter, queueSize, doNow});
        if (doNow || queueSize >= Constants.MAX_UNITS_IN_UPDATE_QUEUE || (currentCounter) % Constants.PERSIST_CHECK_INTERVAL == 0) {

            Set<CacheMirror> entries;
            while (true) {
                writeLock.lock();

                entries = drain(Constants.BATCH_TO_COMMIT);
                int size;
                if ((size = entries.size()) == 0) {
                    logger.error("{}:定时[{}]检测结束，queue内无数据", this.segment, currentCounter);
                    break;
                }

                logger.debug(this.segment + ":缓存存储数据开始，size：" + map.size());

                try {
                    if (entries.size() > 0) {
                        try {
                            logger.debug(this.segment + ":批处理大小：{}", size);
                            persistNum += size;
                            Collection<Object[]> transform = Collections2.transform(entries, new Function<CacheMirror, Object[]>() {
                                @Override
                                public Object[] apply(CacheMirror input) {
                                    return new Object[]{input.getEntity(), input.getDbState()};
                                }
                            });
                            batchPersistData(transform);
                        } catch (Exception e) {
                            logger.error(this.segment + ":缓存批处理异常", e);
                        }
                    }
                } finally {
                    writeLock.unlock();
                }

                for (CacheMirror mirror : entries) {
                    // 注意：这里不要放到lock内
                    // 因为改状态是要锁定segment的，放到lock内，则出现大锁套小锁，有概率会导致死锁
                    // recheck,有可能又有其他线程更新了对象，此时也不能重置为P
                    ConcurrentCache.getInstance().changeDbStatePersist(mirror.getCacheEntry(), mirror.getVersion());
                }

                if (entries.size() < Constants.BATCH_TO_COMMIT) {
                    logger.error(this.segment + ":定时[{}]检测结束，数量不足退出", currentCounter);
                    break;
                }
            }
        }
        logger.error(this.segment + ":定时[{}]检测结束，本次存储大小为{}", currentCounter, persistNum);
    }

    /**
     * 批处理写入数据
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void batchPersistData(Collection<Object[]> entities) {
        try {
            for (Object[] entry : entities) {
                try {
                    IEntity e = (IEntity) entry[0];
                    DBState dbState = (DBState) entry[1];
                    if (DBState.U == dbState) {
                        try {
                            // 这里之前报DataIntegrityViolationException
                            // 是因为，没做好并发控制，persistAndRemove()方法先保存了，但hibernate里慢了一步，也判断要save
                            // 因此persistAndRemove()方法中要先从队列移除，再persist

                            // todo 去除mirrorDbState
                            // 猜测：是不是dbState对象的状态不对？？ 不要mirrorDbState了。
                            hibernate.saveOrUpdate(e);
                        } catch (ConstraintViolationException e1) {
                            e1.printStackTrace();
                            hibernate.save(e);
                        }
                    } else if (DBState.D == dbState) {
                        hibernate.delete(e);
                    }
                } catch (DataAccessException e1) {
                    logger.error("", e1);
                }
            }
        } catch (Exception e) {
            logger.error(this.segment + ":批处理失败", e);
        }
    }

    public ConcurrentHashMap<String, CacheMirror> getMap() {
        return map;
    }
}
