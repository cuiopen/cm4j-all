package com.cm4j.test.guava.consist.value;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cm4j.test.guava.consist.CacheEntry;
import com.cm4j.test.guava.consist.ConcurrentCache;
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
public class ListReference<E extends CacheEntry> implements IValue {
	private CopyOnWriteArrayList<E> all_objects = new CopyOnWriteArrayList<E>();

	/**
	 * 此对象所依附的key
	 */
	private String attachedKey;

	/**
	 * 初始化
	 * 
	 * @param all_objects
	 */
	public ListReference(List<E> all_objects) {
		if (all_objects == null) {
			throw new IllegalArgumentException("cache wrap must not be null");
		}
		this.all_objects.addAll(all_objects);
	}

	/**
	 * 获取，如果要增删，不要直接对list操作，应调用{@link #delete(CacheEntry)},
	 * {@link #saveOrUpdate(CacheEntry)}
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<E> get() {
		return all_objects;
	}

	/**
	 * 删除
	 * 
	 * @param e
	 */
	public void delete(E e) {
		if (!all_objects.contains(e)) {
			throw new RuntimeException("ListValue中不包含此对象，无法删除");
		}
		// 注意顺序，先remove再change
		ConcurrentCache.getInstance().changeDbState(e, DBState.D, false);
		all_objects.remove(e);
	}

	/**
	 * 新增或修改
	 * 
	 * @param e
	 */
	public void saveOrUpdate(E e) {
		if (!all_objects.contains(e)) {
			e.setAttachedKey(attachedKey);
			all_objects.add(e);
		}
		ConcurrentCache.getInstance().changeDbState(e, DBState.U, false);
	}

	@Override
	public boolean isAllPersist() {
		for (E e : all_objects) {
			if (DBState.P != e.getDbState()) {
				return false;
			}
		}
		return true;
	}

	public String getAttachedKey() {
		return attachedKey;
	}

	@Override
	public void setAttachedKey(String attachedKey) {
		this.attachedKey = attachedKey;
		for (E e : all_objects) {
			e.setAttachedKey(attachedKey);
		}
	}
}
