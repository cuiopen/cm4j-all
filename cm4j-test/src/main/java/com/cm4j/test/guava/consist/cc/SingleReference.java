package com.cm4j.test.guava.consist.cc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

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

    public void update(V v) {
        updateEntry(v);
    }

    /**
     * 从db删除，而不是移除缓存
     */
    public void delete() {
        deleteEntry(this.v);
    }

    @Override
    public Set<CacheEntry> getNotDeletedSet() {
        HashSet<CacheEntry> set = Sets.newHashSet();
        if (this.v != null) {
            set.add(this.v);
        }
        return set;
    }

    /*
     * ================== extend methods under lock ====================
     */
    @Override
    protected void _update(CacheEntry e) {
        V v = (V) e;
        if (this.v == null) {
            // 代表v是新增的
            v.resetRef(this);
        } else if (this.v != v) {
            // 已存在对象且对象不一致
            throw new RuntimeException("SingleReference中对象已存在，不允许修改其他对象");
        }
        this.v = v;
    }

    @Override
    protected void _delete(CacheEntry e) {
        V v = (V) e;
        Preconditions.checkArgument(this.v == v, "对象不一致，无法删除");
        this.v = null;
    }
}