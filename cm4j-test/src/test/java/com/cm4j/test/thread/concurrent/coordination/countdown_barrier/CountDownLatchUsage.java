package com.cm4j.test.thread.concurrent.coordination.countdown_barrier;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchUsage {

    @Test
    public void usageOne() throws InterruptedException {
        final int count = 10;
        final CountDownLatch completeLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread("thread:" + i) {
                public void run() {
                    System.out.println(this.getName() + " init done...");
                    completeLatch.countDown();
                };
            }.start();
        }

        // 保证所有线程在同一重点开始跑
        completeLatch.await(); // main线程 等 N线程

        System.out.println("all threads done,main thread to go");
    }

    @Test
    public void usageTwo() throws InterruptedException {
        final int count = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < count; i++) {
            new Thread("thread:" + i) {
                public void run() {
                    System.out.println(this.getName() + " run...");
                    try {
                        // 启动很多线程，需要这些线程在等到通知时才启动
                        startLatch.await();
                        System.out.println(this.getName() + " start at startpoint...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                };
            }.start();
        }

        // 先线程暂停1秒，原因：防止上面循环线程还没运行到 await()，就已经执行下面的通知了！
        Thread.sleep(1000);

        // 通知等待线程启动
        startLatch.countDown(); // N线程 等 mian线程
    }

}
