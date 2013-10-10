package com.cm4j.test.guava.consist.usage.caches.performance;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TestTable;
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
public class ListFunctionTest {

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

		System.out.println("======================");
		System.out.println("完成，总运行个数：" + counter.get());
		System.out.println((double) (end - start) / 1000000000);
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
				for (int i = 0; i < 20000; i++) { // 执行20000次
					int random = 0;
					while (random < 100) {
						random = RandomUtils.nextInt(1000);
					}
					if (random >= 100) {
						SingleReference<TestTable> ref = null; // new TableIdCache(random).ref();

						synchronized (counter) {
							TestTable testTable = ref.get();
							if (testTable == null) {
								ref.update(new TestTable(random, 1L));
								long num = counter.incrementAndGet();
								// logger.debug("new 新对象,总计 = {}",num);
							} else {
								double d = RandomUtils.nextDouble();
								if (d >= 0.8) {
									testTable.increaseValue();
									ref.update(testTable);
									long num = counter.incrementAndGet();
									// logger.debug("对象+1,总计 = {}", num);
								} else {
									ref.delete();
									counter.addAndGet(-testTable.getNValue());
									// logger.debug("对象 {} 被删除",testTable.getNId());
								}
							}
						}

						// 为增加并发异常，暂停10ms
						Thread.sleep(10);
					}
				}
				barrier.await();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
