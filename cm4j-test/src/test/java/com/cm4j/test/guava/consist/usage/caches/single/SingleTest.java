package com.cm4j.test.guava.consist.usage.caches.single;

import com.cm4j.test.guava.consist.cc.TmpFhhdCache;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.ConcurrentCache;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.TmpFhhd;
import com.cm4j.test.guava.service.ServiceManager;

/**
 * 
 * @author Yang.hao
 * @since 2013-2-15 上午09:45:41
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:test_1/spring-ds.xml" })
public class SingleTest {

	@Test
	public void getTest() throws Exception {
		TmpFhhd fhhd = ConcurrentCache.getInstance().get(new TmpFhhdCache(50769)).get();
		TmpFhhd fhhd2 = new TmpFhhdCache(50769).ref().get();

		Assert.assertTrue(fhhd == fhhd2);
	}

	@Test
	public void deleteTest() {
		new TmpFhhdCache(50769).ref().delete();
		new TmpFhhdCache(50769).ref().persist();
	}
	
	@Test
	public void deleteTest2(){
		new TmpFhhdCache(50769).ref().get().delete();
		new TmpFhhdCache(50769).ref().persist();
	}

	@Test
	public void deleteTest3() {
		SingleReference<TmpFhhd> ref = new TmpFhhdCache(50769).ref();
		TmpFhhd tmpFhhd = ref.get();

		// 删除
		ref.delete();

		// 这里应该报错，被删除了就不能被修改
		boolean hasException = false;
		try {
			ref.update(tmpFhhd);
		} catch (Exception e) {
			hasException = true;
		}
		Assert.assertTrue(hasException);
	}

	@Test
	public void addTest() {
		// 新增，如果有记录则不能add new出来的对象，因为对象已经存在，请更新
		new TmpFhhdCache(50769).ref().update(new TmpFhhd(50769, 10, 10, ""));
		new TmpFhhdCache(50769).ref().persist();
	}

	@Test
	public void updateTest() {
		TmpFhhd fhhd = new TmpFhhdCache(50769).ref().get();
		long old = fhhd.getNCurToken();
		fhhd.setNCurToken((int) (old + 1));
		fhhd.update();

		new TmpFhhdCache(50769).ref().persist();

		Assert.assertEquals(old + 1, new TmpFhhdCache(50769).ref().get().getNCurToken().intValue());
	}

	@Test
	public void refreshTest() {
		TmpFhhd fhhd = ConcurrentCache.getInstance().get(new TmpFhhdCache(50769)).get();
		TmpFhhd fhhd2 = new TmpFhhdCache(50769).refresh().get();

		Assert.assertTrue(fhhd != fhhd2);
	}
	
	@Test
	public void persistAndRemove() {
		TmpFhhd fhhd = new TmpFhhdCache(50769).ref().get();
		fhhd.setNCurToken(1);
		fhhd.update();
		new TmpFhhdCache(50769).ref().persist();

		// 存在缓存
		Assert.assertEquals(1, new TmpFhhdCache(50769).refIfPresent().get().getNCurToken().intValue());

		fhhd.setNCurToken(2);
		fhhd.update();
		new TmpFhhdCache(50769).ref().persistAndRemove();

		// 不存在缓存
		Assert.assertNull(new TmpFhhdCache(50769).refIfPresent());
		// 数值已更改
		Assert.assertEquals(2, new TmpFhhdCache(50769).ref().get().getNCurToken().intValue());
	}
	
	@Test
	public void tt(){
		HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
		Session session = hibernate.getSession();
		Transaction tx = session.beginTransaction();
		try {
			session.merge(new TmpFhhd(1, 1, 3, ""));
			session.merge(new TmpFhhd(1, 3, 3, ""));
			
			session.flush(); // 清理缓存，执行批量插入20条记录的SQL insert语句
			session.clear(); // 清空缓存中的Customer对象
			
			// 提交
			tx.commit();
		} catch (HibernateException exception) {
			exception.printStackTrace();
		} finally {
			try {
				session.close();
			} catch (HibernateException e) {
			}
		}
		
//		SingleReference<TmpFhhd> ref = new TmpFhhdCache(1).ref();
//		ref.update(new TmpFhhd(1, 1, 3, ""));
//		
//		SingleReference<TmpFhhd> ref2 = new TmpFhhdCache(1).ref();
//		
//		ref2.update(new TmpFhhd(1, 1, 3, ""));
//		ref2.persist();
	}
}