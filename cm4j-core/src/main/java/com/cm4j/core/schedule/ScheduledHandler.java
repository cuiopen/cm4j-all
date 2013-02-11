package com.cm4j.core.schedule;


/**
 * 定时处理器-具体业务处理
 * 
 * @author Yang.hao
 * @since 2012-3-16 下午04:04:22
 *
 * @param <K>
 * @param <V>
 */
public interface ScheduledHandler<T extends ScheduledSource> {

	public void exec(T cache);

	/**
	 * 执行异常处理
	 * 
	 * @param e
	 * @param cache
	 */
	public void exceptionCaught(Exception e, T cache);
}
