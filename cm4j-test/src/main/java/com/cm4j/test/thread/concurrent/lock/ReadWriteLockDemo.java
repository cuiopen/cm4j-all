package com.cm4j.test.thread.concurrent.lock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <pre>
 * 对象的方法中一旦加入synchronized修饰，则任何时刻只能有一个线程访问synchronized修饰的方法。假设有个数据对象拥有写方法与读方法，
 * 多线程环境中要想保证数据的安全，需对该对象的读写方法都要加入
 * synchronized同步块。这样任何线程在写入时，其它线程无法读取与改变数据；如果有线程在读取时
 * ，其他线程也无法读取或写入。这种方式在写入操作远大于读操作时
 * ，问题不大，而当读取远远大于写入时，会造成性能瓶颈，因为此种情况下读取操作是可以同时进行的，而加锁操作限制了数据的并发读取。
 * 
 * ReadWriteLock解决了这个问题，当写操作时，其他线程无法读取或写入数据，而当读操作时，其它线程无法写入数据，但却可以读取数据 。
 * </pre>
 * 
 * @author yang.hao
 * @since 2011-8-24 上午11:23:56
 */
public class ReadWriteLockDemo {
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void main(String[] args) {
		Data data = new Data();
		// 写
        Worker t1 = new Worker(data, false);
        // 读
        Worker t2 = new Worker(data, true);
		Worker t3 = new Worker(data, true);
		t1.start();
		t2.start();
		t3.start();
	}

	static class Worker extends Thread {
		Data data;
		boolean read;

		public Worker(Data data, boolean read) {
			this.data = data;
			this.read = read;
		}

		public void run() {
			if (read)
				data.get();
			else
				data.set();
		}
	}

	static class Data {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		Lock read = lock.readLock();
		Lock write = lock.writeLock();

		public void set() {
			write.lock();
			System.out.println(Thread.currentThread().hashCode() + " set:begin " + sdf.format(new Date()));
			try {
				Thread.sleep(1000);
			} catch (Exception e) {

			} finally {
				System.out.println(Thread.currentThread().hashCode() + " set:end " + sdf.format(new Date()));
				write.unlock();
			}
		}

		public int get() {
			read.lock();
			System.out.println(Thread.currentThread().hashCode() + " get :begin " + sdf.format(new Date()));
			try {
				Thread.sleep(1000);
			} catch (Exception e) {

			} finally {
				System.out.println(Thread.currentThread().hashCode() + " get :end " + sdf.format(new Date()));
				read.unlock();
			}

			return 1;
		}
	}
}
