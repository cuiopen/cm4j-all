package com.cm4j.test.guava.consist.usage;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.DBState;
import com.cm4j.test.guava.consist.ListReference;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.usage.caches.TableAndNameCache;
import com.cm4j.test.guava.consist.usage.caches.TableAndNameVO;
import com.cm4j.test.guava.consist.usage.caches.TableIdCache;
import com.cm4j.test.guava.consist.usage.caches.TableValueCache;

/**
 * 1.缓存过期了不应该能修改状态? 使用引用队列？<br>
 * 2.单个对象的状态修改？ 不要放到应用中修改，放到SingleValue和ListValue中处理
 * 
 * @author Yang.hao
 * @since 2013-2-15 上午09:45:41
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:test_1/spring-ds.xml" })
public class ConcurrentCacheTest {

	@Test
	public void singleTest() {
		TestTable table = ConcurrentCache.getInstance().get(new TableIdCache(1)).get();
		TestTable table2 = ConcurrentCache.getInstance().get(new TableIdCache(1)).get();

		Assert.assertTrue(table == table2);

		TestTable test = new TestTable(6, (long) 6);
		TableIdCache desc = new TableIdCache(3);
		SingleReference<TestTable> reference = ConcurrentCache.getInstance().get(desc);
		reference.update(test);

		TestTable testTable = ConcurrentCache.getInstance().get(desc).get();
		Assert.assertTrue(testTable == test);

		reference.delete();
		Assert.assertNull(ConcurrentCache.getInstance().get(desc).get());
	}

	@Test
	public void multiTableGetTest() {
		TableAndNameVO tableAndName = ConcurrentCache.getInstance().get(new TableAndNameCache(1)).get();
		Assert.assertNotNull(tableAndName.getName());
	}

	@Test
	public void listTest() {
		TableValueCache desc = new TableValueCache(1);
		ListReference<TestTable> list = ConcurrentCache.getInstance().get(desc);
		Assert.assertTrue(list.get().size() > 0);

		// 基于desc搜索结果上的二次筛选
		TestTable table3 = desc.findById(3);
		Assert.assertNotNull(table3);
		// 基于desc搜索结果上的二次筛选
		TestTable table4 = desc.findById(4);
		Assert.assertNull(table4);

		TableValueCache desc2 = new TableValueCache(1);
		ListReference<TestTable> list2 = ConcurrentCache.getInstance().get(desc2);
		TestTable table = new TestTable(4, (long) 6);
		list2.update(table);
		list2.delete(table);
		list2.update(table);
	}

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

		// TODO 为什么不写入db就挂了？
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
					SingleReference<TestTable> reference = ConcurrentCache.getInstance().get(new TableIdCache(1));
					reference.get().increaseValue();
					reference.update(reference.get());
					// 为增加并发异常，暂停100ms
					// Thread.sleep(10);
				}
				barrier.await();
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void concurrentTest() throws InterruptedException, BrokenBarrierException {
		TableValueCache desc = new TableValueCache(1);
		ConcurrentCache.getInstance().get(desc);

		new Thread(new concurrentThread(), "update thread").start();
		System.out.println(ConcurrentCache.getInstance().contains(new TableIdCache(1)));
	}

	public class concurrentThread implements Runnable {
		@Override
		public void run() {
			SingleReference<TestTable> reference = ConcurrentCache.getInstance().get(new TableIdCache(1));
			reference.update(reference.get());
		}
	}
}