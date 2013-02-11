package com.cm4j.test.guava.consist.loader;

public abstract class CacheLoader<K, V> {

	/**
	 * 加载数据
	 * 
	 * @param key
	 * @return
	 * @throws RuntimeException
	 */
	public abstract V load(K key) throws RuntimeException;
}
