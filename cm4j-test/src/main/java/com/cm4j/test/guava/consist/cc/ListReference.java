package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * list 缓存对象建议使用此类，避免对状态的操作<br>
 * 此类为线程安全的
 *
 * @param <V>
 * @author Yang.hao
 * @since 2013-1-18 上午10:25:04
 */
public class ListReference<V extends CacheEntry> extends AbsReference {
    private final CopyOnWriteArrayList<V> all_objects = new CopyOnWriteArrayList<V>();

    /**
     * 初始化
     */
    public ListReference(List<V> all_objects) {
        Preconditions.checkNotNull(all_objects);
        this.all_objects.addAll(all_objects);
    }

	/*
     * ===================== public methods =====================
	 */

    /**
     * 获取，如果要增删，不要直接对list操作，应调用{@link #deleteEntry(CacheEntry)},
     * {@link #update(CacheEntry)}
     *
     * @return 不可更改的list，以防止外部破坏内部结构和状态
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<V> get() {
        return ImmutableList.copyOf(all_objects);
    }

    /**
     * 新增或修改
     */
    public void update(V v) {
        updateEntry(v);
    }

    @Override
    public Set<CacheEntry> getAllEntries() {
        return new HashSet<CacheEntry>(all_objects);
    }

	/*
     * ================== extend methods ====================
	 */

    @Override
    protected void _update(CacheEntry e) {
        V v = (V) e;
        if (!all_objects.contains(v)) {
            // 新增的
            v.resetRef(this);
            all_objects.add(v);
        }
        changeDbState(v, DBState.U);
    }

    @Override
    protected void _delete(CacheEntry e) {

    }
}
