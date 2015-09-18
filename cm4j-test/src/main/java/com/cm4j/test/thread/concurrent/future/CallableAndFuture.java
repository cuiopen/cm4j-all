package com.cm4j.test.thread.concurrent.future;

import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by yanghao on 2015/1/24.
 */
public class CallableAndFuture {
    // 与Runnable类似，不同在于Callable有返回值
    private final static Callable<Integer> callable = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            Thread.sleep(5000);// 可能做一些事情
            return new Random().nextInt(100);
        }
    };

    /**
     * 1.单线程运行获得FutureTask
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private static void t1() throws Exception {
        System.out.println("------------------------");
        FutureTask<Integer> future = new FutureTask(callable);
        // 运行线程
        new Thread(future).start();
        // 获取结果
        System.out.println(future.get());

    }

    /**
     * 2.线程池运行获得Future
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private static void t2() throws Exception {
        System.out.println("------------------------");
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        // 运行线程池
        Future<Integer> future = threadPool.submit(callable);
        // 获取结果
        System.out.println(future.get());
        // 结果获取到才关闭线程池
        threadPool.shutdown();
    }

    /**
     * 3.执行多个带返回值的任务，并取得多个返回值
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void t3() throws Exception {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        CompletionService<Integer> cs = new ExecutorCompletionService<Integer>(threadPool);

        System.out.println("-----------------------");
        for (int i = 0; i < 5; i++) {
            final int taskID = i;
            cs.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    Thread.sleep(5000);// 可能做一些事情
                    return taskID;
                }
            });
        }
        // 可能做一些事情
        for (int i = 0; i < 5; i++) {
            System.out.println(cs.take().get());
        }

        // 其实也可以不使用CompletionService，可以先创建一个装Future类型的集合，用Executor提交的任务返回值添加到集合中，
        // 最后遍历集合取出数据，代码略。
    }

    public static void main(String[] args) throws Exception {
        t3();
    }
}