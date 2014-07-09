package com.cm4j.test.guava.consist.cc;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Reference的一些公共方法
 *
 * @author Yang.hao
 * @since 2013-3-2 上午10:42:59
 */
public abstract class AbsReference {

    // 用于存放暂时未被删除对象
    // 里面对象只能被删除，不可更改状态
    private final Set<CacheEntry> deletedSet = new HashSet<CacheEntry>();

    /**
     * 此对象所依附的key
     */
    private String attachedKey;

    /**
     * 获取值
     *
     * @param <V>
     * @return
     */
    public abstract <V> V get();

    /**
     * 更新entry<br>
     * 因为子类有范型，类型兼容问题，所以子类多写一个update()方法来规定对象给外部调用，而此方法也转类型调用update()
     *
     * @param e
     */
    protected abstract void updateEntry(CacheEntry e);

    /**
     * 删除entry<br>
     * 因为子类有范型，类型兼容问题，所以子类多写一个delete()方法来规定对象给外部调用，而此方法也转类型调用update()
     *
     * @param e
     */
    protected abstract void deleteEntry(CacheEntry e);

    /**
     * 是否所有对象都与数据库保持一致(状态P)
     * 缓存过期是否可移除的判断条件之一<br />
     * <font color="red">此方法在lock下被调用</font>
     *
     * @return
     */
    protected boolean isAllPersist() {
        // TODO 这个是否要改成标识，而不是每次去循环判断？
        if (getDeletedSet().size() > 0) {
            return false;
        }
        return allPersist();
    }

    /**
     * 非deleteSet的数据是否都是P的状态
     * 缓存过期是否可移除的判断条件之一<br />
     * <font color="red">此方法在lock下被调用</font>
     *
     * @return
     */
    protected abstract boolean allPersist();

    /**
     * 非deleteSet数据保存，deleteSet中数据在persistAndRemove()已经处理了<br />
     * <font color="red">此方法在lock下被调用</font>
     * // todo 这里是否可不放到子类中实现，父类getNotDeletedSet可获得到所有entry
     */
    protected abstract void persistDB();

    /**
     * 缓存中单个对象的修改后更改此对象的状态，此方法在lock下被调用
     *
     * @param entry
     * @param dbState
     * @return
     */
    protected abstract boolean changeDbState(CacheEntry entry, DBState dbState);

    /**
     * 在从db获取数据之后，设置缓存内数据所属的key，用来辨识此对象是哪个缓存的
     *
     * @param attachedKey
     */
    protected abstract void attachedKey(String attachedKey);

    /**
     * 获取所有不在DeletedSet中的元素
     * @return
     */
    public abstract Set<CacheEntry> getNotDeletedSet();

    /**
     * 持久化但不移除
     */
    public void persist() {
        ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), false);
    }

    /**
     * 持久化后移除<br />
     * <pre><font color=red>
     * 这个方法一般是用于先移除缓存，然后手动改DB数值，最后缓存重新加载的就是更改后的数值
     *
     * 调用这个方法有可能导致报RuntimeException: 缓存中不存在此对象[$1_157]，无法更改状态
     * 原因：
     * 如果一个线程已经从缓存获取到数据ref，此时调用此方法会把ref从缓存remove，
     * 也就是缓存中已不存在此数据，如果持有线程再update或者delete，则无法更改缓存状态
     * </font></pre>
     */
    public void persistAndRemove() {
        ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), true);
    }

    /**
     * 将deleteSet中的对象持久化
     */
    protected void persistDeleteSet() {
        HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        // deleteSet数据处理
        for (CacheEntry v : getDeletedSet()) {
            hibernate.delete(v);
            changeDbState(v, DBState.P);
            // 占位：发送到更新队列，状态P
            // ConcurrentCache.getInstance().sendToPersistQueue(v);
            // 从保存队列里面移除
            ConcurrentCache.getInstance().removeFromPersistQueue(v);
        }
        getDeletedSet().clear();
    }

    /**
     * 数据在deleteSet，则状态只能改为P
     *
     * @param entry
     * @param dbState
     *         如果存在于deleteSet中，则状态必须为P，因为deleteSet中对象不能被修改，只能是在删除后被修改为P
     * @return 是否存在
     */
    protected boolean checkAndDealDeleteSet(CacheEntry entry, DBState dbState) {
        // deleteSet数据处理
        Iterator<CacheEntry> iter = getDeletedSet().iterator();
        while (iter.hasNext()) {
            CacheEntry v = iter.next();
            // 进入deleteSet的对象只能被写入，
            if (v == entry) {
                Preconditions.checkArgument(DBState.P == dbState, "对象被删除后不允许再修改");
                iter.remove();
                return true;
            }
        }
        return false;
    }

    protected String getAttachedKey() {
        return attachedKey;
    }

    protected void setAttachedKey(String attachedKey) {
        this.attachedKey = attachedKey;
        attachedKey(attachedKey);
    }

    public Set<CacheEntry> getDeletedSet() {
        return deletedSet;
    }
}
