package com.cm4j.test.thread.concurrent.coordination.countdown_barrier;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class Self_Semaphore {

	private int permits;

	private class Sync extends AbstractQueuedSynchronizer {

		private int permits;

		public Sync(int permits) {
			this.permits = permits;
			super.setState(permits);
		}

		@Override
		protected int tryAcquireShared(int arg) {
			for (;;) {
                int state = getState();
                int next = state - 1;
                if (next < 0 || compareAndSetState(state, next)) {
                    return next;
				}
			}
		}

		@Override
		protected boolean tryReleaseShared(int arg) {
			int state = getState();
			int next = state + 1;
			if (next <= permits) {
                return compareAndSetState(state, next);
            }
            return false;
		}
	}

	private Sync sync;

	public Self_Semaphore(int permits) {
		this.permits = permits;
		this.sync = new Sync(this.permits);
	}

	public void acquire() {
		sync.acquireShared(1);
	}

	public boolean release() {
		return sync.releaseShared(1);
	}

	public class SendingThread extends Thread {
		Self_Semaphore semaphorer;
		int idx;

		public SendingThread(Self_Semaphore semaphorer) {
			super();
			this.semaphorer = semaphorer;
		}

		@Override
		public void run() {
			while (true) {
				// do something then signal
				semaphorer.acquire();
				System.out.println("acquire:" + idx++);
			}
		}
	}

	public class ReceivingThread extends Thread {
		Self_Semaphore semaphorer;
		int idx;

		public ReceivingThread(Self_Semaphore semaphorer) {
			this.semaphorer = semaphorer;
		}

		@Override
		public void run() {
			while (true) {
				if (this.semaphorer.release()) {
					System.out.println("release:" + idx++);
				}
			}
		}
	}

	public static void main(String[] args) {
		Self_Semaphore use = new Self_Semaphore(3);
		SendingThread sendingThread = use.new SendingThread(use);
		SendingThread sendingThread1 = use.new SendingThread(use);
		ReceivingThread receivingThread = use.new ReceivingThread(use);

		sendingThread.start();
		sendingThread1.start();
		receivingThread.start();
	}

}
