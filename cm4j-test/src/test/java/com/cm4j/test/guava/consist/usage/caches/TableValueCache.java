package com.cm4j.test.guava.consist.usage.caches;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.ListReference;
import com.cm4j.test.guava.consist.ServiceManager;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.google.common.base.Preconditions;

public class TableValueCache extends CacheDescriptor<ListReference<TestTable>> {

	public TableValueCache(Object... params) {
		super(params);
	}

	@Override
	public ListReference<TestTable> load(String... params) {
		Preconditions.checkArgument(params.length == 1);
		HibernateDao<TestTable, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(TestTable.class);
		List<TestTable> all = hibernate.findAllByProperty("NValue", NumberUtils.toLong(params[0]));
		return new ListReference<TestTable>(all);
	}

	public TestTable findById(int id) {
		ListReference<TestTable> all = ConcurrentCache.getInstance().get(this);
		for (TestTable _testTable : all.get()) {
			if (_testTable.getNId() == id) {
				return _testTable;
			}
		}
		return null;
	}
}
