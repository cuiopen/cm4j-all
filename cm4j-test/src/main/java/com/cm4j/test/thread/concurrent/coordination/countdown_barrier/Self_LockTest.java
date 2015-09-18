package com.cm4j.test.thread.concurrent.coordination.countdown_barrier;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Created by yanghao on 2015/1/27.
 */
public class Self_LockTest {

    private static class Lock extends AbstractQueuedSynchronizer{
        @Override
        protected boolean tryAcquire(int arg) {
            return compareAndSetState(0, 1);
        }

        @Override
        protected boolean tryRelease(int arg) {
            setState(0);
            return true;
        }

        public void lock() {
            acquire(0);
    }

        public void unlock() {
            release(0);
        }
    }

    public static void main(String[] args) {
        final Lock lock = new Lock();
        
        new Thread(){
        	public void run() {
        		lock.lock();
        		System.out.println("thread lock...");
        		lock.unlock();
        	}
        }.start();
        
        lock.lock();
        System.out.println("main lock...");
        lock.unlock();
    }
}
