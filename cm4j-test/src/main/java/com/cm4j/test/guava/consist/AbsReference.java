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
	 * 是否所有对象都与数据库保持一致，缓存过期是否可移除的判断条件之一，此方法在lock下被调用<br>
	 * 
	 * @return
	 */
	protected abstract boolean isAllPersist();

	/**
	 * 持久化到数据库，用于persistAndRemove()，此方法在lock下被调用
	 */
	protected abstract void persistDB();

	/**
	 * 缓存中单个对象的修改后更改此对象的状态，此方法在lock下被调用
	 * 
	 * @param entry
	 * @param dbState
	 * @return
	 */
	protected abstract boolean changeDbState(CacheEntry entry, DBState dbState);

	/**
	 * 在从db获取数据之后，设置缓存内数据所属的key，用来辨识此对象是哪个缓存的
	 * 
	 * @param attachedKey
	 */
	protected abstract void attachedKey(String attachedKey);

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
