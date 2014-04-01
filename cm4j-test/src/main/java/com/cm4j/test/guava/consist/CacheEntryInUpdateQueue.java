package com.cm4j.test.guava.consist;

import com.cm4j.test.guava.consist.entity.IEntity;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.springframework.beans.BeanUtils;

/**
 * 读取计数器:引用原对象<br>
 * 读取写入对象：本类中dbState<br>
 * 写入数据：本类中copied
 */
final class CacheEntryInUpdateQueue {
    private final CacheEntry reference;
    private final DBState dbState;
    private final IEntity entity;

    CacheEntryInUpdateQueue(CacheEntry reference) {
        this.reference = reference;
        this.dbState = reference.getDbState();

        IEntity parseEntity = reference.parseEntity();
        StopWatch watch = new Slf4JStopWatch();
        if (reference instanceof IEntity && (reference != parseEntity)) {
            // 内存地址不同，创建了新对象
            this.entity = parseEntity;
            watch.lap("cache.entry.new_object()");
        } else {
            // 其他情况，属性拷贝
            try {
                this.entity = parseEntity.getClass().newInstance();
                BeanUtils.copyProperties(reference, this.entity);
                watch.lap("cache.entry.property_copy()");
            } catch (Exception e) {
                throw new RuntimeException("CacheEntry[" + reference.ref() + "]不能被PropertyCopy", e);
            }
        }
        watch.stop("cache.entry.init_finish()");
    }

    /**
     * 引用原对象
     */
    public CacheEntry getReference() {
        return reference;
    }

    /**
     * 数据，是CacheEntry的数据备份
     */
    public IEntity getEntity() {
        return entity;
    }

    public DBState getDbState() {
        return dbState;
    }
}
