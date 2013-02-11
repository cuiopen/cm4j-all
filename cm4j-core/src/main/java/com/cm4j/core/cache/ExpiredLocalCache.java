package com.cm4j.core.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于过期时间策略的本地缓存
 * 
 * @author Yang.hao
 * @since 2012-3-13 下午03:44:43
 * 
 */
public class ExpiredLocalCache<K, V> implements ICache<K, V> {
	private static final long serialVersionUID = 1L;

	private final long expireMillis;

	// 缓存
	private final ConcurrentHashMap<K, V> cached = new ConcurrentHashMap<K, V>();
	// 缓存对应的过期时间
	private final ConcurrentHashMap<K, Long> expired = new ConcurrentHashMap<K, Long>(1000);

	public ExpiredLocalCache(long expire, TimeUnit unit) {
		this.expireMillis = unit.toMillis(expire);
		Thread t = new Thread(new RemoveExpired());
		t.setDaemon(true);
		t.start();
	}

	public V put(K key, V value) {
		// 放入过期时间
		expired.put(key, System.currentTimeMillis() + expireMillis);
		cached.put(key, value);
		return value;
	};

	@Override
	public V get(K key) {
		return get(key, false);
	}

	/**
	 * @param key
	 * @param prolongExpireTime
	 *            过期时间后延
	 * @return
	 */
	public V get(K key, boolean prolongExpireTime) {
		if (removeExpired(key)) {
			return null;
		}
		// 过期时间后延
		if (prolongExpireTime) {
			expired.replace(key, System.currentTimeMillis() + expireMillis);
		}
		return cached.get(key);
	}

	@Override
	public void remove(K key) {
		cached.remove(key);
		expired.remove(key);
	}

	@Override
	public void clear() {
		cached.clear();
		expired.clear();
	}

	/**
	 * 删除过期元素
	 * 
	 * @param key
	 * @return
	 */
	private boolean removeExpired(K key) {
		Long oldTime = expired.get(key);
		if (oldTime != null && System.currentTimeMillis() > oldTime) {
			remove(key);
			return true;
		}
		return false;
	}

	public class RemoveExpired implements Runnable {
		@Override
		public void run() {
			long sleep;
			// 大于1s的1000ms清理一次
			if (expireMillis > 1000) {
				sleep = 1000;
			} else {
				sleep = expireMillis / 4;
			}
			for (;;) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
				}
				Set<K> keySet = cached.keySet();
				for (K key : keySet) {
					removeExpired(key);
				}
			}
		}
	}

	@Override
	public int size() {
		return cached.size();
	}

	@Override
	public String toString() {
		return "cacheSize:" + cached.size() + ",expireSize:" + expired.size();
	}

	/**
	 * main 方法测试
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		ExpiredLocalCache<Integer, Integer> cache = new ExpiredLocalCache<Integer, Integer>(10, TimeUnit.MILLISECONDS);
		long current = System.nanoTime();
		for (int i = 0; i < 5000000; i++) {
			cache.put(i, i);
			cache.get(i);
			if (i % 100000 == 0) {
				System.out.println(i + "->" + cache);
			}
		}
		long end = System.nanoTime();
		System.out.println("time used:" + (end - current) / 1000000);

		Thread.sleep(1000);
		System.out.println("cache result:" + cache);
	}
}
