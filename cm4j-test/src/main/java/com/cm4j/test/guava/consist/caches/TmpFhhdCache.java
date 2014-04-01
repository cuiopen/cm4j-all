package com.cm4j.test.guava.consist.caches;

import org.apache.commons.lang.math.NumberUtils;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.SingleReference;
import com.cm4j.test.guava.consist.entity.TmpFhhd;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

public class TmpFhhdCache extends CacheDefiniens<SingleReference<TmpFhhd>> {

	public TmpFhhdCache() {
	}
	
	public TmpFhhdCache(int playerId) {
		super(playerId);
	}

	@Override
	public SingleReference<TmpFhhd> load(String... params) {
		Preconditions.checkArgument(params.length == 1);
		HibernateDao<TmpFhhd, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(TmpFhhd.class);
		return new SingleReference<TmpFhhd>(hibernate.findById(NumberUtils.toInt(params[0])));
	}
}
