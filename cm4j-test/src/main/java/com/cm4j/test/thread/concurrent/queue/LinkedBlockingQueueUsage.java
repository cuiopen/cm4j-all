package com.cm4j.test.thread.concurrent.queue;

import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueUsage {

	static class PutThread implements Runnable {
        private LinkedBlockingQueue<Integer> blockingQueue;

        public PutThread(LinkedBlockingQueue<Integer> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void run() {
            while (true) {
            	blockingQueue.offer(1);
            }
        }
    }
	
	static class GetThread implements Runnable{
        private LinkedBlockingQueue<Integer> blockingQueue;

        public GetThread(LinkedBlockingQueue<Integer> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Object obj = blockingQueue.take();
                    System.out.println(obj);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
	
	public static void main(String[] args) {
		LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer>();
		new Thread(new GetThread(queue)).start();
		new Thread(new PutThread(queue)).start();
	}
}
