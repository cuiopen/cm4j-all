package com.cm4j.test.guava.consist;

import com.cm4j.test.guava.consist.fifo.AQueueEntry;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
* Created by yanghao on 14-3-31.
*/
final class HashEntry extends AQueueEntry<AbsReference> {

    volatile long accessTime = Long.MAX_VALUE;

    // HashEntery内对象的final不变性来降低读操作对加锁的需求
    private final String key;
    private final int hash;
    private final HashEntry next;

    public HashEntry(String key, int hash, HashEntry next, AbsReference value) {
        super(value);
        this.key = key;
        this.hash = hash;
        this.next = next;
    }

    static final AtomicReferenceArray<HashEntry> newArray(int i) {
        return new AtomicReferenceArray<HashEntry>(i);
    }

    public String getKey() {
        return key;
    }

    public int getHash() {
        return hash;
    }

    public HashEntry getNext() {
        return next;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long time) {
        this.accessTime = time;
    }
}
