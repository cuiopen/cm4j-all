package com.cm4j.test.guava.consist.usage.caches.performance;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.math.RandomUtils;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by yanghao on 14-5-4.
 */
public class GuavaCachePerTest {
    private static final LoadingCache<Integer, T> cache = CacheBuilder.newBuilder().build(new CacheLoader<Integer, T>() {
        @Override
        public T load(Integer key) throws Exception {
            return new T(key);
        }
    });


    public void funcTest() throws InterruptedException, BrokenBarrierException {
        int num = 5;
        CyclicBarrier barrier = new CyclicBarrier(num + 1);
        AtomicLong counter = new AtomicLong();
        for (int i = 0; i < num; i++) {
            new Thread(new randomThread(counter, barrier)).start();
        }
        barrier.await();
        long start = System.nanoTime();
        barrier.await();
        long end = System.nanoTime();

        System.out.println("======================");
        System.out.println("完成，数值sum为：" + counter.get());
        System.out.println("计算消耗时间：" + (double) (end - start) / 1000000000);
        System.out.println("每秒完成：" + counter.get() * 1000000000 / (double) (end - start));
    }

    public class randomThread implements Runnable {
        private AtomicLong counter;
        private CyclicBarrier barrier;

        public randomThread(AtomicLong counter, CyclicBarrier barrier) {
            this.counter = counter;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                for (int i = 0; i < 200000; i++) { // 执行20000次
                    try {
                        int random = RandomUtils.nextInt(1000);
                        T value = cache.get(random);
                        double d = RandomUtils.nextDouble();
                        if (d >= 0.5) {
                            value.setVal(value.getVal() + 1);
                            cache.put(i, value);

                            counter.incrementAndGet();
                        } else {
                            value.setVal(0);
                            cache.put(i, value);

                            counter.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception{
        GuavaCachePerTest test = new GuavaCachePerTest();
        test.funcTest();
    }

    public static class T{
        private int val;

        public T(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

        public void setVal(int val) {
            this.val = val;
        }
    }
}
