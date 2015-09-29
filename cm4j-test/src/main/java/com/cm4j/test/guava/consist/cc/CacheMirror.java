package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.google.common.base.Preconditions;

/**
 * entity copy出来的对象
 *
 * Created by yanghao on 2015/9/19.
 */
public class CacheMirror {

    private final String cacheKey;
    private final String dbKey;
    // 当前版本号
    private final int version;

    // 生成的即时镜像entity
    private final IEntity entity;
    // 需要持久化的状态
    private final DBState dbState;

    public CacheMirror(String cacheKey, String dbKey, int version, IEntity entity, DBState dbState) {
        this.cacheKey = cacheKey;
        this.dbKey = dbKey;
        this.version = version;
        this.entity = entity;
        this.dbState = dbState;

        Preconditions.checkNotNull(this.cacheKey);
        Preconditions.checkNotNull(this.dbKey);
        Preconditions.checkNotNull(this.entity);
        Preconditions.checkNotNull(this.dbState);
    }

    public IEntity getEntity() {
        return entity;
    }

    public DBState getDbState() {
        return dbState;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getDbKey() {
        return dbKey;
    }

    public int getVersion() {
        return version;
    }
}
