package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;

/**
 * entity copy出来的对象
 *
 * Created by yanghao on 2015/9/19.
 */
public class CacheMirror {
    // 源CacheEntry
    private final CacheEntry cacheEntry;
    // 缓存的key，用于进行缓存操作
    private final String key;
    // DB中对应的唯一ID
    private final String dbKey;
    // 生成的即时镜像entity
    private final IEntity entity;
    private final DBState dbState;
    private final int version;

    public CacheMirror(CacheEntry cacheEntry, IEntity entity) {
        this.cacheEntry = cacheEntry;

        // 这2个不会变，其实可以不用加在这里面
        this.key = cacheEntry.ref().getAttachedKey();
        this.dbKey = cacheEntry.getID();

        // 这几个字段会变的，需要在mirror的时候就确定下来
        this.entity = entity;
        this.dbState = cacheEntry.getDbState();
        this.version = cacheEntry.getVersion();
    }

    public String getKey() {
        return key;
    }

    public String getDbKey() {
        return dbKey;
    }

    public CacheEntry getCacheEntry() {
        return cacheEntry;
    }

    public IEntity getEntity() {
        return entity;
    }

    public DBState getDbState() {
        return dbState;
    }

    public int getVersion() {
        return version;
    }
}
