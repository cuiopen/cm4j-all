package com.cm4j.core.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cm4j.core.cache.ExpiredLocalCache;
import com.cm4j.core.cache.ICache;

/**
 * 计划处理器 - 使用{@link ScheduledExecutorService}调用{@link ScheduledHandler}来处理
 * {@link ScheduledSource}
 * 
 * @author Yang.hao
 * @since 2012-3-16 下午04:28:38
 * 
 * @param <T>
 */
public class ScheduledProcessor {

	private ScheduledSource source;
	private ScheduledExecutorService e;
	private final R r;

	/**
	 * 构造函数
	 * 
	 * @param handler
	 *            定时处理handler
	 * @param delay
	 *            定时处理中间的延迟时间
	 * @param expire
	 *            数据过期时间
	 * @param unit
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ScheduledProcessor(ScheduledExecutorService e, final ScheduledHandler handler,
			ScheduledSource source) {
		this.source = source;
		this.e = e;
		r = new R(handler);
		Runtime.getRuntime().addShutdownHook(new Thread(r));
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(long initialDelay, long delay, TimeUnit unit) {
		return e.scheduleWithFixedDelay(this.r, initialDelay, delay, unit);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(long initialDelay, long period, TimeUnit unit) {
		return e.scheduleAtFixedRate(r, initialDelay, period, unit);
	}

	public ScheduledSource getScheduledResource() {
		return source;
	}

	/**
	 * 处理线程类
	 * 
	 * @author Yang.hao
	 * @since 2012-3-16 下午04:06:43
	 * 
	 */
	private class R implements Runnable {
		private ScheduledHandler<ScheduledSource> handler;

		public R(ScheduledHandler<ScheduledSource> handler) {
			this.handler = handler;
		}

		@Override
		public void run() {
			try {
				handler.exec(source);
			} catch (Exception e) {
				handler.exceptionCaught(e, source);
			}
		}
	}

	/**
	 * main 方法测试
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// 创建缓存
		ICache<Integer, Integer> cache = new ExpiredLocalCache<Integer, Integer>(10, TimeUnit.MILLISECONDS);

		// 新建缓存处理器
		ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
		ScheduledHandler<ICache<Integer, Integer>> handler = new ScheduledHandler<ICache<Integer, Integer>>() {
			@Override
			public void exec(ICache<Integer, Integer> cache) {
				System.out.println("cache size:" + cache.size());
			}

			@Override
			public void exceptionCaught(Exception e, ICache<Integer, Integer> cache) {
			}
		};
		ScheduledProcessor processor = new ScheduledProcessor(e, handler, cache);

		processor.scheduleWithFixedDelay(1L, 500, TimeUnit.MILLISECONDS);

		long current = System.nanoTime();
		for (int i = 0; i < 5000000; i++) {
			cache.put(i, i);
			cache.get(i);

			if (i % 100000 == 0) {
				System.out.println("put -> " + i);
			}
		}
		long end = System.nanoTime();
		System.out.println("time used:" + (end - current) / 1000000);
	}
}
