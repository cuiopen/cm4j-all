package com.cm4j.test.guava.consist.loader;

import org.apache.commons.lang.ArrayUtils;

import com.cm4j.test.guava.consist.keys.KEYS;
import com.cm4j.test.guava.consist.value.IValue;
import com.google.common.base.Preconditions;

/**
 * 缓存值加载
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午03:34:58
 * 
 */
public class CacheValueLoader extends CacheLoader<String, IValue> {

	@Override
	public IValue load(String key) throws RuntimeException {
		String[] keyInfo = KEYS.getKeyInfo(key);
		String prefix = keyInfo[0];
		String[] params = (String[]) ArrayUtils.remove(keyInfo, 0);

		PrefixMappping mappping = PrefixMappping.valueOf(prefix);
		Preconditions.checkNotNull(mappping);

		CacheDes<? extends IValue> desc = mappping.getCacheDesc();
		return desc.load(params);
	}
}
