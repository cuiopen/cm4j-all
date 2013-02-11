package com.cm4j.test.guava.consist;

/**
 * DB缓存状态
 * 
 * @author Yang.hao
 * @since 2013-1-14 下午06:47:59
 * 
 */
public enum DBState {
	/**
	 * persist - 持久状态
	 */
	P,
	/**
	 * delete - 删除
	 */
	D,
	/**
	 * save or update - 新增或更新
	 */
	U
}