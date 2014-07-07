package com.cm4j.test.guava.consist.cc;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.constants.Constants;
import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.fifo.FIFOAccessQueue;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.collect.Lists;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import java.util.Collection;
import java.util.List;

/**
 * Created by yanghao on 14-4-1.
 */
public class DBPersistQueue {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final FIFOAccessQueue<CacheEntry> queue;
    private final Segment segment;

    /**
     * 更新队列消费计数器
     */
    public long counter = 0L;

    public DBPersistQueue(Segment segment) {
        this.segment = segment;
        this.queue = new FIFOAccessQueue<CacheEntry>();
    }

    /**
     * 发送到更新队列
     *
     * @param entry
     */
    public void sendToPersistQueue(CacheEntry entry) {
        // TODO 这里是个瓶颈，从queue移除是一个个遍历移除的，如果数据量很大，则会拖慢速度
        // 之前采用只加入不移除，则出现遍历队列太慢的问题
        // 现在改成移除再加入，则出现remove慢的问题
        // 保留：1w/s 如果下面注释，则5w/s

        // 最好是提供一个自动加入到队列尾部的高并发队列 [待扩展]

//        // 先从队列删除
//        queue.remove(entry);
//        // 重置persist信息
//        entry.mirror();
//        // 加入到队列尾部
//        queue.add(entry);


        // 这里是否要先从persistQueue remove再offer ？
        // 应该不用，这里是在锁下面执行的

        // 重置persist信息
        entry.mirror();
        queue.offer(entry);
    }

    public void removeFromPersistQueue(CacheEntry entry) {
        queue.remove(entry);
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
        int queueSize = queue.size();
        logger.error(this.segment + ":定时[{}]检测：缓存存储数据队列大小：[{}]，doNow：[{}]", new Object[]{currentCounter, queueSize, doNow});
        if (doNow || queueSize >= Constants.MAX_UNITS_IN_UPDATE_QUEUE || (currentCounter) % Constants.PERSIST_CHECK_INTERVAL == 0) {

            List<CacheEntry> toBatch = Lists.newArrayList();

            List<CacheEntry> entries;
            while (true) {
                entries = this.segment.drainUnderLock(Constants.BATCH_TO_COMMIT);
                if (entries.size() == 0) {
                    logger.error(this.segment + ":定时[{}]检测结束，queue内无数据", currentCounter);
                    break;
                }

                logger.debug(this.segment + ":缓存存储数据开始");

                for (CacheEntry wrapper : entries) {
                    wrapper.getIsChanged().set(true);

                    // 删除或者更新的num为0
                    IEntity entity = wrapper.mirrorEntity();
                    if (entity != null && DBState.P != wrapper.mirrorDBState()) {
                        toBatch.add(wrapper);
                    }
                }

                // 条件：在toBatch大于0的情况下 (或关系)：
                // 1.toBatch的大小达到 Constants.BATCH_TO_COMMIT
                // 2.queue为空

                // && (toBatch.size() % Constants.BATCH_TO_COMMIT == 0 || queue.size() == 0)
                if (toBatch.size() > 0 ) {
                    try {
                        logger.debug(this.segment + ":批处理大小：{}", toBatch.size());
                        persistNum += toBatch.size();
                        batchPersistData(toBatch);
                    } catch (Exception e) {
                        logger.error(this.segment + ":缓存批处理异常", e);
                    }
                    toBatch.clear();
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
    private void batchPersistData(Collection<CacheEntry> entities) {
        HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        Session session = hibernate.getSession();
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
        } catch (HibernateException exception) {
            tx.rollback();
            if (!(exception instanceof NonUniqueObjectException)) {
                // 在删除时，如果同一个key的不同对象删除，会报NonUniqueObjectException，但这种情况比较少
                // 可以参考：http://stackoverflow.com/questions/6518567/org-hibernate-nonuniqueobjectexception
                logger.error(this.segment + ":缓存批处理写入DB异常", exception);
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
                } catch (DataAccessException e1) {
                    logger.error(this.segment + ":批处理失败，单条更新失败", e1);
                }
            }
        } finally {
            try {
                session.close();
            } catch (HibernateException e) {
                logger.error(this.segment + ":批处理失败，session.close()异常", e);
            }
        }
    }

    public FIFOAccessQueue<CacheEntry> getQueue() {
        return queue;
    }
}
