package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.AbsReference;
import com.cm4j.test.guava.consist.usage.caches.list.FieldFlagListCache;
import com.cm4j.test.guava.consist.usage.caches.list.TableValueListCache;
import com.cm4j.test.guava.consist.usage.caches.single.FieldFlagSingleCache;
import com.cm4j.test.guava.consist.usage.caches.single.TableAndNameCache;
import com.cm4j.test.guava.consist.usage.caches.single.TableIdCache;

/**
 * 缓存前缀与描述的映射
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午12:09:59
 * 
 */
public enum PrefixMappping {

	$1(new TableIdCache()),
	$2(new TableValueListCache()),
	$3(new TableAndNameCache()),
	$4(new FieldFlagSingleCache()),
	$5(new FieldFlagListCache());

	private CacheDescriptor<? extends AbsReference> cacheDesc;

	PrefixMappping(CacheDescriptor<? extends AbsReference> cacheDesc) {
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
			if (value.getCacheDesc().getClass().isAssignableFrom(cacheDesc.getClass())) {
				return value;
			}
		}
		return null;
	}

	public CacheDescriptor<? extends AbsReference> getCacheDesc() {
		return cacheDesc;
	}
}
