package com.cm4j.test.guava.consist.loader;

import org.apache.commons.lang.ArrayUtils;

import com.cm4j.test.guava.consist.cc.AbsReference;
import com.cm4j.test.guava.consist.keys.KEYS;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * 缓存值加载
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午03:34:58
 * 
 */
public class CacheValueLoader extends CacheLoader<String, AbsReference> {

	@SuppressWarnings("rawtypes")
	@Override
	public AbsReference load(String key) throws RuntimeException {
		String[] keyInfo = KEYS.getKeyInfo(key);
		String prefix = keyInfo[0];
		String[] params = (String[]) ArrayUtils.remove(keyInfo, 0);

		PrefixMappping mappping = PrefixMappping.valueOf(prefix);
		Preconditions.checkNotNull(mappping);

		try {
			// 这里配的是无参数对象
			Class<? extends CacheDefiniens<?>> clazz = mappping.getCacheDesc();
			CacheDefiniens desc = clazz.newInstance();
			
			AbsReference result = desc.load(params);
			Preconditions.checkNotNull(result);
			return result;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	/**
	 * 占位符，表明null值
	 */
	public static final NULL NULL_PH = new NULL();

	public static class NULL {
	}
}
