package com.cm4j.test.guava.consist.usage.caches.list;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.ListReference;
import com.cm4j.test.guava.consist.entity.TestTable;

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
public class ListTest {

	@Test
	public void getTest() {
		TableValueListCache desc = new TableValueListCache(1);

		// 基于desc搜索结果上的二次筛选
		TestTable table3 = desc.findById(3);
		Assert.assertNotNull(table3);
		// 基于desc搜索结果上的二次筛选
		TestTable table4 = desc.findById(4);
		Assert.assertNull(table4);

		// 查找所有
		ListReference<TestTable> reference = ConcurrentCache.getInstance().get(desc);
		Assert.assertTrue(reference.get().size() > 0);
	}

	@Test
	public void changeTest() {
		ListReference<TestTable> reference = ConcurrentCache.getInstance().get(new TableValueListCache(6));
		// 注意：这里的Value不一定就是和Key是一致的
		// 不一致代表把新对象加入到此缓存中，这样缓存过期之前是没问题的，但过期后再查询db是有问题的，所以不要这样写

		// 也就是：不要去更改缓存的键里面的值，加入到缓存的值的键要一致
		TestTable table = new TestTable(4, (long) 6);
		reference.update(table);

		Assert.assertEquals(6L, new TableValueListCache(6).findById(4).getNValue().longValue());

		reference.delete(table);
		Assert.assertNull(new TableValueListCache(4).findById(6));
	}

	@Test
	public void changeTest2() {
		ListReference<TestTable> reference = new TableValueListCache(6).reference();
		TestTable table = new TableValueListCache(6).findById(4);
		reference.update(table);

	}
}