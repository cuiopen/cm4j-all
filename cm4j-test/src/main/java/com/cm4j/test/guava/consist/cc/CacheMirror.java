package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;

/**
 * entity copy出来的对象
 *
 * Created by yanghao on 2015/9/19.
 */
public class CacheMirror {

    // 生成的即时镜像entity
    private final IEntity entity;
    private final DBState dbState;

    public CacheMirror(IEntity entity, DBState dbState) {
        this.entity = entity;
        this.dbState = dbState;
    }

    public IEntity getEntity() {
        return entity;
    }

    public DBState getDbState() {
        return dbState;
    }

}
