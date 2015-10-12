package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * Reference的一些公共方法
 *
 * @author Yang.hao
 * @since 2013-3-2 上午10:42:59
 */
public abstract class AbsReference {

    private final Map<String, PersistValue> persistMap = Maps.newConcurrentMap();

    /**
     * 此对象所依附的key
     */
    private String attachedKey;

    /**
     * 获取值
     *
     * @param <V>
     * @return
     */
    public abstract <V> V get();

    /**
     * 获取当前的缓存中有效的数据集合
     *
     * @return
     */
    protected abstract Set<CacheEntry> getAllEntries();

    /**
     * 更新entry，在锁下执行
     *
     * @param e
     */
    protected void updateEntry(final CacheEntry e) {
        Preconditions.checkNotNull(e, "对象e为空，不能修改");
        doUnderLock(new CCUtils.SegmentLockHandler() {
            @Override
            public Object doInSegmentUnderLock(Segment segment, HashEntry entry, AbsReference ref) {
                Preconditions.checkArgument(ref != null && ref == AbsReference.this,
                        "ref为空或与缓存中不一致[可能缓存过期或重新加载]:" + (ref == null ? "ref NULL" : ref.getAttachedKey()));
                _update(e);
                changeDbState(e, DBState.U);
                return null;
            }
        });
    }
    protected abstract void _update(CacheEntry e);

    /**
     * 删除entry<br>
     * 因为子类有范型，类型兼容问题，所以子类多写一个delete()方法来规定对象给外部调用，而此方法也转类型调用update()
     *
     * @param e
     */
    public void deleteEntry(final CacheEntry e){
        Preconditions.checkNotNull(e, "对象为null，无法delete");
        // 注意顺序，先remove再change
        doUnderLock(new CCUtils.SegmentLockHandler() {
            @Override
            public Object doInSegmentUnderLock(Segment segment, HashEntry hashEntry, AbsReference ref) {
                Preconditions.checkArgument(ref != null && ref == AbsReference.this,
                        "ref为空或与缓存中不一致[可能缓存过期或重新加载]:" + (ref == null ? "" : ref.getAttachedKey()));
                _delete(e);
                changeDbState(e, DBState.D);
                return null;
            }
        });
    }

    /**
     * 是否已全部保存
     *
     * @return
     */
    public boolean isAllPersist() {
        return this.persistMap.isEmpty();
    }

    protected abstract void _delete(CacheEntry e);

    /**
     * 缓存中单个对象的修改后更改此对象的状态，此方法在lock下被调用
     *
     * @param entry
     * @param dbState
     * @return
     */
    protected void changeDbState(CacheEntry entry, DBState dbState){
        String key = entry.dbKey();
        PersistValue persistValue = persistMap.get(key);
        if (persistValue == null) {
            persistMap.put(key, new PersistValue(entry, dbState, 1));
        } else {
            persistMap.put(key, new PersistValue(entry, dbState, persistValue.getVersion() + 1));
        }
    }

    protected void doUnderLock(CCUtils.SegmentLockHandler handler) {
        ConcurrentCache.getInstance().doUnderLock(this.attachedKey, handler);
    }

    /**
     * 持久化但不移除
     */
    public void persist() {
        ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), false);
    }

    /**
     * 持久化后移除，这个方法一般是用于先移除缓存，然后手动改DB数值，最后缓存重新加载的就是更改后的数值
     *
     * 注意：它与定时持久化{@link DBPersistQueue}是互斥关系（加锁控制），所以调这个方法可能会等待持久化线程，会稍有性能问题，故不要频繁使用
     *
     * <pre><font color=red>
     * 调用这个方法有可能导致报RuntimeException: 缓存中不存在此对象[$1_157]，无法更改状态
     *
     * 原因【这是不可避免的】：
     * 如果一个线程已经从缓存获取到数据ref，此时调用此方法会把ref从缓存remove，
     * 也就是缓存中已不存在此数据，如果持有线程再update或者delete，则无法更改缓存状态
     *
     * 解决方法：调用者自行控制并发，保证调用线程安全。先锁定再获取缓存ref，则不会出现这个问题
     *
     * </font></pre>
     */
    public void persistAndRemove() {
        ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), true);
    }

    protected String getAttachedKey() {
        return attachedKey;
    }

    protected void setAttachedKey(String attachedKey) {
        this.attachedKey = attachedKey;

        for (CacheEntry e : getAllEntries()) {
            e.resetRef(this);
        }
    }

    public Map<String, PersistValue> getPersistMap() {
        return persistMap;
    }
}
