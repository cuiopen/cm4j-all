package com.cm4j.test.guava.consist.caches;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.MapReference;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.math.NumberUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COMMENT HERE
 *
 * User: yanghao
 * Date: 13-10-16 上午9:29
 */
public class TmpListMultikeyMapCache extends CacheDefiniens<MapReference<Integer, TmpListMultikey>> {

    public TmpListMultikeyMapCache() {
    }

    public TmpListMultikeyMapCache(int playerId) {
        super(playerId);
    }

    @Override
    public MapReference<Integer, TmpListMultikey> load(String... params) {
        Preconditions.checkArgument(params.length == 1);
        HibernateDao<TmpListMultikey, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(TmpListMultikey.class);
        String hql = "from TmpListMultikey where id.NPlayerId = ?";
        List<TmpListMultikey> all = hibernate.findAll(hql, NumberUtils.toInt(params[0]));

        Map<Integer, TmpListMultikey> map = new HashMap<Integer, TmpListMultikey>();
        for (TmpListMultikey tmpListMultikey : all) {
            map.put(tmpListMultikey.getId().getNType(), tmpListMultikey);
        }
        return new MapReference<Integer, TmpListMultikey>(map);
    }
}
