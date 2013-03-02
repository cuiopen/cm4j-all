package com.cm4j.test.guava.consist;


/**
 * 原生value接口，可设置任何对象为缓存，子类可对缓存对象进一步封装，{@link DBState}状态需要自己维护<br>
 * 缓存对象内的成员变量一致性需外部自行维护
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:30:17
 * 
 */
public interface IReference {

	/**
	 * 获取值
	 * 
	 * @param <V>
	 * @return
	 */
	<V> V get();
	
	/**
	 * 是否所有对象都与数据库保持一致<br>
	 * 如果是coll集合，则内部需维持锁的一致性
	 * 
	 * @return
	 */
	boolean isAllPersist();
}
