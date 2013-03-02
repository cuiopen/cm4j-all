package com.cm4j.test.guava.consist.usage.caches;

import com.cm4j.test.guava.consist.MapReference;
import com.cm4j.test.guava.consist.entity.FieldFlagSituation;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;

public class FieldFlagCollCache extends CacheDescriptor<MapReference<Integer, FieldFlagSituation>> {

	public FieldFlagCollCache(Object... params) {
		super(params);
	}

	@Override
	public MapReference<Integer, FieldFlagSituation> load(String... params) {
		// Preconditions.checkArgument(params.length == 1);
		// HibernateDao<FieldFlagSituation, Integer> hibernate =
		// ServiceManager.getInstance().getSpringBean("hibernateDao");
		// hibernate.setPersistentClass(FieldFlagSituation.class);
		// return new
		// FieldFlagCollReference<FieldFlagSituation>(hibernate.findById(Integer.valueOf(params[0])));

		return null;
	}

}
