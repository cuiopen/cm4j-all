package com.cm4j.test.guava.consist.usage.caches;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.ServiceManager;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.FieldFlagSituation;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.google.common.base.Preconditions;

public class FieldFlagCache extends CacheDescriptor<SingleReference<FieldFlagSituation>> {

	public FieldFlagCache(Object... params) {
		super(params);
	}

	@Override
	public SingleReference<FieldFlagSituation> load(String... params) {
		Preconditions.checkArgument(params.length == 1);
		HibernateDao<FieldFlagSituation, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(FieldFlagSituation.class);
		return new SingleReference<FieldFlagSituation>(hibernate.findById(Integer.valueOf(params[0])));
	}

}
