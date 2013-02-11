package com.cm4j.core.cache;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class ExpiredLocalCacheTest {

	@Test
	public void test() throws InterruptedException {
		ExpiredLocalCache<Integer, Integer> cache = new ExpiredLocalCache<Integer, Integer>(1, TimeUnit.SECONDS);
		cache.put(1, 1);
		Assert.assertEquals(new Integer(1), cache.get(1));
		Thread.sleep(1010);
		System.out.println(cache.get(1));
		Assert.assertNull(cache.get(1));
	}
}
