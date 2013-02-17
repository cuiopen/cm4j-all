package com.cm4j.test.guava.consist;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDescriptor;
import com.cm4j.test.guava.consist.value.ListValue;
import com.google.common.base.Preconditions;

public class TableValueCache extends CacheDescriptor<ListValue<TestTable>> {

	public TableValueCache(Object... params) {
		super(params);
	}

	@Override
	public ListValue<TestTable> load(String... params) {
		Preconditions.checkArgument(params.length == 1);
		HibernateDao<TestTable, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(TestTable.class);
		List<TestTable> all = hibernate.findAllByProperty("NValue", NumberUtils.toLong(params[0]));
		return new ListValue<TestTable>(all);
	}

	public TestTable findById(int id) {
		ListValue<TestTable> all = PersistCache.getInstance().get(this);
		for (TestTable _testTable : all.getAllObjects()) {
			if (_testTable.getNId() == id) {
				return _testTable;
			}
		}
		return null;
	}
}
