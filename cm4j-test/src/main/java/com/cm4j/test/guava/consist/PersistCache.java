package com.cm4j.test.guava.consist;

import java.text.MessageFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.cm4j.test.guava.consist.loader.CacheLoader;
import com.cm4j.test.guava.consist.loader.CacheValueLoader;
import com.cm4j.test.guava.consist.loader.PrefixMappping;
import com.cm4j.test.guava.consist.value.IValue;
import com.google.common.base.Preconditions;

/**
 * 持久化缓存
 * 
 * <pre>
 * 读写分离：
 * 读取：{@link ConcurrentCache}提供get or load、put、expire操作
 * 
 * 写入：是由{@link CacheEntry#changeDbState(DBState)}控制对象状态
 * 同时{@link PersistCache}独立维护了一份写入队列，独立于缓存操作
 * 
 * 使用流程：
 * 1.定义缓存描述信息{@link CacheDescriptor}
 * 2.定义映射{@link PrefixMappping}
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
	private final ConcurrentCache cache;
	private final ConcurrentLinkedQueue<CacheEntry> updateQueue;
	private final ScheduledExecutorService service;

	private final Runnable consumeRunnable;

	private PersistCache(CacheLoader<String, IValue> loader) {
		// 缓存初始化
		cache = new ConcurrentCache(loader);

		// 更新队列
		updateQueue = new ConcurrentLinkedQueue<CacheEntry>();

		// 更新队列处理线程
		consumeRunnable = new Runnable() {
			@Override
			public void run() {
				consumeUpdateQueue();
			}
		};

		// 定时处理器
		service = Executors.newScheduledThreadPool(1);
		service.scheduleAtFixedRate(consumeRunnable, 1, 50, TimeUnit.SECONDS);
	}

	/*
	 * ================= api ==================
	 */

	/**
	 * 获取，没有则加载
	 * 
	 * @param key
	 * @return
	 * @throws ExecutionException
	 *             loading时异常
	 */
	@SuppressWarnings("unchecked")
	public <V extends IValue> V get(CacheDescriptor<V> desc) {
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
	public void put(CacheDescriptor<? extends IValue> desc, IValue value) {
		cache.put(desc.getKey(), value);
	}

	/**
	 * 从缓存中移除
	 * 
	 * @param <V>
	 * @param desc
	 */
	public void remove(CacheDescriptor<? extends IValue> desc) {
		cache.remove(desc.getKey());
	}

	/**
	 * 先持久化再移除
	 * 
	 * @param <V>
	 * @param desc
	 */
	public void persistAndRemove(CacheDescriptor<? extends IValue> desc) {
		if (contains(desc)) {
			IValue v = get(desc);
			while (!v.isAllPersist()) {
				// TODO 先持久化再remove，有可能改了之后有其他线程又修改了:放到segment中
				// 如何定位到updateQueue中的最后一个，并将它置为P,以防止再次写入:直接插入P做占位
				// 移除后，再获取修改，这是索引又从0开始，怎么办:用P做占位如：4,3,2,P,3,2,1,U
				v.persist();
			}
			remove(desc);
		}
	}

	/**
	 * 是否包含缓存
	 * 
	 * @param <V>
	 * @param cacheDesc
	 * @return
	 */
	public boolean contains(CacheDescriptor<? extends IValue> cacheDesc) {
		return cache.containsKey(cacheDesc.getKey());
	}

	/**
	 * 是否包含缓存
	 * 
	 * @param <V>
	 * @param cacheDesc
	 * @return
	 */
	public boolean contains(String key) {
		return cache.containsKey(key);
	}

	public String stats() {
		String pattern = "cache size:{0},update queue:{1}";
		return MessageFormat.format(pattern, cache.size(), updateQueue.size());
	}

	/*
	 * ================== utils =====================
	 */
	/**
	 * 修改db状态
	 */
	public void changeDbState(CacheEntry entry, DBState dbState) {
		Preconditions.checkNotNull(entry.getAttachedKey(), "CacheEntry's attachedKey must not null");
		cache.changeDbState(entry, dbState);

		// TODO copy 一个新对象？
		// 是否应该放到segment中？
		if (dbState != DBState.P) {
			sendToUpdateQueue(entry);
		}
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

	public void stop() {
		Future<?> future = service.submit(consumeRunnable);
		try {
			// 阻塞等待线程完成
			future.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		service.shutdown();
	}

	/**
	 * 将更新队列发送给db存储<br>
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
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

					changeDbState(entry, DBState.P);
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
