package com.cm4j.test.guava.consist.fifo;

import com.google.common.base.Preconditions;

/**
 * FIFO队列内Entry的封装类
 *
 * Created by yanghao on 14-3-27.
 */
public class FIFOEntry<T> implements IQueueEntry {

    /**
     * Q:这里value为啥要是volatile?
     * A:是为了读取的一致性
     * Q:那设值通过构造函数，如果保证读到的不是null值？通过setter方法为什么不加同步？
     * A:因为HashEntry的操作都是在Segment锁下，
     * 具体解释参考：JDK {@link java.util.concurrent.ConcurrentHashMap.HashEntry}
     */
    private volatile T queueEntry;

    private IQueueEntry nextAccess = NullEntry.INSTANCE, previousAccess = NullEntry.INSTANCE;

    public FIFOEntry() {
    }

    public FIFOEntry(T queueEntry) {
        Preconditions.checkNotNull(queueEntry);
        this.queueEntry = queueEntry;
    }

    @Override
    public IQueueEntry getNextInAccessQueue() {
        return nextAccess;
    }

    @Override
    public void setNextInAccessQueue(IQueueEntry next) {
        this.nextAccess = next;
    }

    @Override
    public IQueueEntry getPreviousInAccessQueue() {
        return previousAccess;
    }

    @Override
    public void setPreviousInAccessQueue(IQueueEntry previous) {
        this.previousAccess = previous;
    }

    public T getQueueEntry() {
        return queueEntry;
    }

    public void setQueueEntry(T queueEntry) {
        this.queueEntry = queueEntry;
    }

    @Override
    public String toString() {
        return queueEntry.toString();
    }
}
