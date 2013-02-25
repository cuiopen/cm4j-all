package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.IReference;
import com.cm4j.test.guava.consist.usage.caches.TableAndNameCache;
import com.cm4j.test.guava.consist.usage.caches.TableIdCache;
import com.cm4j.test.guava.consist.usage.caches.TableValueCache;

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

	private CacheDescriptor<? extends IReference> cacheDesc;

	PrefixMappping(CacheDescriptor<? extends IReference> cacheDesc) {
		this.cacheDesc = cacheDesc;
	}

	/**
	 * 根据描述找到对应class的类
	 * 
	 * @param cacheDesc
	 * @return
	 */
	public static PrefixMappping getMapping(CacheDescriptor<? extends IReference> cacheDesc) {
		PrefixMappping[] values = values();
		for (PrefixMappping value : values) {
			if (value.getCacheDesc().getClass().isAssignableFrom(cacheDesc.getClass())) {
				return value;
			}
		}
		return null;
	}

	public CacheDescriptor<? extends IReference> getCacheDesc() {
		return cacheDesc;
	}
}
