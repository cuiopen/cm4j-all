package com.cm4j.test.guava.consist.cc;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.ListReference;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.math.NumberUtils;

/**
 * COMMENT HERE
 *
 * User: yanghao
 * Date: 13-10-11 下午3:16
 */
public class TmpListMultikeyCache extends CacheDescriptor<ListReference<TmpListMultikey>> {

    public TmpListMultikeyCache() {
    }

    public TmpListMultikeyCache(int playerId) {
        super(playerId);
    }

    @Override
    public ListReference<TmpListMultikey> load(String... params) {
        Preconditions.checkArgument(params.length == 1);
        HibernateDao<TmpListMultikey, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(TmpListMultikey.class);
        return new ListReference<TmpListMultikey>(hibernate.findAllByProperty("nPlayerId", NumberUtils.toInt(params[0])));
    }

    public TmpListMultikey findByType(int type) {
        ListReference<TmpListMultikey> all = ConcurrentCache.getInstance().get(this);
        for (TmpListMultikey _TmpListMultikey : all.get()) {
            if (_TmpListMultikey.getId().getNType() == type) {
                return _TmpListMultikey;
            }
        }
        return null;
    }
}
