package com.cm4j.test.guava.consist;

import com.google.common.base.Preconditions;

/**
 * 单个缓存对象
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:31:51
 * 
 */
public class SingleReference<V extends CacheEntry> extends AbsReference {

	private V v;

	public SingleReference(V value) {
		this.v = value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get() {
		return v;
	}

	/**
	 * 删除
	 */
	public void delete() {
		Preconditions.checkNotNull(this.v, "SingleValue中不包含对象，无法删除");
		// 注意顺序，先remove再change
		ConcurrentCache.getInstance().changeDbState(this.v, DBState.D);
		this.v = null;
	}

	/**
	 * 新增或修改
	 */
	public void update(V v) {
		Preconditions.checkNotNull(v);
		v.setAttachedKey(getAttachedKey());
		this.v = v;
		ConcurrentCache.getInstance().changeDbState(this.v, DBState.U);
	}

	@Override
	public boolean isAllPersist() {
		return DBState.P == v.getDbState();
	}

	@Override
	public void setAttachedKey(String attachedKey) {
		super.setAttachedKey(attachedKey);
		if (v != null) {
			v.setAttachedKey(attachedKey);
		}
	}
}