package com.cm4j.test.guava.consist.usage.caches.performance;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.usage.caches.single.TableIdCache;

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
		int num = 2;
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		AtomicInteger counter = new AtomicInteger();
		for (int i = 0; i < num; i++) {
			new Thread(new randomThread(counter, barrier)).start();
		}
		barrier.await();
		long start = System.nanoTime();
		barrier.await();
		long end = System.nanoTime();

		ConcurrentCache.getInstance().stop();

		System.out.println("======================");
		System.out.println("完成，总运行个数：" + counter.get());
		System.out.println((double) (end - start) / 1000000000);
	}

	public class randomThread implements Runnable {
		private AtomicInteger counter;
		private CyclicBarrier barrier;

		public randomThread(AtomicInteger counter, CyclicBarrier barrier) {
			this.counter = counter;
			this.barrier = barrier;
		}

		@Override
		public void run() {
			try {
				barrier.await();
				for (int i = 0; i < 20000; i++) { // 执行20000次
					int random = 0;
					while (random < 100) {
						random = RandomUtils.nextInt(1000);
					}
					if (random >= 100) {
						SingleReference<TestTable> reference = new TableIdCache(random).reference();
						reference.get().increaseValue();
						reference.update(reference.get());

						logger.debug("线程+1,now = {}", counter.incrementAndGet());

						// 为增加并发异常，暂停50ms
						Thread.sleep(50);
					}
				}
				barrier.await();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
