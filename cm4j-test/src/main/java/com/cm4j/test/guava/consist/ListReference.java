package com.cm4j.test.guava.consist;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.google.common.base.Preconditions;

/**
 * list 缓存对象建议使用此类，避免对状态的操作<br>
 * 此类为线程安全的
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午10:25:04
 * 
 * @param <E>
 * @param <C>
 */
public class ListReference<E extends CacheEntry> extends AbsReference {
	private final CopyOnWriteArrayList<E> all_objects = new CopyOnWriteArrayList<E>();

	/**
	 * 初始化
	 */
	public ListReference(List<E> all_objects) {
		Preconditions.checkNotNull(all_objects);
		this.all_objects.addAll(all_objects);
	}

	/*
	 * ===================== public methods =====================
	 */

	/**
	 * 获取，如果要增删，不要直接对list操作，应调用{@link #delete(CacheEntry)},
	 * {@link #update(CacheEntry)}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<E> get() {
		return all_objects;
	}

	/**
	 * 新增或修改
	 */
	public void update(E e) {
		if (!all_objects.contains(e)) {
			e.setAttachedKey(getAttachedKey());
			all_objects.add(e);
		}
		ConcurrentCache.getInstance().changeDbState(e, DBState.U);
	}

	/**
	 * 删除
	 */
	public void delete(E e) {
		Preconditions.checkState(!all_objects.contains(e), "ListValue中不包含此对象，无法删除");
		// 注意顺序，先remove再change
		ConcurrentCache.getInstance().changeDbState(e, DBState.D);
		all_objects.remove(e);
	}

	/*
	 * ================== extend methods ====================
	 */

	@Override
	protected boolean isAllPersist() {
		for (E e : all_objects) {
			if (DBState.P != e.getDbState()) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void persistDB() {
		HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		for (CacheEntry entry : all_objects) {
			if (DBState.P != entry.getDbState()) {
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
		}
	}

	protected boolean changeDbState(CacheEntry entry, DBState dbState) {
		for (CacheEntry cacheEntry : all_objects) {
			if (cacheEntry == entry) {
				cacheEntry.changeDbState(dbState);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void attachedKey(String attachedKey) {
		for (CacheEntry v : all_objects) {
			v.setAttachedKey(getAttachedKey());
		}
	}
}
