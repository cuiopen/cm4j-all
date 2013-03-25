package com.cm4j.test.guava.consist.usage.caches.single;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

public class TableIdCache extends CacheDescriptor<SingleReference<TestTable>> {

	public TableIdCache() {
	}

	public TableIdCache(int id) {
		super(id);
	}

	@Override
	public SingleReference<TestTable> load(String... params) {
		Preconditions.checkArgument(params.length == 1);
		HibernateDao<TestTable, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(TestTable.class);
		return new SingleReference<TestTable>(hibernate.findById(Integer.valueOf(params[0])));
	}
}
