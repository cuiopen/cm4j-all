package com.cm4j.test.guava.consist.value;

import com.cm4j.test.guava.consist.CacheEntry;
import com.cm4j.test.guava.consist.DBState;

/**
 * 单个缓存对象
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:31:51
 * 
 */
public abstract class SingleValue extends CacheEntry implements IValue {

	@Override
	public boolean isAllPersist() {
		return DBState.P == getDbState();
	}
}
