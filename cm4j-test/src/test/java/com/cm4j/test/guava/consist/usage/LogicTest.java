package com.cm4j.test.guava.consist.usage;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.usage.caches.list.TableValueListCache;
import com.cm4j.test.guava.consist.usage.caches.single.TableIdCache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 逻辑测试
 * 
 * @author Yang.hao
 * @since 2013-3-2 上午10:28:23
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:test_1/spring-ds.xml" })
public class LogicTest {

	/**
	 * 测试
	 */
	@Test
	public void normal() {
		SingleReference<TestTable> reference = new TableIdCache(5).reference();
		TestTable testTable = reference.get();
		testTable.setNValue(1L);
		reference.update(testTable);
		testTable.setNValue(2L);
		reference.update(testTable);
	}

	@Test
	public void ccTest() {
		SingleReference<TestTable> reference = new TableIdCache(5).reference();
		reference.update(new TestTable(101, 7L));
		reference.update(new TestTable(6, 8L));
		reference.persistAndRemove();
	}

	@Test
	public void removeTest() {
		ConcurrentCache.getInstance().get(new TableIdCache(3)).get();

		ConcurrentCache.getInstance().remove(new TableIdCache(3));
		Assert.assertNull(ConcurrentCache.getInstance().get(new TableIdCache(3)).get());
	}

	@Test
	public void concurrentTest() throws InterruptedException, BrokenBarrierException {
		TableValueListCache desc = new TableValueListCache(1);
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

	@Test
	public void removeLogicTest() {
		// new TableIdCache(777).reference();
		// new TableIdCache(888).reference();
		// new TableIdCache(999).reference();

		ConcurrentCache.getInstance().put("$1_355", new SingleReference<TestTable>(new TestTable(355, (long) 0)));
		ConcurrentCache.getInstance().put("$1_964", new SingleReference<TestTable>(new TestTable(964, (long) 0)));
		ConcurrentCache.getInstance().put("$1_323", new SingleReference<TestTable>(new TestTable(323, (long) 0)));
		ConcurrentCache.getInstance().put("$1_818", new SingleReference<TestTable>(new TestTable(818, (long) 0)));

		ConcurrentCache.getInstance().remove("964");
		ConcurrentCache.getInstance().remove("323");
	}

	@Test
	public void guavaRemoveTest() throws ExecutionException {
		LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.SECONDS)
				.build(new CacheLoader<Integer, Integer>() {
					@Override
					public Integer load(Integer key) throws Exception {
						return key;
					}
				});

		cache.get(375);
		cache.get(687);

		cache.invalidate(687);
		cache.invalidate(2);
	}
}
