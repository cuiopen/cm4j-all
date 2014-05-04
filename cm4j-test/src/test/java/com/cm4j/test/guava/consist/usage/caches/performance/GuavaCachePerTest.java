package com.cm4j.test.guava.consist.usage.caches.performance;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;

/**
 * Created by yanghao on 14-5-4.
 */
public class GuavaCachePerTest {
    private static final LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder().build(new CacheLoader<Integer, Integer>() {
        @Override
        public Integer load(Integer key) throws Exception {
            return key;
        }
    });

    public static void main(String[] args) throws ExecutionException {

        long start = System.nanoTime();
        for (int i = 0; i < 200000; i++) {
            Integer v = cache.get(i);
            v ++;
        }
        long end = System.nanoTime();

        System.out.println("计算消耗时间：" + (double) (end - start) / 1000000000);
    }
}
