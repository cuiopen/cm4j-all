package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.keys.KEYS.JOINER;
import com.cm4j.test.guava.consist.value.IValue;
import com.google.common.base.Preconditions;

/**
 * 缓存信息描述
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午01:13:54
 * 
 */
public abstract class CacheDesc<V extends IValue> {

	private Object[] params;

	protected CacheDesc(Object... params) {
		this.params = params;
	}

	/**
	 * 从db加载
	 * 
	 * @param params
	 *            就是初始化的对象数组，在做key时转化为string数组
	 * @return
	 */
	public abstract V load(String... params);

	public String getKey() {
		Preconditions.checkArgument(params.length > 0, "cache entry param's size is 0");
		return new JOINER(PrefixMappping.getMapping(this).name(), params).key();
	}
}
