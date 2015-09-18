package com.cm4j.test.thread.concurrent.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureTaskWithRunnable {

	public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
		// 1.定义Callable
		Callable<Integer> callable = new Callable<Integer>() {
			private Integer i = 10;

			@Override
			public Integer call() throws Exception {
				Thread.sleep(3000);
				return ++this.i;
			}
		};

		// 2.根据Callable获取FutureTask
		FutureTask<Integer> futureTask = new FutureTask<Integer>(callable);
		// 3.根据FutureTask获取Thread并执行
		new Thread(futureTask).start();
		// 4.从futureTask获取值
		System.out.println(futureTask.get(1000, TimeUnit.MILLISECONDS));
	}
}
