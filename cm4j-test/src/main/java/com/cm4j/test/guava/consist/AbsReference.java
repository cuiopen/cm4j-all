package com.cm4j.test.guava.consist;

/**
 * Reference的一些公共方法
 * 
 * @author Yang.hao
 * @since 2013-3-2 上午10:42:59
 * 
 */
public abstract class AbsReference implements IReference {

	/**
	 * 此对象所依附的key
	 */
	private String attachedKey;

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

	public String getAttachedKey() {
		return attachedKey;
	}

	public void setAttachedKey(String attachedKey) {
		this.attachedKey = attachedKey;
	};
}
