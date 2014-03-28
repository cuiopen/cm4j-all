package com.cm4j.test.guava.consist.queue;

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
}
