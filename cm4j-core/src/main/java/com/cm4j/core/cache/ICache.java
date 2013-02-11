package com.cm4j.core.cache;

import com.cm4j.core.schedule.ScheduledSource;

public interface ICache<K, V> extends ScheduledSource {

	/**
	 * 获取对象
	 * 
	 * @param key
	 * @return
	 */
	public Object get(K key);

	/**
	 * 放入对象
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Object put(K key, V value);

	/**
	 * 删除单个对象
	 * 
	 * @param key
	 */
	public void remove(K key);

	/**
	 * 清除所有元素
	 */
	public void clear();
	
	/**
	 * 缓存大小
	 * @return
	 */
	public int size();
}
