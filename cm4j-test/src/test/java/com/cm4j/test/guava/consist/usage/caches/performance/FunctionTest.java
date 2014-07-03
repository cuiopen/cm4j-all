package com.cm4j.test.guava.consist.usage.caches.performance;

import com.cm4j.test.guava.consist.caches.TmpFhhdCache;
import com.cm4j.test.guava.consist.cc.ConcurrentCache;
import com.cm4j.test.guava.consist.cc.SingleReference;
import com.cm4j.test.guava.consist.entity.TmpFhhd;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程异步短时间过期+写入测试
 * 
 * @author Yang.hao
 * @since 2013-3-6 上午10:12:38
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:test_1/spring-ds.xml" })
public class FunctionTest {

	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void funcTest() throws InterruptedException, BrokenBarrierException {
		int num = 5;
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		AtomicLong counter = new AtomicLong();
		for (int i = 0; i < num; i++) {
			new Thread(new randomThread(counter, barrier)).start();
		}
		barrier.await();
		long start = System.nanoTime();
		barrier.await();
		long end = System.nanoTime();

		ConcurrentCache.getInstance().stop();

		long writeEnd = System.nanoTime();

		System.out.println("======================");
		System.out.println("完成，数值sum为：" + counter.get());
		System.out.println("计算消耗时间：" + (double) (end - start) / 1000000000);
		System.out.println("写入消耗时间：" + (double) (writeEnd - end) / 1000000000);
	}

	public class randomThread implements Runnable {
		private AtomicLong counter;
		private CyclicBarrier barrier;

		public randomThread(AtomicLong counter, CyclicBarrier barrier) {
			this.counter = counter;
			this.barrier = barrier;
		}

		@Override
		public void run() {
			try {
				barrier.await();
				for (int i = 0; i < 50; i++) { // 执行20000次
					try {
						int random = RandomUtils.nextInt(1000);

                        SingleReference<TmpFhhd> ref = new TmpFhhdCache(random).ref();
                        synchronized (ref) {
							TmpFhhd fhhd = ref.get();
							if (fhhd == null) {
								counter.incrementAndGet();
								
								ref.update(new TmpFhhd(random, 1, 1, ""));

                                // todo persistAndRemove方法报错，为什么？
								ref.persistAndRemove();
							} else {
								double d = RandomUtils.nextDouble();
								if (d >= 0.2) { // >=0 一定成立，则无删除
									counter.incrementAndGet();
									
									fhhd.increaseValue();
									fhhd.update();

                                    // 直接persist需注释
									ref.persistAndRemove();
								} else {
									counter.addAndGet(-fhhd.getNCurToken());

									fhhd.delete();

                                    // 直接persist需注释
									ref.persistAndRemove();
								}
							}
						}

						// 为增加并发异常，暂停10ms
						// Thread.sleep(10);
					} catch (Exception e) {
						logger.error("THREAD ERROR[" + Thread.currentThread().getName() + "]", e);
					}
				}
				barrier.await();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
