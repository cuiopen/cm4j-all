package com.cm4j.test.guava.consist.queue;

import com.google.common.base.Preconditions;

/**
 * Created by yanghao on 14-3-27.
 */
public abstract class AQueueEntry<T> implements IQueueEntry {

    private final T value;

    private IQueueEntry nextAccess = NullEntry.INSTANCE, previousAccess = NullEntry.INSTANCE;

    public AQueueEntry(T value) {
        Preconditions.checkNotNull(value);
        this.value = value;
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

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
