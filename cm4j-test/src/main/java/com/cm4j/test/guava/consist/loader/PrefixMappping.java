package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.TestCacheById;
import com.cm4j.test.guava.consist.TestCacheByValue;
import com.cm4j.test.guava.consist.value.IValue;

/**
 * 缓存前缀与描述的映射
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午12:09:59
 * 
 */
public enum PrefixMappping {

	$1(new TestCacheById()),
	$2(new TestCacheByValue());

	private CacheDes<? extends IValue> cacheDesc;

	PrefixMappping(CacheDes<? extends IValue> cacheDesc) {
		this.cacheDesc = cacheDesc;
	}

	/**
	 * 根据描述找到对应class的类
	 * 
	 * @param cacheDesc
	 * @return
	 */
	public static PrefixMappping getMapping(CacheDes<? extends IValue> cacheDesc) {
		PrefixMappping[] values = values();
		for (PrefixMappping value : values) {
			if (value.getCacheDesc().getClass().isAssignableFrom(cacheDesc.getClass())) {
				return value;
			}
		}
		return null;
	}

	public CacheDes<? extends IValue> getCacheDesc() {
		return cacheDesc;
	}
}
