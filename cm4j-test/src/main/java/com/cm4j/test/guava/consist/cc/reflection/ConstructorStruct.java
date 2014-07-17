package com.cm4j.test.guava.consist.cc.reflection;

import java.lang.reflect.Constructor;

/**
 * 构造函数结构
 * <p>
 * Created by yanghao on 14-7-15.
 */
public class ConstructorStruct {

    /**
     * 构造函数
     */
    private final Constructor constructor;

    /**
     * 参数类型list
     */
    private Class<?>[] paramsType;

    public ConstructorStruct(Constructor constructor) {
        this.constructor = constructor;
    }

    public ConstructorStruct(Constructor constructor, Class<?>[] paramsType) {
        this.constructor = constructor;
        this.paramsType = paramsType;
    }

    public Constructor getConstructor() {
        return constructor;
    }

    public boolean isHasParams() {
        return paramsType != null && paramsType.length > 0;
    }

    public Class<?>[] getParamsType() {
        return paramsType;
    }
}
