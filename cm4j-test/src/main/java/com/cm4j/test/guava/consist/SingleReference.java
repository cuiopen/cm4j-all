package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 单个缓存对象，一个reference只能存放一次对象
 *
 * @author Yang.hao
 * @since 2013-1-18 上午09:31:51
 */
public class SingleReference<V extends CacheEntry> extends AbsReference {

    private V v;

    // 用于存放暂时未被删除对象
    // 里面对象只能被删除，不可更改状态
    private Set<V> deletedSet = new HashSet<V>();

    public SingleReference(V value) {
        this.v = value;
    }

	/*
     * ===================== public methods =====================
	 */

    @SuppressWarnings("unchecked")
    @Override
    public V get() {
        return v;
    }

    /**
     * 新增或修改<br>
     * 注意：修改的对象必须是 {@link SingleReference#v} 被删除的对象不允许再被修改
     */
    public void update(V v) {
        Preconditions.checkNotNull(v);
        if (this.v == null) {
            // 代表v是新增的
            v.resetRef(this);
        } else if (this.v != v) {
            // 已存在对象且对象不一致
            throw new RuntimeException("SingleReference中对象已存在，不允许修改其他对象");
        }
        this.v = v;
        ConcurrentCache.getInstance().changeDbState(this.v, DBState.U);
    }

    /**
     * 从db删除，而不是移除缓存
     */
    public void delete() {
        Preconditions.checkNotNull(this.v, "SingleValue中不包含对象，无法删除");

        // 注意顺序，先remove再change
        ConcurrentCache.getInstance().changeDbState(this.v, DBState.D);
    }

    /*
     * ================== extend methods under lock ====================
     */
    @Override
    protected void updateEntry(CacheEntry e) {
        @SuppressWarnings("unchecked")
        V v = (V) e;
        this.update(v);
    }

    @Override
    protected void deleteEntry(CacheEntry e) {
        @SuppressWarnings("unchecked")
        V v = (V) e;
        Preconditions.checkArgument(this.v == v);
        this.delete();
    }

    @Override
    protected boolean isAllPersist() {
        if (deletedSet.size() > 0) {
            return false;
        }
        if (this.v != null && DBState.P != this.v.getDbState()) {
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void persistDB() {
        HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        // deleteSet数据处理
        for (V v : deletedSet) {
            hibernate.delete(v);
            changeDbState(v, DBState.P);
            // entry.setDbState(DBState.P);
            // 占位：发送到更新队列，状态P
            ConcurrentCache.getInstance().sendToUpdateQueue(v);
        }
        deletedSet.clear();

        // v数据处理
        // 有可能对象被删除到deletedSet，entry则为null
        if (this.v != null) {
            IEntity entity = this.v.parseEntity();
            if (DBState.U == this.v.getDbState()) {
                hibernate.saveOrUpdate(entity);
            } else if (DBState.D == this.v.getDbState()) {
                hibernate.delete(entity);
            }
            changeDbState(this.v, DBState.P);
            // entry.setDbState(DBState.P);
            // 占位：发送到更新队列，状态P
            ConcurrentCache.getInstance().sendToUpdateQueue(this.v);
        }
    }

    @Override
    protected boolean changeDbState(CacheEntry entry, DBState dbState) {
        // deleteSet数据处理
        Iterator<V> itor = deletedSet.iterator();
        while (itor.hasNext()) {
            V v = (V) itor.next();
            // 进入deleteSet的对象只能被写入，
            if (v == entry) {
                Preconditions.checkArgument(DBState.P == dbState, "对象被删除后不允许再修改");
                itor.remove();
                return true;
            }
        }

        Preconditions.checkArgument(this.v == entry, "缓存内对象不一致");

        entry.changeDbState(dbState);
        // v对象处理
        if (DBState.D == dbState) {
            this.deletedSet.add(this.v);
            this.v = null;
        }
        return true;
    }

    @Override
    protected void attachedKey(String attachedKey) {
        if (this.v != null) {
            this.v.resetRef(this);
        }
    }
}