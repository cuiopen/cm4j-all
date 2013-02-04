package com.cm4j.test.guava.consist;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDesc;
import com.cm4j.test.guava.consist.value.ListValue;

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
public class PersistCacheTest {

	@Test
	public void testGet() {
		TestTable table = PersistCache.getInstance().get(new TestCacheById(1));
		TestTable table2 = PersistCache.getInstance().get(new TestCacheById(1));

		Assert.assertTrue(table == table2);

		TestCacheByValue desc = new TestCacheByValue(1);
		ListValue<TestTable> list = PersistCache.getInstance().get(desc);
		Assert.assertTrue(list.getAll_objects().size() > 0);

		// 基于desc搜索结果上的二次筛选
		TestTable table3 = desc.findById(3);
		Assert.assertNotNull(table3);
		// 基于desc搜索结果上的二次筛选
		TestTable table4 = desc.findById(4);
		Assert.assertNull(table4);
	}

	@Test
	public void addTest() {
		TestTable test = new TestTable(3, (long) 4);
		CacheDesc<TestTable> desc = new TestCacheById(3);
		PersistCache.getInstance().put(desc, test);
		test.setDbState(DBState.U);
		TestTable testTable = PersistCache.getInstance().get(desc);
		Assert.assertTrue(testTable == test);
	}

	@Test
	public void collTest() {
		TestCacheByValue desc = new TestCacheByValue(1);
		ListValue<TestTable> list = PersistCache.getInstance().get(desc);
		TestTable table = new TestTable(4, (long) 6);
		list.saveOrUpdate(table);
		list.delete(table);
		list.saveOrUpdate(table);
	}

	@Test
	public void multiThreadTest() throws InterruptedException, BrokenBarrierException {
		int num = 5;
		CyclicBarrier barrier = new CyclicBarrier(num + 1);
		for (int i = 0; i < num; i++) {
			new Thread(new addThread(barrier)).start();
		}
		barrier.await();
		long start = System.nanoTime();
		barrier.await();
		long end = System.nanoTime();

		TestTable table = PersistCache.getInstance().get(new TestCacheById(1));

		System.out.println("=========" + table.getNValue());
		System.out.println((end - start));
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
				for (int i = 0; i < 100; i++) {
					TestTable table = PersistCache.getInstance().get(new TestCacheById(1));
					table.increaseValue();
				}
				barrier.await();
			} catch (Exception e) {
			}
		}
	}
}