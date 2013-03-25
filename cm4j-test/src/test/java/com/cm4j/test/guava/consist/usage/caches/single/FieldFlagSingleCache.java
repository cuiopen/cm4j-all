package com.cm4j.test.guava.consist.usage.caches.single;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.FieldFlagSituation;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

public class FieldFlagSingleCache extends CacheDescriptor<SingleReference<FieldFlagSituation>> {

	public FieldFlagSingleCache(Object... params) {
		super(params);
	}

	@Override
	public SingleReference<FieldFlagSituation> load(String... params) {
		Preconditions.checkArgument(params.length == 1);
		HibernateDao<FieldFlagSituation, Integer> hibernate = ServiceManager.getInstance()
				.getSpringBean("hibernateDao");
		hibernate.setPersistentClass(FieldFlagSituation.class);
		return new SingleReference<FieldFlagSituation>(hibernate.findById(Integer.valueOf(params[0])));
	}

}
