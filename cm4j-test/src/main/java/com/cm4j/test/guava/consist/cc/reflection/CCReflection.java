package com.cm4j.test.guava.consist.cc.reflection;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

/**
 * Created by yanghao on 14-7-15.
 */
public class CCReflection {

    private static final Map<Class, List<ConstructorStruct>> constructorMap = Maps.newConcurrentMap();

    public static List<ConstructorStruct> getConstructorStruct(Class clazz) {
        List<ConstructorStruct> constructors = constructorMap.get(clazz);
        if (constructors == null) {
            constructors = reflectConstructors(clazz);
            constructorMap.put(clazz, constructors);
        }
        return constructors;
    }

    /**
     * 反射解析构造函数
     *
     * @param clazz
     */
    private static List<ConstructorStruct> reflectConstructors(Class clazz) {
        List<ConstructorStruct> constructorStructs = Lists.newArrayList();

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] typeParameters = constructor.getParameterTypes();

            if (typeParameters.length == 0) {
                constructorStructs.add(new ConstructorStruct(constructor));
            } else {
                constructorStructs.add(new ConstructorStruct(constructor, typeParameters));
            }
        }
        return constructorStructs;
    }
}
