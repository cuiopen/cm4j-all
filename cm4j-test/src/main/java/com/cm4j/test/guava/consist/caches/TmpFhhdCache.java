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
        return new SingleReference<TmpFhhd>(hibernate.findById(playerId));
	}

//    @Override
//    public void afterLoad(SingleReference<TmpFhhd> ref) {
//        if (ref.get() == null) {
//            TmpFhhd fhhd = new TmpFhhd();
//            fhhd.setNPlayerId(playerId);
//            fhhd.setNCurToken(0);
//
//            ref.update(fhhd);
//        }
//    }
}
