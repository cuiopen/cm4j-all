package com.cm4j.test.guava.consist.usage.caches.list;

import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.ListReference;
import com.cm4j.test.guava.consist.cc.TmpListMultikeyCache;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.entity.TmpListMultikeyPK;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 1.缓存过期了不应该能修改状态? 使用引用队列？<br>
 * 2.单个对象的状态修改？ 不要放到应用中修改，放到SingleValue和ListValue中处理
 *
 * @author Yang.hao
 * @since 2013-2-15 上午09:45:41
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test_1/spring-ds.xml"})
public class ListTest {

    @Test
    public void getTest() {
        TmpListMultikeyCache desc = new TmpListMultikeyCache(50705);
        desc.ref().update(new TmpListMultikey(new TmpListMultikeyPK(50705, 1), 1));

        // 基于desc搜索结果上的二次筛选
        TmpListMultikey table3 = desc.findByType(1);
        Assert.assertNotNull(table3);
        // 基于desc搜索结果上的二次筛选
        TmpListMultikey table4 = desc.findByType(2);
        Assert.assertNull(table4);

        // 查找所有
        ListReference<TmpListMultikey> reference = ConcurrentCache.getInstance().get(desc);
        Assert.assertTrue(reference.get().size() > 0);
    }

    @Test
    public void changeTest() {
        ListReference<TmpListMultikey> reference = ConcurrentCache.getInstance().get(new TmpListMultikeyCache(6));
        // 注意：这里的Value不一定就是和Key是一致的
        // 不一致代表把新对象加入到此缓存中，这样缓存过期之前是没问题的，但过期后再查询db是有问题的，所以不要这样写

        // 也就是：不要去更改缓存的键里面的值，加入到缓存的值的键要一致
        TmpListMultikey table = new TmpListMultikey(new TmpListMultikeyPK(50705, 2), 1);
        reference.update(table);

        Assert.assertEquals(1, new TmpListMultikeyCache(50705).findByType(2).getNValue());

        reference.delete(table);
        Assert.assertNull(new TmpListMultikeyCache(50705).findByType(2));
    }
}