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

	private Object[] params;

	protected CacheDefiniens() {
	}
	
	/**
	 * 子类可调用本方法，用可变数值做参数<br>
	 * 或者新建一个无参构造函数用于{@link PrefixMappping}映射，有参构造函数则指明具体参数类型
	 */
	protected CacheDefiniens(Object... params) {
		for (Object param : params) {
			KEYS.checkParam(param);
		}
		this.params = params;
	}

	/**
	 * 从db加载
	 * 
	 * @param params
	 *            就是初始化的对象数组，在做key时转化为string数组
	 * @return
	 */
	public abstract V load(String... params);

	public String getKey() {
		PrefixMappping mapping = PrefixMappping.getMapping(this);
		Preconditions.checkNotNull(mapping, "此缓存未在PrefixMappping进行映射：" + this.getClass().getSimpleName());
		return new JOINER(mapping.name(), params).key();
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

	/**
	 * 忽略之前的数值重新加载DB中的最新值
	 * 
	 * @return
	 */
	public V refresh() {
		return ConcurrentCache.getInstance().refresh(this);
	}
}
