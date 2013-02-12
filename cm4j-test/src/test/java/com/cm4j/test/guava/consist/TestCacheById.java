package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDesc;
import com.google.common.base.Preconditions;

public class TestCacheById extends CacheDesc<TestTable> {

	public TestCacheById(Object... params) {
		super(params);
	}

	@Override
	public TestTable load(String... params) {
		Preconditions.checkArgument(params.length == 1);
		HibernateDao<TestTable, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(TestTable.class);
		return hibernate.findById(Integer.valueOf(params[0]));
	}
}
