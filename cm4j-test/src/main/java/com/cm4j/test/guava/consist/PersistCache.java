package com.cm4j.test.guava.consist;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.loader.CacheDesc;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.loader.CacheValueLoader;
import com.cm4j.test.guava.consist.value.IValue;

/**
 * 持久化缓存
 * 
 * <pre>
 * 读写分离：
 * 读取：{@link ConcurrentCache}提供get or load、put、expire操作
 * 
 * 写入：是由{@link CacheEntry#setDbState(DBState)}控制对象状态
 * 同时{@link PersistCache}独立维护了一份写入队列，独立于缓存操作
 * 
 * </pre>
 * 
 * @author Yang.hao
 * @since 2013-1-14 下午05:39:49
 * 
 */
public class PersistCache {

	private static class Holder {
		public static final PersistCache instance = new PersistCache(new CacheValueLoader());
	}

	public static PersistCache getInstance() {
		return Holder.instance;
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ConcurrentCache<String, IValue> cache;
	private final ConcurrentLinkedQueue<CacheEntry> updateQueue;
	private final ScheduledExecutorService service;

	private PersistCache(CacheLoader<String, IValue> loader) {
		// 缓存初始化
		cache = new ConcurrentCache<String, IValue>(loader);

		// 更新队列
		updateQueue = new ConcurrentLinkedQueue<CacheEntry>();

		// 定时处理器
		service = Executors.newScheduledThreadPool(1);
		service.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				consumeUpdateQueue();
			}
		}, 1, 5, TimeUnit.SECONDS);
	}

	/**
	 * 获取，没有则加载
	 * 
	 * @param key
	 * @return
	 * @throws ExecutionException
	 *             loading时异常
	 */
	@SuppressWarnings("unchecked")
	public <V extends IValue> V get(CacheDesc<V> desc) {
		return (V) cache.get(desc.getKey());
	}

	/**
	 * 加入缓存，但控制db状态
	 * 
	 * @param <V>
	 * @param desc
	 * @param value
	 * @throws Exception
	 */
	public <V extends IValue> void put(CacheDesc<V> desc, V value) {
		cache.put(desc.getKey(), value);
	}

	/**
	 * 从缓存中移除
	 * 
	 * @param <V>
	 * @param desc
	 */
	public <V extends IValue> void remove(CacheDesc<V> desc) {
		cache.remove(desc.getKey());
	}

	/**
	 * 是否包含缓存
	 * 
	 * @param <V>
	 * @param cacheDesc
	 * @return
	 */
	public <V extends IValue> boolean contains(CacheDesc<V> cacheDesc) {
		return cache.containsKey(cacheDesc.getKey());
	}

	/**
	 * 是否包含缓存
	 * 
	 * @param <V>
	 * @param cacheDesc
	 * @return
	 */
	public <V extends IValue> boolean contains(String key) {
		return cache.containsKey(key);
	}

	public void stop() {
		// TODO
	}

	/**
	 * 发送到更新队列
	 * 
	 * @param entry
	 */
	void sendToUpdateQueue(CacheEntry entry) {
		entry.getNumInUpdateQueue().incrementAndGet();
		updateQueue.add(entry);
	}

	/**
	 * 将更新队列发送给db存储<br>
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void consumeUpdateQueue() {
		logger.warn("缓存定时存储数据，队列大小：{}", updateQueue.size());
		CacheEntry entry = null;
		while ((entry = updateQueue.poll()) != null) {
			int num = entry.getNumInUpdateQueue().decrementAndGet();
			// 删除或者更新的num为0
			// 注意：这里要使用CacheEntryWrapper#DbState，而不要用CacheEntry的DbState
			if (num == 0) {
				IEntity entity = entry.parseEntity();
				if (entity != null) {
					// TODO 发送db去批处理
					System.out.println(entry.getDbState() + " " + entity.toString());

					HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
					hibernate.saveOrUpdate(entity);

					entry.setDbPersist();
				}
			}
		}
	}

	/*
	 * static class CacheEntryWrapper { private CacheEntry entry; private
	 * DBState dbState;
	 * 
	 * private CacheEntryWrapper(CacheEntry entry) { this.entry = entry; // new
	 * 一个CacheEntryWrapper来保存每个entry的dbState this.dbState = entry.getDbState();
	 * }
	 * 
	 * public CacheEntry getEntry() { return entry; }
	 * 
	 * public DBState getDbState() { return dbState; } }
	 */
}
