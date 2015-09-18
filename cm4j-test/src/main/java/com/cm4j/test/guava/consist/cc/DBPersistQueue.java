package com.cm4j.test.guava.consist.cc;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.constants.Constants;
import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import javax.validation.ConstraintViolationException;
import java.util.*;
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

    private final ConcurrentHashMap<String, CacheEntry> map;
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
     * @param entry
     */
    public void sendToPersistQueue(CacheEntry entry) {
        // 重置persist信息
        entry.mirror();
        map.put(entry.getID(), entry);
    }

    public void persistImmediately(Collection<CacheEntry> del, Collection<CacheEntry> up) {
        HashSet<CacheEntry> all = Sets.newHashSet();
        all.addAll(del);
        all.addAll(up);

        for (CacheEntry e : all) {
            e.mirror();
        }

        writeLock.lock();
        try {
            for (CacheEntry e : all) {
                // 重置persist信息
                String key = e.getID();
                map.remove(key);
            }

            batchPersistData(del);
            batchPersistData(up);
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
    public Set<CacheEntry> drain(int size) {
        Enumeration<String> keys = map.keys();
        int i = 0;
        Set result = Sets.newHashSet();
        while (keys.hasMoreElements() && i++ < size) {
            String key = keys.nextElement();
            CacheEntry value = map.remove(key);
            if (value != null) {
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

            List<CacheEntry> toBatch = Lists.newArrayList();

            Set<CacheEntry> entries;
            while (true) {
                writeLock.lock();

                entries = drain(Constants.BATCH_TO_COMMIT);
                if (entries.size() == 0) {
                    logger.error(this.segment + ":定时[{}]检测结束，queue内无数据", currentCounter);
                    break;
                }

                logger.debug(this.segment + ":缓存存储数据开始，size：" + map.size());

                try {
                    for (CacheEntry wrapper : entries) {
                        // 删除或者更新的num为0
                        IEntity entity = wrapper.mirrorEntity();
                        if (entity != null && DBState.P != wrapper.mirrorDBState()) {
                            toBatch.add(wrapper);
                        }
                    }

                    if (toBatch.size() > 0) {
                        try {
                            logger.debug(this.segment + ":批处理大小：{}", toBatch.size());
                            persistNum += toBatch.size();
                            batchPersistData(toBatch);
                        } catch (Exception e) {
                            logger.error(this.segment + ":缓存批处理异常", e);
                        }
                    }
                } finally {
                    writeLock.unlock();
                }

                if (toBatch.size() > 0) {
                    for (CacheEntry wrapper : toBatch) {
                        // 注意：这里不要放到lock内
                        // 因为改状态是要锁定segment的，放到lock内，则出现大锁套小锁，有概率会导致死锁
                        // recheck,有可能又有其他线程更新了对象，此时也不能重置为P
                        ConcurrentCache.getInstance().changeDbStatePersist(wrapper);
                    }
                }

                // 清空List对象
                toBatch.clear();

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
    private void batchPersistData(Collection<CacheEntry> entities) {
        try {
            for (CacheEntry wrapper : entities) {
                try {
                    DBState dbState = wrapper.mirrorDBState();
                    IEntity e = wrapper.mirrorEntity();
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


        /*Session session = hibernate.getSession();
        Transaction tx = session.beginTransaction();
        try {
            int idx = 0;

            for (CacheEntry wrapper : entities) {
                DBState dbState = wrapper.mirrorDBState();
                IEntity entity = wrapper.mirrorEntity();

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
            for (CacheEntry wrapper : entities) {
                // recheck,有可能又有其他线程更新了对象，此时也不能重置为P
                ConcurrentCache.getInstance().changeDbStatePersist(wrapper);
            }
        } catch (Exception exception) {
            if (!(exception instanceof NonUniqueObjectException)) {
                // 在删除时，如果同一个key的不同对象删除，会报NonUniqueObjectException，但这种情况比较少
                // 可以参考：http://stackoverflow.com/questions/6518567/org-hibernate-nonuniqueobjectexception
                logger.error(this.segment + ":缓存批处理写入DB异常", exception);
            }
            try {
                tx.rollback();
            } catch (HibernateException e) {
                e.printStackTrace();
            }
            for (CacheEntry wrapper : entities) {
                try {
                    DBState dbState = wrapper.mirrorDBState();
                    IEntity entity = wrapper.mirrorEntity();

                    if (DBState.U == dbState) {
                        hibernate.saveOrUpdate(entity);
                    } else if (DBState.D == dbState) {
                        hibernate.delete(entity);
                    }
                    // recheck,有可能又有其他线程更新了对象，此时也不能重置为P
                    ConcurrentCache.getInstance().changeDbStatePersist(wrapper);
                } catch (Exception e1) {
                    logger.error(this.segment + ":批处理失败，单条[" + wrapper.ref().getAttachedKey() + "]更新失败", e1);
                }
            }
        } finally {
            try {
                session.close();
            } catch (Exception e) {
                logger.error(this.segment + ":批处理失败，session.close()异常", e);
            }
        }*/
    }

    public ConcurrentHashMap<String, CacheEntry> getMap() {
        return map;
    }
}
