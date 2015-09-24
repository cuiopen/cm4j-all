package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推荐使用{@link ListReference}来查询，在进行map进行索引<br>
 * 注意：此方法未进行详细测试
 *
 * @param <K>
 * @param <V>
 * @author Yang.hao
 * @since 2013-3-4 下午06:54:56
 */
public class MapReference<K, V extends CacheEntry> extends AbsReference {

    // todo 有必要用并发包吗？？？
    private Map<K, V> map = new ConcurrentHashMap<K, V>();

    public MapReference(Map<K, V> map) {
        Preconditions.checkNotNull(map);
        this.map.putAll(map);
    }

	/*
     * ===================== public methods =====================
	 */

    /**
     * 获取值
     *
     * @return 不可更改的map，以防止外部破坏内部结构和状态
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<K, V> get() {
        return ImmutableMap.copyOf(map);
    }

    public V get(K key) {
        return map.get(key);
    }

    public void put(K key, V value) {
        Preconditions.checkArgument(!map.containsKey(key), "对象已存在，请调用update(V)更新对象");
        value.resetRef(this);
        map.put(key, value);

        changeDbState(value, DBState.U);
    }

    public void update(V value) {
        Preconditions.checkArgument(map.containsValue(value), "对象不存在，请通过put(K,V)增加对象到缓存中");
        // 存在则修改
        changeDbState(value, DBState.U);
    }

    public void delete(K key) {
        V v = map.get(key);
        deleteEntry(v);
    }

	/*
     * ================== extend methods ====================
	 */

    @Override
    protected void updateEntry(CacheEntry e) {
        @SuppressWarnings("unchecked")
        V v = (V) e;
        this.update(v);
    }

    @Override
    protected void _update(CacheEntry e) {

    }

    @Override
    protected void _delete(CacheEntry e) {

    }

    @Override
    public Set<CacheEntry> getNotDeletedSet() {
        return new HashSet<CacheEntry>(map.values());
    }
}
