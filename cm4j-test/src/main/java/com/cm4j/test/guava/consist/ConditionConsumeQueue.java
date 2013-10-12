package com.cm4j.test.guava.consist;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 满足一定条件则消费
 *
 * User: yanghao
 * Date: 13-10-12 下午5:20
 */
public class ConditionConsumeQueue<E> {

    private final ConcurrentLinkedQueue<E> updateQueue;
    private final IConsumer consumer;
    private final Lock lock;
    private final Condition condition;

    public ConditionConsumeQueue(IConsumer consumer) {
        this.consumer = consumer;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.updateQueue = new ConcurrentLinkedQueue<E>();
    }

    public void add(E e) {
        updateQueue.add(e);
        if (updateQueue.size() > 10) {
            condition.signal();
        }
    }

    public void consumeAll() {
        while (true) {
            consumer.consume(updateQueue);
            if (updateQueue.size() == 0) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    interface IConsumer {
        public void consume(ConcurrentLinkedQueue<E> queue);
    }
}
