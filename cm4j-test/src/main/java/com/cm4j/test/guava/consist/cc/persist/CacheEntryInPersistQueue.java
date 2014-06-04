package com.cm4j.test.guava.consist.cc.persist;

import com.cm4j.test.guava.consist.cc.CacheEntry;
import com.cm4j.test.guava.consist.entity.IEntity;
import org.springframework.beans.BeanUtils;

/**
 * 读取计数器:引用原对象<br>
 * 读取写入对象：本类中dbState<br>
 * 写入数据：本类中copied
 */
public class CacheEntryInPersistQueue {
    private final CacheEntry reference;
    private final DBState dbState;
    private final IEntity entity;

    public CacheEntryInPersistQueue(CacheEntry reference) {
        this.reference = reference;
        this.dbState = reference.getDbState();

        IEntity parseEntity = reference.parseEntity();
        if (reference instanceof IEntity && (reference != parseEntity)) {
            // 内存地址不同，创建了新对象
            this.entity = parseEntity;
        } else {
            // 其他情况，属性拷贝
            try {
                this.entity = parseEntity.getClass().newInstance();
                BeanUtils.copyProperties(reference, this.entity);
            } catch (Exception e) {
                throw new RuntimeException("CacheEntry[" + reference.ref() + "]不能被PropertyCopy", e);
            }
        }
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
