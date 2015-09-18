package com.cm4j.test.guava.consist.loader;

import com.cm4j.test.guava.consist.cc.AbsReference;
import com.cm4j.test.guava.consist.cc.ConcurrentCache;
import com.cm4j.test.guava.consist.keys.KEYS;
import com.cm4j.test.guava.consist.keys.KEYS.JOINER;
import com.google.common.base.Preconditions;

/**
 * 缓存定义类
 * 
 * @author Yang.hao
 * @since 2013-1-19 下午01:13:54
 * 
 */
public abstract class CacheDefiniens<V extends AbsReference> {

    private final String key;

	/**
     * 本构造函数用于构建缓存Key，<font color=red>因此：子类必须调用此方法</font>
	 */
	protected CacheDefiniens(Object... params) {
		for (Object param : params) {
			KEYS.checkParam(param);
		}

        PrefixMappping mapping = PrefixMappping.getMapping(this);
        Preconditions.checkNotNull(mapping, "请先在PrefixMappping进行配置此缓存映射：" + this.getClass().getSimpleName());
        this.key = new JOINER(mapping.name(), params).key();
	}

	/**
	 * 从db加载
	 * 
	 * @return
	 */
	public abstract V load();

    /**
     * 在load之后调用
     * 子类可覆盖，用于没有则创建的业务
     *
     * @param ref
     */
    public void afterLoad(V ref){};

	public String getKey() {
		return this.key;
	}

	/**
	 * 查询缓存引用
	 * 
	 * @return
	 */
	public V ref() {
		return ConcurrentCache.getInstance().get(this);
	}

	public V refIfPresent() {
		return ConcurrentCache.getInstance().getIfPresent(this);
	}
}
