package com.cm4j.test.guava.consist;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.value.ListValue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:test_1/spring-ds.xml" })
public class PersistCacheTest {

	@Test
	public void test() throws Exception {
		TestTable table = PersistCache.getInstance().get(new TestCacheById(1));
		TestTable table2 = PersistCache.getInstance().get(new TestCacheById(1));

		assert (table == table2);

		table.setNValue(2L);
		table.setDbState(DBState.D);
		table.setNValue(3L);
		table.setDbState(DBState.U);
		
		ListValue<TestTable> list = PersistCache.getInstance().get(new TestCacheByValue(1));
	}
}
