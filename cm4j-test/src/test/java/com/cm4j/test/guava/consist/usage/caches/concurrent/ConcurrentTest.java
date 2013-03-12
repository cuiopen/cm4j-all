package com.cm4j.test.guava.consist.usage.caches.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.usage.caches.single.TableIdCache;

/**
 * 并发测试
 * 
 * @author Yang.hao
 * @since 2013-3-6 上午10:12:46
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:test_1/spring-ds.xml" })
public class ConcurrentTest {

	@Test
	public void multiTest() throws InterruptedException, BrokenBarrierException {
		int num = 2;
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		for (int i = 0; i < num; i++) {
			new Thread(new addThread(barrier)).start();
		}
		barrier.await();
		long start = System.nanoTime();
		barrier.await();
		long end = System.nanoTime();

		ConcurrentCache.getInstance().stop();

		SingleReference<TestTable> singleValue = ConcurrentCache.getInstance().get(new TableIdCache(1));

		System.out.println("=========" + singleValue.get().getNValue());
		System.out.println((double) (end - start) / 1000000000);
	}

	public class addThread implements Runnable {
		private CyclicBarrier barrier;

		public addThread(CyclicBarrier barrier) {
			this.barrier = barrier;
		}

		@Override
		public void run() {
			try {
				barrier.await();
				for (int i = 0; i < 20000; i++) {
					SingleReference<TestTable> reference = new TableIdCache(1).reference();
					reference.get().increaseValue();
					reference.update(reference.get());
					// 为增加并发异常，暂停100ms
					// Thread.sleep(10);
				}
				barrier.await();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}