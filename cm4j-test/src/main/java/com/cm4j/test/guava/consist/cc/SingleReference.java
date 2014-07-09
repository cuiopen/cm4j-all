package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.Set;

/**
 * 单个缓存对象，一个reference只能存放一次对象
 *
 * @author Yang.hao
 * @since 2013-1-18 上午09:31:51
 */
public class SingleReference<V extends CacheEntry> extends AbsReference {

    private V v;

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
    protected boolean changeDbState(CacheEntry entry, DBState dbState) {
        // deleteSet中数据状态修改
        if (checkAndDealDeleteSet(entry, dbState)) {
            return true;
        }

        Preconditions.checkArgument(this.v == entry, "缓存内对象不一致");

        entry.changeDbState(dbState);
        // v对象处理
        if (DBState.D == dbState) {
            getDeletedSet().add(this.v);
            this.v = null;
        }
        return true;
    }

    @Override
    public Set<CacheEntry> getNotDeletedSet() {
        HashSet<CacheEntry> set = new HashSet<CacheEntry>();
        if (this.v != null) {
            set.add(this.v);
        }
        return set;
    }
}