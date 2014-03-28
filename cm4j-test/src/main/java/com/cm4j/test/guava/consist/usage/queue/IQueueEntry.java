package com.cm4j.test.guava.consist.usage.queue;

/**
 * 队列元素
 *
 * Created by yanghao on 14-3-26.
 */
public interface IQueueEntry {

    /**
     * Returns the next entry in the access queue.
     */
    IQueueEntry getNextInAccessQueue();

    /**
     * Sets the next entry in the access queue.
     */
    void setNextInAccessQueue(IQueueEntry next);

    /**
     * Returns the previous entry in the access queue.
     */
    IQueueEntry getPreviousInAccessQueue();

    /**
     * Sets the previous entry in the access queue.
     */
    void setPreviousInAccessQueue(IQueueEntry previous);
}
