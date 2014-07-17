package com.cm4j.test.guava.consist.caches;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.ConcurrentCache;
import com.cm4j.test.guava.consist.cc.ListReference;
import com.cm4j.test.guava.consist.entity.TestTable;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;

import java.util.List;

/**
 * 根据Value查询的缓存
 * 
 * @author Yang.hao
 * @since 2013-2-27 下午03:23:03
 *
 */
public class TableValueListCache extends CacheDefiniens<ListReference<TestTable>> {

    private long value;

	public TableValueListCache(long value) {
		super(value);
        this.value = value;
    }

	@Override
	public ListReference<TestTable> load() {
		HibernateDao<TestTable, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		hibernate.setPersistentClass(TestTable.class);
		List<TestTable> all = hibernate.findAllByProperty("NValue", value);
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
