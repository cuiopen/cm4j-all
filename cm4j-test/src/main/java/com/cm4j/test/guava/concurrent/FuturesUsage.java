package com.cm4j.test.guava.concurrent;

import com.google.common.util.concurrent.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Created by yanghao on 2015/6/8.
 */
public class FuturesUsage {

    public static void main(String[] args) {
        // 封装ExecutorService
        ListeningExecutorService service
                = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));

        // submit获得Future
        ListenableFuture<String> future = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "abc";
            }
        });

        // 添加后台回调
        Futures.addCallback(future, new FutureCallback<String>() {
            @Override
            public void onSuccess(String result) {
                System.out.println(">>>" + result);
            }

            @Override
            public void onFailure(Throwable t) {
                System.out.println("failed:" + t);
            }
        });

        Futures.transform(null, new AsyncFunction<String, Integer>() {
            @Override
            public ListenableFuture<Integer> apply(String input) throws Exception {
                return null;
            }
        });
    }
}
