package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * list 缓存对象建议使用此类，避免对状态的操作<br>
 * 此类为线程安全的
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午10:25:04
 * 
 * @param <V>
 */
public class ListReference<V extends CacheEntry> extends AbsReference {
	private final CopyOnWriteArraySet<V> all_objects = new CopyOnWriteArraySet<V>();

	// 用于存放暂时未被删除对象，里面对象只能被删除，不可更改状态
	private final Set<V> deletedSet = new HashSet<V>();

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
	 * 获取，如果要增删，不要直接对list操作，应调用{@link #delete(CacheEntry)},
	 * {@link #update(CacheEntry)}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<V> get() {
		return all_objects;
	}

	/**
	 * 新增或修改
	 */
	public void update(V v) {
		if (!all_objects.contains(v)) {
			// 新增的
			v.resetRef(this);
			all_objects.add(v);
		}
		ConcurrentCache.getInstance().changeDbState(v, DBState.U);
	}

	/**
	 * 删除
	 */
	public void delete(V e) {
		Preconditions.checkState(all_objects.contains(e), "ListValue中不包含此对象，无法删除");
		// 注意顺序，先remove再change
		ConcurrentCache.getInstance().changeDbState(e, DBState.D);
	}

	/*
	 * ================== extend methods ====================
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
		this.delete(v);
	}

	@Override
	protected boolean isAllPersist() {
		if (deletedSet.size() > 0) {
			return false;
		}
		for (V e : all_objects) {
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
		// 先deleteSet，后all_objects
		// deleteSet数据处理
		for (V v : deletedSet) {
			hibernate.delete(v);
		}
		deletedSet.clear();

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

	@Override
	protected boolean changeDbState(CacheEntry entry, DBState dbState) {
		// deleteSet中如果为P，则从deleteSet中删除，以减少对象
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

		for (V e : all_objects) {
			if (e == entry) {
				e.changeDbState(dbState);

				if (DBState.D == dbState) {
					this.deletedSet.add(e);
					this.all_objects.remove(e);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	protected void attachedKey(String attachedKey) {
		for (CacheEntry v : all_objects) {
			v.resetRef(this);
		}
	}
}
