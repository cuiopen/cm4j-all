package com.cm4j.test.guava.consist.cc;

import java.util.List;

import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import org.apache.commons.lang.math.NumberUtils;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.ListReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

/**
 * 根据Value查询的缓存
 * 
 * @author Yang.hao
 * @since 2013-2-27 下午03:23:03
 *
 */
public class TableValueListCache extends CacheDefiniens<ListReference<TestTable>> {

	public TableValueListCache(Object... params) {
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
