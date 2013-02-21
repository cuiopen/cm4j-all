package com.cm4j.test.guava.consist.value;

import com.cm4j.test.guava.consist.CacheEntry;
import com.cm4j.test.guava.consist.DBState;
import com.cm4j.test.guava.consist.PersistCache;

/**
 * 单个缓存对象
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:31:51
 * 
 */
public class SingleReference<V extends CacheEntry> implements IValue {
	
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
		if (v == null) {
			throw new RuntimeException("SingleValue中不包含对象，无法删除");
		}
		// 注意顺序，先remove再change
		PersistCache.getInstance().changeDbState(v, DBState.D);
		// TODO 需要拷贝新对象，以防止对象=null？
		v = null;
	}

	/**
	 * 新增或修改
	 * 
	 * @param v
	 */
	public void saveOrUpdate(V v) {
		v.setAttachedKey(attachedKey);
		this.v = v;
		PersistCache.getInstance().changeDbState(this.v, DBState.U);
	}


	@Override
	public boolean isAllPersist() {
		return DBState.P == v.getDbState();
	}

	@Override
	public void persist() {
		// TODO 持久化
		PersistCache.getInstance().changeDbState(this.v, DBState.P);
	}

	@Override
	public void setAttachedKey(String attachedKey) {
		this.attachedKey = attachedKey;
		if (v != null){
			v.setAttachedKey(attachedKey);
		}
	}

	public String getAttachedKey() {
		return attachedKey;
	};
}