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

	/**
	 * expire设置DB状态为P，能获取到锁则修改
	 */
	void setDbPersist() {
		// 能获取到锁则修改
		if (lock.tryLock()) {
			try {
				this.dbState = DBState.P;
			} finally {
				lock.unlock();
			}
		}
	}

	AtomicInteger getNumInUpdateQueue() {
		return numInUpdateQueue;
	}

	public Lock getLock() {
		return lock;
	}

	public DBState getDbState() {
		return dbState;
	}

	/**
	 * 修改db状态，注意：缓存中必须有此对象才可修改db状态
	 * 
	 * @param state
	 */
	public void setDbState(DBState state) {
		lock.lock();
		try {
			this.dbState = state;
			if (state != DBState.P) {
				// TODO 缓存中不存在的时候不允许修改
//				if (StringUtils.isBlank(attachedKey) || !PersistCache.getInstance().contains(attachedKey)) {
//					throw new RuntimeException("缓存中不存在此对象，无法修改状态");
//				}
				PersistCache.getInstance().sendToUpdateQueue(this);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 根据当前缓存对象解析hibernate entity进行数据保存
	 * 
	 * @return
	 */
	protected abstract IEntity parseEntity();
}
