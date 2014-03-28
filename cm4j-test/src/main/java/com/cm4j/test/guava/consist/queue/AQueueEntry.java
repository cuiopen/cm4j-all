package com.cm4j.test.guava.consist.queue;

import com.google.common.base.Preconditions;

/**
 * Created by yanghao on 14-3-27.
 */
public abstract class AQueueEntry implements IQueueEntry {

    private final Object value;

    private IQueueEntry nextAccess = NullEntry.INSTANCE, previousAccess = NullEntry.INSTANCE;

    public AQueueEntry(Object value) {
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

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
