package com.cm4j.test.guava.consist;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.cm4j.test.guava.consist.entity.IEntity;

/**
 * 单个缓存值，与list相对应<br>
 * 例如数据库中的一行数据
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:25:24
 * 
 */
public abstract class CacheEntry {

	/**
	 * 缓存状态 - 默认持久化
	 */
	private volatile DBState dbState = DBState.P;
	/**
	 * 在更新队列中的数量
	 */
	private final AtomicInteger numInUpdateQueue = new AtomicInteger(0);
	
	private final Lock lock = new ReentrantLock();

	public void setDbState(DBState state) {
		lock.lock();
		try {
			this.dbState = state;
			if (state != DBState.P) {
				PersistCache.getInstance().sendToUpdateQueue(this);
			}
		} finally {
			lock.unlock();
		}
	}

	public DBState getDbState() {
		return dbState;
	}

	public AtomicInteger getNumInUpdateQueue() {
		return numInUpdateQueue;
	}
	
	public Lock getLock() {
		return lock;
	}

	/**
	 * 根据当前缓存对象解析hibernate entity进行数据保存
	 * 
	 * @return
	 */
	public abstract IEntity parseEntity();
}
