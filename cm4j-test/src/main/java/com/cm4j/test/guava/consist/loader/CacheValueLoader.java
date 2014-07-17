package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.cc.AbsReference;
import com.cm4j.test.guava.consist.cc.reflection.CCReflection;
import com.cm4j.test.guava.consist.cc.reflection.ConstructorStruct;
import com.cm4j.test.guava.consist.cc.reflection.ParamDefaultValue;
import com.cm4j.test.guava.consist.keys.KEYS;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;

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

		PrefixMappping mappping = PrefixMappping.valueOf(prefix);
		Preconditions.checkNotNull(mappping);

		try {
			// 这里配的是无参数对象
			Class<? extends CacheDefiniens<?>> clazz = mappping.getCacheDesc();
            List<ConstructorStruct> constructorStruct = CCReflection.getConstructorStruct(clazz);

            Preconditions.checkArgument(constructorStruct.size() == 1,
                    "缓存" + clazz.getSimpleName() + "必须有一个构造函数");

            CacheDefiniens desc = null;
            ConstructorStruct struct = constructorStruct.get(0);

            // 默认取第一个有参构造函数来构造对象
            if (struct.isHasParams()) {
                String[] params = (String[]) ArrayUtils.remove(keyInfo, 0);

                Class<?>[] paramsType = struct.getParamsType();
                Object[] paramDefaultValue = new Object[paramsType.length];
                for (int i = 0; i < paramsType.length; i++) {
                    paramDefaultValue[i] = ParamDefaultValue.get(paramsType[i]).translate(params[i]);
                }
                // 构造对象
                desc = (CacheDefiniens) struct.getConstructor().newInstance(paramDefaultValue);
            } else {
                desc = clazz.newInstance();
            }

			AbsReference result = desc.load();
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
