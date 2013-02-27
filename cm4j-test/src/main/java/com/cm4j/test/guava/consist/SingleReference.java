package com.cm4j.test.guava.consist;

/**
 * 单个缓存对象
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:31:51
 * 
 */
public class SingleReference<V extends CacheEntry> implements IReference {

	private V v;

	/**
	 * 此对象所依附的key
	 */
	private String attachedKey;

	public SingleReference(V value) {
		this.v = value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get() {
		return v;
	}

	/**
	 * 删除
	 * 
	 * @param e
	 */
	public void delete() {
		if (this.v == null) {
			throw new RuntimeException("SingleValue中不包含对象，无法删除");
		}
		// 注意顺序，先remove再change
		ConcurrentCache.getInstance().changeDbState(this.v, DBState.D);
		this.v = null;
	}

	/**
	 * 新增或修改
	 * 
	 * @param v
	 */
	public void update(V v) {
		v.setAttachedKey(attachedKey);
		this.v = v;
		ConcurrentCache.getInstance().changeDbState(this.v, DBState.U);
	}

	@Override
	public boolean isAllPersist() {
		return DBState.P == v.getDbState();
	}

	@Override
	public void setAttachedKey(String attachedKey) {
		this.attachedKey = attachedKey;
		if (v != null) {
			v.setAttachedKey(attachedKey);
		}
	}

	public String getAttachedKey() {
		return attachedKey;
	};
}