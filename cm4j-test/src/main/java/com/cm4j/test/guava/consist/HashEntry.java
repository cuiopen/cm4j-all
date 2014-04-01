package com.cm4j.test.guava.consist;

import com.cm4j.test.guava.consist.queue.AQueueEntry;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
* Created by yanghao on 14-3-31.
*/ /* ---------------- Inner Classes -------------- */
final class HashEntry extends AQueueEntry<AbsReference> {
    // HashEntery内对象的final不变性来降低读操作对加锁的需求
    final String key;
    final int hash;
    final HashEntry next;
    volatile AbsReference value;

    HashEntry(String key, int hash, HashEntry next, AbsReference value) {
        super(value);
        this.key = key;
        this.hash = hash;
        this.next = next;
        this.value = value;
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

    static final AtomicReferenceArray<HashEntry> newArray(int i) {
        return new AtomicReferenceArray<HashEntry>(i);
    }

    volatile long accessTime = Long.MAX_VALUE;

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long time) {
        this.accessTime = time;
    }
}
