package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.TableIdCache;
import com.cm4j.test.guava.consist.TableAndNameCache;
import com.cm4j.test.guava.consist.TableValueCache;
import com.cm4j.test.guava.consist.value.IValue;

/**
 * 缓存前缀与描述的映射
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午12:09:59
 * 
 */
public enum PrefixMappping {

	$1(new TableIdCache()),
	$2(new TableValueCache()),
	$3(new TableAndNameCache());

	private CacheDescriptor<? extends IValue> cacheDesc;

	PrefixMappping(CacheDescriptor<? extends IValue> cacheDesc) {
		this.cacheDesc = cacheDesc;
	}

	/**
	 * 根据描述找到对应class的类
	 * 
	 * @param cacheDesc
	 * @return
	 */
	public static PrefixMappping getMapping(CacheDescriptor<? extends IValue> cacheDesc) {
		PrefixMappping[] values = values();
		for (PrefixMappping value : values) {
			if (value.getCacheDesc().getClass().isAssignableFrom(cacheDesc.getClass())) {
				return value;
			}
		}
		return null;
	}

	public CacheDescriptor<? extends IValue> getCacheDesc() {
		return cacheDesc;
	}
}
