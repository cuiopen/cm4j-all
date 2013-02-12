package com.cm4j.test.guava.consist;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDesc;
import com.cm4j.test.guava.consist.value.ListValue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:test_1/spring-ds.xml" })
public class PersistCacheTest {

	@Test
	public void testGet() {
		TestTable table = PersistCache.getInstance().get(new TestCacheById(1));
		TestTable table2 = PersistCache.getInstance().get(new TestCacheById(99));

		Assert.assertTrue(table == table2);

		ListValue<TestTable> list = PersistCache.getInstance().get(new TestCacheByValue(1));
		Assert.assertTrue(list.getAll_objects().size() > 0);
	}

	@Test
	public void addTest() {
		TestTable test = new TestTable(3, (long) 4);
		CacheDesc<TestTable> desc = new TestCacheById(3);
		PersistCache.getInstance().put(desc, test);
		test.setDbState(DBState.U);
	}
}