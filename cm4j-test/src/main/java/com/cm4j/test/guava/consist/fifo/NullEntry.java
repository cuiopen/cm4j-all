package com.cm4j.test.guava.consist.fifo;

/**
 * Created by yanghao on 14-3-26.
 */
public enum NullEntry implements IQueueEntry {
    INSTANCE;

    @Override
    public IQueueEntry getNextInAccessQueue() {
        return this;
    }

    @Override
    public void setNextInAccessQueue(IQueueEntry next) {
    }

    @Override
    public IQueueEntry getPreviousInAccessQueue() {
        return this;
    }

    @Override
    public void setPreviousInAccessQueue(IQueueEntry previous) {
    }

    @Override
    public String toString() {
        return "FIFO_ENTRY_NULL";
    }
}
