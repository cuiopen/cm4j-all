package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.AbsReference;
import com.cm4j.test.guava.consist.caches.TmpFhhdCache;
import com.cm4j.test.guava.consist.caches.TmpListMultikeyListCache;
import com.cm4j.test.guava.consist.caches.TmpListMultikeyMapCache;

/**
 * 缓存前缀与描述的映射
 *
 * @author Yang.hao
 * @since 2013-1-19 下午12:09:59
 */
public enum PrefixMappping {

    // 不直接用对象，是为了防止构造函数设置的参数不清除，导致其他缓存查询问题
    $1(TmpFhhdCache.class),
    $2(TmpListMultikeyListCache.class),
    $3(TmpListMultikeyMapCache.class);

    private Class<? extends CacheDefiniens<?>> cacheDesc;

    PrefixMappping(Class<? extends CacheDefiniens<?>> cacheDesc) {
        this.cacheDesc = cacheDesc;
    }

    /**
     * 根据描述找到对应class的类
     *
     * @param cacheDef
     * @return
     */
    public static PrefixMappping getMapping(CacheDefiniens<? extends AbsReference> cacheDef) {
        PrefixMappping[] values = values();
        for (PrefixMappping value : values) {
            if (value.getCacheDesc().isAssignableFrom(cacheDef.getClass())) {
                return value;
            }
        }
        return null;
    }

    public Class<? extends CacheDefiniens<?>> getCacheDesc() {
        return cacheDesc;
    }
}
