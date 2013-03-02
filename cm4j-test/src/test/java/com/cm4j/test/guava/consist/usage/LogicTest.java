package com.cm4j.test.guava.consist.usage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.usage.caches.TableIdCache;

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
	public void ccTest(){
		SingleReference<TestTable> reference = new TableIdCache(5).reference();
		reference.update(new TestTable(6, 7L));
		reference.update(new TestTable(6, 8L));
		reference.persistAndRemove();
	}
}
