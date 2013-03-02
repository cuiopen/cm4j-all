package com.cm4j.test.guava.consist.loader;

import org.apache.commons.lang.ArrayUtils;

import com.cm4j.test.guava.consist.AbsReference;
import com.cm4j.test.guava.consist.keys.KEYS;
import com.google.common.base.Preconditions;

/**
 * 缓存值加载
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午03:34:58
 * 
 */
public class CacheValueLoader extends CacheLoader<String, AbsReference> {

	@Override
	public AbsReference load(String key) throws RuntimeException {
		String[] keyInfo = KEYS.getKeyInfo(key);
		String prefix = keyInfo[0];
		String[] params = (String[]) ArrayUtils.remove(keyInfo, 0);

		PrefixMappping mappping = PrefixMappping.valueOf(prefix);
		Preconditions.checkNotNull(mappping);

		// 这里配的是无参数对象
		CacheDescriptor<? extends AbsReference> desc = mappping.getCacheDesc();
		return desc.load(params);
	}
}
