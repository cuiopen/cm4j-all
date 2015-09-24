package com.cm4j.test.guava.consist.cc;

/**
 * Created by yanghao on 14-6-16.
 */
public class CCUtils {

    /* ---------------- Small Utilities -------------- */
    public static int rehash(int h) {
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    public static long now() {
        return System.nanoTime();
    }

    interface SegmentLockHandler<R> {
        R doInSegmentUnderLock(Segment segment, HashEntry e, AbsReference ref);
    }
}
