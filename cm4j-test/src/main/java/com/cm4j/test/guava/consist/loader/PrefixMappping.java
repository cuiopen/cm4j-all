package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.AbsReference;
import com.cm4j.test.guava.consist.usage.caches.cc.TmpFhhdCache;
import com.cm4j.test.guava.consist.usage.caches.single.TableIdCache;

/**
 * 缓存前缀与描述的映射
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午12:09:59
 * 
 */
public enum PrefixMappping {

	$1(TmpFhhdCache.class),
	$2(TableIdCache.class);


	private Class<? extends CacheDescriptor<?>> cacheDesc;

	PrefixMappping(Class<? extends CacheDescriptor<?>> cacheDesc) {
		this.cacheDesc = cacheDesc;
	}

	/**
	 * 根据描述找到对应class的类
	 * 
	 * @param cacheDesc
	 * @return
	 */
	public static PrefixMappping getMapping(CacheDescriptor<? extends AbsReference> cacheDesc) {
		PrefixMappping[] values = values();
		for (PrefixMappping value : values) {
			if (value.getCacheDesc().isAssignableFrom(cacheDesc.getClass())) {
				return value;
			}
		}
		return null;
	}

	public Class<? extends CacheDescriptor<?>> getCacheDesc() {
		return cacheDesc;
	}
}
