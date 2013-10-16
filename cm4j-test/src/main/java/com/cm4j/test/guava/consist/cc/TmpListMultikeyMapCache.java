package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.MapReference;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;

/**
 * COMMENT HERE
 *
 * User: yanghao
 * Date: 13-10-16 上午9:29
 */
public class TmpListMultikeyMapCache extends CacheDescriptor<MapReference<Integer, TmpListMultikey>> {

    public TmpListMultikeyMapCache() {
    }

    public TmpListMultikeyMapCache(int playerId) {
        super(playerId);
    }

    @Override
    public MapReference<Integer, TmpListMultikey> load(String... params) {

        return null;
    }
}
