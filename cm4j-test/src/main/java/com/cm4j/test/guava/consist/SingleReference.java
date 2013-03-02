package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
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

	/*
	 * ===================== public methods =====================
	 */

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
		this.v.setAttachedKey(getAttachedKey());
		ConcurrentCache.getInstance().changeDbState(this.v, DBState.D);
		this.v = null;
	}

	/**
	 * 新增或修改
	 */
	public void update(V v) {
		Preconditions.checkNotNull(v);
		this.v = v;
		this.v.setAttachedKey(getAttachedKey());
		ConcurrentCache.getInstance().changeDbState(this.v, DBState.U);
	}

	/*
	 * ================== extend methods ====================
	 */

	@Override
	protected boolean isAllPersist() {
		return DBState.P == v.getDbState();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void persistDB() {
		HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		CacheEntry entry = this.v;
		IEntity entity = entry.parseEntity();
		if (DBState.U == entry.getDbState()) {
			hibernate.saveOrUpdate(entity);
		} else if (DBState.D == entry.getDbState()) {
			hibernate.delete(entity);
		}
		entry.setDbState(DBState.P);
		// 占位：发送到更新队列，状态P
		ConcurrentCache.getInstance().sendToUpdateQueue(entry);
	}

	@Override
	protected boolean changeDbState(CacheEntry entry, DBState dbState) {
		Preconditions.checkArgument(entry == v, "不是同一对象，无法更改状态");
		entry.changeDbState(dbState);
		return true;
	}
}