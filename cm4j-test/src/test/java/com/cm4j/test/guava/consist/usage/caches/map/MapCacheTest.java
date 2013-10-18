package com.cm4j.test.guava.consist.usage.caches.map;

import com.cm4j.test.guava.consist.MapReference;
import com.cm4j.test.guava.consist.cc.TmpListMultikeyMapCache;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.entity.TmpListMultikeyPK;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * COMMENT HERE
 *
 * User: yanghao
 * Date: 13-10-17 下午7:23
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test_1/spring-ds.xml"})
public class MapCacheTest {

    @Test
    public void getTest() {
        MapReference<Integer, TmpListMultikey> ref = new TmpListMultikeyMapCache(1001).ref();
        TmpListMultikey value = ref.get(1);
        if (value == null) {
            value = new TmpListMultikey(new TmpListMultikeyPK(1001, 1), 99);
            ref.put(1, value);
            ref.put(1, value);
        } else {
            value.setNValue(88);
            value.update();
        }
        Assert.assertNotNull(new TmpListMultikeyMapCache(1001).ref().get(1));
    }

    @Test
    public void deleteTest() {
        new TmpListMultikeyMapCache(1001).ref().delete(1);
        new TmpListMultikeyMapCache(1001).ref().persist();
    }

}
