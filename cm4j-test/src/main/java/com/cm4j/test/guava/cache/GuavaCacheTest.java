package com.cm4j.test.guava.cache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;

public class GuavaCacheTest {

	private static final LoadingCache<String, String> cache = CacheBuilder.newBuilder()
			.expireAfterWrite(500, TimeUnit.SECONDS).removalListener(new RemovalListener<String, String>() {

				@Override
				public void onRemoval(RemovalNotification<String, String> event) {
					System.out.println("removed,key=" + event.getKey() + ",value=" + event.getValue());
				}
			}).recordStats().maximumWeight(2000).weigher(new Weigher<String, String>() {
				@Override
				public int weigh(String key, String value) {
					return value == null ? 0 : value.length();
				}
			}).build(new CacheLoader<String, String>() {
				@Override
				public String load(String key) throws Exception {
					// if ("abc".equals(key)){
					// return "def";
					// }
					return "xyz";
				}
			});

	public static void main(String[] args) throws ExecutionException {
		System.out.println(Integer.toBinaryString(63));
		System.out.println(cache.getIfPresent("ccd"));

//		new Thread(new T()).start();
		System.out.println(cache.get("ccd"));
		System.out.println(cache.get("ccd"));

		cache.put("ccd", "def");
		cache.put("ccd", "xyz");
		
		System.out.println(cache.get("ccd"));
		System.out.println(cache.stats());
		
		System.out.println(63 & 65);

	}

	static class T implements Runnable {
		@Override
		public void run() {
			try {
				System.out.println(cache.get("abc"));
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

	}
	
	

}
