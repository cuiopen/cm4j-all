package com.cm4j.test.guava.consist.value;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cm4j.test.guava.consist.CacheEntry;
import com.cm4j.test.guava.consist.DBState;

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
public class ListValue<E extends CacheEntry> implements IValue {
	private CopyOnWriteArrayList<E> all_objects = new CopyOnWriteArrayList<E>();

	/**
	 * 初始化
	 * 
	 * @param all_objects
	 */
	public ListValue(List<E> all_objects) {
		if (all_objects == null) {
			throw new IllegalArgumentException("cache wrap must not be null");
		}
		this.all_objects.addAll(all_objects);
		for (E cached : all_objects) {
			cached.setDbState(DBState.P);
		}
	}

	/**
	 * 获取
	 * 
	 * @return
	 */
	public List<E> getAllObjects() {
		return all_objects;
	}

	/**
	 * 删除
	 * 
	 * @param e
	 */
	public void delete(E e) {
		if (!all_objects.contains(e)) {
			throw new RuntimeException("cache object is not exist,can not delete it");
		}
		all_objects.remove(e);
		e.setDbState(DBState.D);
	}

	/**
	 * 新增或修改
	 * 
	 * @param e
	 */
	public void saveOrUpdate(E e) {
		if (!all_objects.contains(e)) {
			all_objects.add(e);
		}
		e.setDbState(DBState.U);
	}

	@Override
	public boolean isAllPersist() {
		for (E e : all_objects) {
			e.getLock().lock();
		}
		try {
			for (E e : all_objects) {
				if (DBState.P != e.getDbState()) {
					return false;
				}
			}
			return true;
		} finally {
			for (E e : all_objects) {
				e.getLock().unlock();
			}
		}
	}
}
