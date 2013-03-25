package com.cm4j.test.guava.consist;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

/**
 * 推荐使用{@link ListReference}来查询，在进行map进行索引<br>
 * 注意：此方法未进行详细测试
 * 
 * @author Yang.hao
 * @since 2013-3-4 下午06:54:56
 * 
 * @param <K>
 * @param <V>
 */
public class MapReference<K, V extends CacheEntry> extends AbsReference {

	private Map<K, V> map = new ConcurrentHashMap<K, V>();

	public MapReference(Map<K, V> map) {
		Preconditions.checkNotNull(map);
		this.map.putAll(map);
	}

	/*
	 * ===================== public methods =====================
	 */

	@SuppressWarnings("unchecked")
	@Override
	public Map<K, V> get() {
		return map;
	}

	public V get(K key) {
		return map.get(key);
	}

	public void put(K key, V value) {
		if (!map.containsKey(key)) {
			value.setAttachedKey(getAttachedKey());
			map.put(key, value);
		}
		ConcurrentCache.getInstance().changeDbState(value, DBState.U);
	}

	public void delete(K key) {
		V v = map.get(key);
		Preconditions.checkNotNull(v, "MapValue中不包含此对象，无法删除");
		// 注意顺序，先remove再change
		ConcurrentCache.getInstance().changeDbState(v, DBState.D);
		map.remove(key);
	}

	/*
	 * ================== extend methods ====================
	 */

	@Override
	protected boolean isAllPersist() {
		for (V e : map.values()) {
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
		for (CacheEntry entry : map.values()) {
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
		for (CacheEntry cacheEntry : map.values()) {
			if (cacheEntry == entry) {
				cacheEntry.changeDbState(dbState);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void attachedKey(String attachedKey) {
		for (CacheEntry v : map.values()) {
			v.setAttachedKey(getAttachedKey());
		}
	}
}
