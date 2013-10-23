package com.cm4j.test.guava.consist.usage.caches.performance;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.MapReference;
import com.cm4j.test.guava.consist.cc.TmpListMultikeyMapCache;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.entity.TmpListMultikeyPK;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程异步短时间过期+写入测试
 *
 * @author Yang.hao
 * @since 2013-3-6 上午10:12:38
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test_1/spring-ds.xml"})
public class MapFunctionTest {

    public final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void funcTest() throws InterruptedException, BrokenBarrierException {
        int num = 5;
        CyclicBarrier barrier = new CyclicBarrier(num + 1);
        AtomicLong addedCounter = new AtomicLong();
        AtomicLong counter = new AtomicLong();
        for (int i = 0; i < num; i++) {
            new Thread(new randomThread(addedCounter, counter, barrier)).start();
        }
        barrier.await();
        long start = System.nanoTime();
        barrier.await();
        long end = System.nanoTime();

        ConcurrentCache.getInstance().stop();

        long writeEnd = System.nanoTime();

        System.out.println("======================");
        System.out.println("完成，数值sum为：" + counter.get());
        System.out.println("新增对象数量：" + addedCounter.get());
        System.out.println("计算消耗时间[s]：" + (double) (end - start) / 1000000000);
        System.out.println("写入消耗时间[s]：" + (double) (writeEnd - end) / 1000000000);
    }

    public class randomThread implements Runnable {
        private AtomicLong addedCounter;
        private AtomicLong counter;
        private CyclicBarrier barrier;

        public randomThread(AtomicLong addedCounter, AtomicLong counter, CyclicBarrier barrier) {
            this.addedCounter = addedCounter;
            this.counter = counter;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                for (int i = 0; i < 50000; i++) { // 执行20000次
                    int random = RandomUtils.nextInt(1000);
                    MapReference<Integer, TmpListMultikey> ref = new TmpListMultikeyMapCache(1001).ref();

                    synchronized (ref) {
                        TmpListMultikey tmp = ref.get(random);
                        if (tmp == null) {
                            TmpListMultikey newValue = new TmpListMultikey(new TmpListMultikeyPK(50705, random), 1);
                            ref.put(random, newValue);
                            long num = counter.incrementAndGet();
                            addedCounter.incrementAndGet();
                        } else {
                            double d = RandomUtils.nextDouble();
                            if (d >= 0.8) {
                                tmp.setNValue(tmp.getNValue() + 1);
                                tmp.update();
                                long num = counter.incrementAndGet();
                            } else {
                                //tmp.delete();
                                //counter.addAndGet(-tmp.getNValue());
                            }
                        }
                    }

                    // 为增加并发异常，暂停10ms
                    // Thread.sleep(10);
                }
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
