package com.cm4j.test.guava.consist.caches;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.SingleReference;
import com.cm4j.test.guava.consist.entity.TmpFhhd;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;

public class TmpFhhdCache extends CacheDefiniens<SingleReference<TmpFhhd>> {

    private int playerId;

    public TmpFhhdCache(int playerId) {
        super(playerId);
        this.playerId = playerId;
    }

	@Override
	public SingleReference<TmpFhhd> load() {
		HibernateDao<TmpFhhd, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(TmpFhhd.class);

        TmpFhhd fhhd = hibernate.findById(playerId);

        // TODO 没有则新建
//        if (fhhd == null) {
//            fhhd = new TmpFhhd();
//            fhhd.setNPlayerId(NumberUtils.toInt(params[0]));
//            fhhd.setNCurToken(100);
//
//            SingleReference<TmpFhhd> ref = new SingleReference<TmpFhhd>(null);
//
//            ref.update(fhhd);
//            return ref;
//        }

        return new SingleReference<TmpFhhd>(fhhd);
	}
}
