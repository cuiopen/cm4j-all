package com.cm4j.test.guava.consist;

import java.util.concurrent.TimeUnit;

public class Constants {

    // 初始化缓存数据个数 默认为16
    public static final int DEFAULT_INITIAL_CAPACITY = 16;
    // 加载因子
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    // TODO 默认设为16
    // 默认segment的个数
    public static final int DEFAULT_CONCURRENCY_LEVEL = 1;
    public static final int MAXIMUM_CAPACITY = 1 << 30;
    public static final int MAX_SEGMENTS = 1 << 16; // slightly conservative
    public static final int RETRIES_BEFORE_LOCK = 2;

    // TODO 默认过期纳秒，完成时需更改为较长时间过期，50ms 用于并发测试
    public final long expireAfterAccessNanos = TimeUnit.SECONDS.toNanos(500);
    /**
     * TODO 更新队列检测间隔，单位s
     */
    public static final int CHECK_UPDATE_QUEUE_INTERVAL = 3;
    /**
     * 间隔多少次检查，可持久化，总间隔时间也就是 5 * 60s = 5min
     */
    public static final int PERSIST_CHECK_INTERVAL = 5;
    /**
     * 达到多少个对象，可持久化
     */
    public static final int MAX_UNITS_IN_UPDATE_QUEUE = 50000;
    /**
     * 达到多少条则提交给批处理
     */
    public static final int BATCH_TO_COMMIT = 300;
}