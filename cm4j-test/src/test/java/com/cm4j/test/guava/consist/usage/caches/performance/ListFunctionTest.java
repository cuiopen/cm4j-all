package com.cm4j.test.guava.consist.usage.caches.performance;

import com.cm4j.test.guava.consist.caches.TmpListMultikeyListCache;
import com.cm4j.test.guava.consist.cc.ConcurrentCache;
import com.cm4j.test.guava.consist.cc.ListReference;
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
 * <p/>
 * 对同一缓存加锁测试数据
 * 结果:
 * 完成，数值sum为：75000
 * 计算消耗时间[s]：6.654958661
 * 写入消耗时间[s]：2.11685312
 *
 * @author Yang.hao
 * @since 2013-3-6 上午10:12:38
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test_1/spring-ds.xml"})
public class ListFunctionTest {

    public final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void counterTest() {
        long start = System.nanoTime();
        int num = 0;
        while (num < 75000) {
            num++;
        }
        long end = System.nanoTime();
        System.out.println("计算消耗时间[ms]：" + (double) (end - start) / 1000000);
    }

    @Test
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

        ConcurrentCache.getInstance().stop();

        long writeEnd = System.nanoTime();

        System.out.println("======================");
        System.out.println("完成，数值sum为：" + counter.get());
        System.out.println("计算消耗时间[s]：" + (double) (end - start) / 1000000000);
        System.out.println("写入消耗时间[s]：" + (double) (writeEnd - end) / 1000000000);
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
                for (int i = 0; i < 15000; i++) { // 执行20000次
                    int random = RandomUtils.nextInt(1000);
                    ListReference<TmpListMultikey> ref = new TmpListMultikeyListCache(50705).ref();

                    synchronized (ref) {
                        TmpListMultikey tmp = new TmpListMultikeyListCache(50705).findByType(random);
                        if (tmp == null) {
                            ref.update(new TmpListMultikey(new TmpListMultikeyPK(50705, random), 1));
                            long num = counter.incrementAndGet();
                            // logger.debug("new 新对象,总计 = {}",num);
                        } else {
                            double d = RandomUtils.nextDouble();
                            // d >= 0 代表纯增加和计算，无删除
                            // d >= 0.8 代表纯20%修改，80%删除
                            if (d >= 0) {
                                tmp.setNValue(tmp.getNValue() + 1);
                                tmp.update();
                                long num = counter.incrementAndGet();
                                // logger.debug("对象+1,总计 = {}", num);
                            } else {
                                tmp.delete();
                                counter.addAndGet(-tmp.getNValue());
                                // logger.debug("对象 {} 被删除",testTable.getNId());
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
