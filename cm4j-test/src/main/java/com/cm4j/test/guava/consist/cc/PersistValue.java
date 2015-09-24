package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;

/**
 * Created by yanghao on 2015/9/23.
 */
public class PersistValue {

    private CacheEntry entry;
    private DBState dbState;
    private int version;

    public PersistValue(CacheEntry entry, DBState dbState, int version) {
        this.entry = entry;
        this.dbState = dbState;
        this.version = version;
    }

    public CacheEntry getEntry() {
        return entry;
    }

    public void setEntry(CacheEntry entry) {
        this.entry = entry;
    }

    public DBState getDbState() {
        return dbState;
    }

    public void setDbState(DBState dbState) {
        this.dbState = dbState;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
