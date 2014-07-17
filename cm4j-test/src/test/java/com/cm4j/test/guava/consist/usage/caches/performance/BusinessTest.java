package com.cm4j.test.guava.consist.usage.caches.performance;

import com.cm4j.test.guava.consist.caches.TmpFhhdCache;
import com.cm4j.test.guava.consist.cc.SingleReference;
import com.cm4j.test.guava.consist.entity.TmpFhhd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.BrokenBarrierException;

/**
 * 功能测试
 *
 * @author Yang.hao
 * @since 2013-3-6 上午10:12:38
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test_1/spring-ds.xml"})
public class BusinessTest {

    public final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void funcTest() throws InterruptedException, BrokenBarrierException {
        SingleReference<TmpFhhd> ref = new TmpFhhdCache(1).ref();

        TmpFhhd fhhd = ref.get();
        if (fhhd == null) {
            fhhd = new TmpFhhd();
            fhhd.setNPlayerId(1);
            ref.update(fhhd);
        }

        fhhd.setNCurToken(10);
        fhhd.update();

        new TmpFhhdCache(1).ref().persist();
    }
}
