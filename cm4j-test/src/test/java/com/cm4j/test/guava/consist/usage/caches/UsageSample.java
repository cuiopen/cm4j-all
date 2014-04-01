package com.cm4j.test.guava.consist.usage.caches;

import com.cm4j.test.guava.consist.cc.ConcurrentCache;
import com.cm4j.test.guava.consist.cc.MapReference;
import com.cm4j.test.guava.consist.cc.SingleReference;
import com.cm4j.test.guava.consist.caches.TmpFhhdCache;
import com.cm4j.test.guava.consist.caches.TmpListMultikeyListCache;
import com.cm4j.test.guava.consist.caches.TmpListMultikeyMapCache;
import com.cm4j.test.guava.consist.entity.TmpFhhd;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.entity.TmpListMultikeyPK;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;

/**
 * 使用实例
 *
 * User: yanghao
 * Date: 13-10-24 上午10:32
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test_1/spring-ds.xml"})
public class UsageSample {

    @Test
    public void getTest() {
        // Single格式缓存获取
        SingleReference<TmpFhhd> singleRef = ConcurrentCache.getInstance().get(new TmpFhhdCache(50769));
        TmpFhhd fhhd = singleRef.get();
        TmpFhhd fhhd2 = new TmpFhhdCache(50769).ref().get();
        Assert.assertTrue(fhhd == fhhd2);

        // List格式缓存获取
        List<TmpListMultikey> list = ConcurrentCache.getInstance().get(new TmpListMultikeyListCache(50705)).get();
        // Map格式缓存获取
        Map<Integer, TmpListMultikey> map = new TmpListMultikeyMapCache(1001).ref().get();
    }

    @Test
    public void updateTest() {
        SingleReference<TmpFhhd> singleRef = new TmpFhhdCache(50769).ref();
        TmpFhhd tmpFhhd = singleRef.get();
        if (tmpFhhd == null) {
            // 新增
            tmpFhhd = new TmpFhhd(50769, 10, 10, "");
        } else {
            // 修改
            tmpFhhd.setNCurToken(10);
        }
        // 新增或修改都可以调用update
        singleRef.update(tmpFhhd);
        Assert.assertTrue(new TmpFhhdCache(50769).ref().get().getNCurToken() == 10);

        // 删除
        singleRef.delete();
        Assert.assertNull(new TmpFhhdCache(50769).ref().get());

        // 立即保存缓存到DB
        singleRef.persist();
    }

    @Test
    public void update2Test() {
        MapReference<Integer, TmpListMultikey> mapRef = new TmpListMultikeyMapCache(1001).ref();
        TmpListMultikey value = mapRef.get(1);
        if (value == null) {
            mapRef.put(1, new TmpListMultikey(new TmpListMultikeyPK(1001, 1), 99));
        }

        TmpListMultikey newValue = new TmpListMultikeyMapCache(1001).ref().get(1);
        newValue.setNValue(2);
        // 对于已经存在于缓存中的对象
        // 我们可以直接调用update()进行修改
        newValue.update();
        Assert.assertTrue(new TmpListMultikeyMapCache(1001).ref().get(1).getNValue() == 2);

        // 也可以直接调用delete()进行删除
        newValue.delete();
        Assert.assertNull(new TmpListMultikeyMapCache(1001).ref().get(1));

        // ? persistAll ?
    }
}
