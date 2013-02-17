package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.google.common.base.Preconditions;

public class TableIdCache extends CacheDescriptor<TestTable> {

	public TableIdCache(Object... params) {
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
