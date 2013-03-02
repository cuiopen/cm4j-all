package com.cm4j.test.guava.consist;

/**
 * Reference的一些公共方法
 * 
 * @author Yang.hao
 * @since 2013-3-2 上午10:42:59
 * 
 */
public abstract class AbsReference {

	/**
	 * 此对象所依附的key
	 */
	private String attachedKey;

	/**
	 * 获取值
	 * 
	 * @param <V>
	 * @return
	 */
	public abstract <V> V get();

	/**
	 * 是否所有对象都与数据库保持一致<br>
	 * 如果是coll集合，则内部需维持锁的一致性
	 * 
	 * @return
	 */
	protected abstract boolean isAllPersist();

	/**
	 * 持久化到数据库，用于persistAndRemove()
	 */
	protected abstract void persistDB();

	/**
	 * 修改单个对象的状态
	 * 
	 * @param entry
	 * @param dbState
	 * @return
	 */
	protected abstract boolean changeDbState(CacheEntry entry, DBState dbState);

	/**
	 * 持久化
	 */
	public void persist() {
		ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), false);
	}

	/**
	 * 持久化后移除
	 */
	public void persistAndRemove() {
		ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), true);
	}

	protected String getAttachedKey() {
		return attachedKey;
	}

	protected void setAttachedKey(String attachedKey) {
		this.attachedKey = attachedKey;
	};

}
