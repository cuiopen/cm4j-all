package com.cm4j.dao.hibernate;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

import com.cm4j.dao.exception.Cm4jDataAccessException;

/**
 * <p>
 * 基于Spring的HibernateTemplate模板的具体数据库操作实现
 * <p>
 * -----------------------------------------------------------
 * <p>
 * <b><font color="red">DAO实现类已通过Autowired方式注入{@link SessionFactory}</font></b>
 * <p>
 * <b><font color="red">DAO实现类可通过this.getHibernateTemplate()获得
 * {@link HibernateTemplate}对象</font></b>
 * <p>
 * -----------------------------------------------------------
 * <p>
 * <b>注入方式：<font
 * color="red">若没有具体dao类，则只能使用第二种注入方式（通过constructor获取pojo），因为无法通过范型获取pojo
 * </font></b>
 * <p>
 * <font color="green"> <b> A、分别继承接口{@link BaseHibernateDAO}和本类
 * {@link HibernateDao}，使用接口指定范型POJO（通过范型获取pojo） </b> </font>
 * <p>
 * <b>接口GsUserDao如下：</b>
 * 
 * <pre>
 * public interface GsUserDao extends BaseHibernateDAO&lt;GsUser, Long&gt; {
 * }
 * </pre>
 * 
 * <b>实现GsUserDaoImpl如下：</b>
 * 
 * <pre>
 * public class GsUserDaoImpl extends HibernateDao&lt;GsUser, Long&gt; implements GsUserDao {
 * }
 * </pre>
 * 
 * <b>Spring配置如下：</b>
 * 
 * <pre>
 * //SessionFacotory通过Autowired注入进来
 * &lt;bean id=&quot;gsUserDao&quot; class=&quot;com.woniu.sncp.popularize.dao.GsUserDaoImpl&quot; /&gt;
 * </pre>
 * <p>
 * <font color="green"> <b> B、使用Spring直接配置，将持久化对象（POJO）通过有参构造函数注入
 * {@link HibernateDao}，避免多余DAO代码（通过构造函数获取pojo）<br>
 * </b> </font>
 * <p>
 * <b>注意：在调用时候采用注解方式，<font
 * color="red">必须使用@Resource（按ID名称），不可使用@Autowired（按类型）</font>，
 * 因为这是通过ID名称注入的，将所有DAO都注入到 {@link HibernateDao}，而A方法两者都可以使用</b>
 * <p>
 * 
 * <b>Spring配置如下：</b>
 * 
 * <pre>
 * //SessionFacotory通过Autowired注入进来
 * &lt;bean id=&quot;gsUserDao&quot; class=&quot;com.cm4j.common.dao.impl.HibernateDao&quot;;&gt;
 * 	 //注入持久化对象POJO
 * 	 &lt;constructor-arg&gt;
 * 	 	&lt;value&gt;com.woniu.sncp.popularize.pojo.GsUser&lt;/value&gt;
 * 	 &lt;/constructor-arg&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * <b>调用如下：</b>
 * 
 * <pre>
 * private BaseHibernateDAO&lt;GsUser, Long&gt; gsUserDao;
 * 
 * \@Resource 
 * public void setGsUserDao(BaseHibernateDAO&lt;GsUser, Long&gt; gsUserDao) {
 * 	this.gsUserDao = gsUserDao;
 * }
 * </pre>
 * <p>
 * -----------------------------------------------------------
 * 
 * @author YangHao
 * @since 2008-12-7 下午09:35:34
 * @contact <a href="mailto:hao.yhao@gmail.com">hao.yhao@gmail.com</a>
 * @copyright cm4j
 */
public class HibernateDao<E, ID extends Serializable> implements InitializingBean {

	private HibernateTemplate hibernateTemplate;

	// 持久化对象：E
	private Class<E> persistentClass;

	public Class<E> getPersistentClass() {
		checkPersistentClass();
		return persistentClass;
	}

	public HibernateDao<E, ID> setPersistentClass(Class<E> persistentClass) {
		this.persistentClass = persistentClass;
		return this;
	}

	// 日志logger对象
	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	// **************************************************************************
	// *
	// * ---------------------- 构造函数，获取持久化对象-------------------------- *
	// **************************************************************************
	// /

	@SuppressWarnings("unchecked")
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			if (this.persistentClass == null) {
				persistentClass = (Class<E>) ((ParameterizedType) getClass().getGenericSuperclass())
						.getActualTypeArguments()[0];
			}
		} catch (Exception e) {
			logger.warn("初始化未能获取范型POJO，class：{}", getClass());
		}

		logger.debug("HibernateDao范型获取POJO：{}", this.persistentClass);
	}

	private void checkPersistentClass() throws Cm4jDataAccessException {
		if (persistentClass == null)
			throw new Cm4jDataAccessException("persistentClass不能为空");
	}

	public HibernateDao() {
		// do nothing
	}

	/**
	 * 构造函数，通过spring配置注入持久化对象
	 */
	public HibernateDao(Class<E> persistentClass) {
		if (persistentClass == null) {
			this.persistentClass = persistentClass;
		}
		logger.debug("HibernateDao构造函数获取POJO：{}", persistentClass);
	}

	// **************************************************************************
	// *
	// * -----------------------------------HQL查询----------------------------- *
	// **************************************************************************
	// /

	public Object findWithHql(String hql, Object[] values) throws DataAccessException {
		List<?> list = this.findAllWithHql(hql, values);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	public E findById(ID id) throws DataAccessException {
		return (E) hibernateTemplate.get(this.getPersistentClass(), id);
	};

	public List<E> findAll() throws DataAccessException {
		String hql = "from " + this.getPersistentClass().getSimpleName();
		return this.findAll(hql);
	}

	public E find(String queryString, Object value) throws DataAccessException {
		List<E> list = findAll(queryString, value);
		E entity = null;
		if (list.size() != 0) {
			entity = (E) list.get(0);
		}
		return entity;
	}

	public E find(String queryString, Object[] values) throws DataAccessException {
		List<E> list = this.findAll(queryString, values);
		E entity = null;
		if (list.size() != 0) {
			entity = list.get(0);
		}
		return entity;
	}

	public E find(String queryString, Map<String, Object> paramValues) throws DataAccessException {
		List<E> list = this.findAll(queryString, paramValues);
		E entity = null;
		if (list.size() != 0) {
			entity = list.get(0);
		}
		return entity;
	}

	public E find(String queryString, String paramName, Object value) throws DataAccessException {
		List<E> list = this.findAll(queryString, paramName, value);
		E entity = null;
		if (list.size() != 0) {
			entity = list.get(0);
		}
		return entity;
	}

	public E find(String queryString, String[] paramNames, Object[] values) throws DataAccessException {
		List<E> list = this.findAll(queryString, paramNames, values);
		E entity = null;
		if (list.size() != 0) {
			entity = list.get(0);
		}
		return entity;
	}

	public E findByProperty(String property, Object value) throws DataAccessException {
		List<E> list = this.findAllByProperty(property, value);
		E entity = null;
		if (list.size() != 0) {
			entity = list.get(0);
		}
		return entity;
	}

	public E findByProperty(String[] properties, Object[] values) throws DataAccessException {
		List<E> list = this.findAllByProperty(properties, values);
		E entity = null;
		if (list.size() != 0) {
			entity = list.get(0);
		}
		return entity;
	}

	public E findByProperty(Map<String, Object> paramValues) throws DataAccessException {
		List<E> list = this.findAllByProperty(paramValues);
		E entity = null;
		if (list.size() != 0) {
			entity = list.get(0);
		}
		return entity;
	}

	public E findByPropertyLike(String property, String value) throws DataAccessException {
		E result = null;
		if (StringUtils.isBlank(property)) {
			throw new Cm4jDataAccessException("查询参数名不允许为空");
		}
		List<E> resultList = this.findAllByPropertyLike(property, value);
		if (resultList != null && resultList.isEmpty() == false) {
			result = resultList.get(0);
		}
		return result;
	}

	public List<?> findAllWithHql(String hql, Object[] values) throws DataAccessException {
		logger.debug("查询语句：{}", hql);
		logger.debug("查询参数：{}", ArrayUtils.toString(values));
		return hibernateTemplate.find(hql, values);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(String hql) throws DataAccessException {
		return hibernateTemplate.find(hql);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(String hql, Object param) throws DataAccessException {
		return hibernateTemplate.find(hql, param);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(String hql, Object[] params) throws DataAccessException {
		return hibernateTemplate.find(hql, params);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(String queryString, Map<String, Object> paramValues) throws DataAccessException {
		if (paramValues == null || paramValues.isEmpty()) {
			throw new Cm4jDataAccessException("查询参数对至少为一对");
		}
		String[] params = paramValues.keySet().toArray(new String[0]);
		Object[] values = new Object[params.length];
		int index = 0;
		for (String param : params) {
			values[index] = paramValues.get(param);
			index++;
		}
		logger.debug("查询语句：{}", queryString);
		logger.debug("参数键值对：{}", paramValues);
		return hibernateTemplate.findByNamedParam(queryString, params, values);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(String queryString, String paramName, Object value) throws DataAccessException {
		return hibernateTemplate.findByNamedParam(queryString, paramName, value);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAll(String queryString, String[] paramNames, Object[] values) throws DataAccessException {
		return hibernateTemplate.findByNamedParam(queryString, paramNames, values);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAllByProperty(String property, Object value) throws DataAccessException {
		if (StringUtils.isBlank(property)) {
			throw new Cm4jDataAccessException("查询参数名不允许为空");
		}
		StringBuilder hql = new StringBuilder("from ").append(this.getPersistentClass().getSimpleName())
				.append(" where ").append(property).append(" = :").append(property);
		logger.debug("查询语句：{}", hql);
		logger.debug("参数名称：{}", property);
		logger.debug("参数值：{}", value);
		return hibernateTemplate.findByNamedParam(hql.toString(), property, value);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAllByProperty(String property, Object value, String orderBy, boolean isAsc)
			throws DataAccessException {
		if (StringUtils.isBlank(property) || StringUtils.isBlank(orderBy)) {
			throw new Cm4jDataAccessException("查询参数名不允许为空");
		}
		StringBuilder hql = new StringBuilder("from ").append(this.getPersistentClass().getSimpleName())
				.append(" where ").append(property).append(" = :").append(property).append(" order by ")
				.append(orderBy).append(isAsc ? " asc" : " desc");
		logger.debug("查询语句：{}", hql);
		logger.debug("参数名：{}", property);
		logger.debug("参数值：{}", value);
		return hibernateTemplate.findByNamedParam(hql.toString(), property, value);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAllByProperty(String[] properties, Object[] values) throws DataAccessException {
		if (properties.length != values.length) {
			throw new Cm4jDataAccessException("查询参数名和参数值的个数必须相同");
		}
		StringBuilder hql = new StringBuilder("from ").append(this.getPersistentClass().getSimpleName()).append(
				" where ");
		for (String param : properties) {
			if (StringUtils.isBlank(param)) {
				throw new Cm4jDataAccessException("查询参数名不允许为空");
			}
			hql = hql.append(param).append(" = :").append(param).append(" and ");
		}
		String queryString = StringUtils.substringBeforeLast(hql.toString(), " and");
		logger.debug("查询语句：{}", queryString);
		for (int i = 0; i < properties.length; i++) {
			logger.debug("参数名[第{}个]：{}，参数值：{}", new Object[] { i + 1, properties[i], values[i] });
		}
		return hibernateTemplate.findByNamedParam(queryString, properties, values);
	}

	@SuppressWarnings("unchecked")
	public List<E> findAllByProperty(Map<String, Object> paramValues) throws DataAccessException {
		if (MapUtils.isEmpty(paramValues)) {
			throw new Cm4jDataAccessException("查询参数对至少为一对");
		}
		StringBuilder hql = new StringBuilder("from ").append(this.getPersistentClass().getSimpleName()).append(
				" where ");
		String[] params = paramValues.keySet().toArray(new String[0]);
		for (String param : params) {
			if (StringUtils.isBlank(param)) {
				throw new Cm4jDataAccessException("查询参数名不允许为空");
			}
			hql = hql.append(param).append(" = :").append(param).append(" and ");
		}
		String queryString = StringUtils.substringBeforeLast(hql.toString(), "and");

		logger.debug("查询语句：{}", queryString);
		logger.debug("参数键值对：{}", paramValues);

		Session session = getSession();
		Query query = session.createQuery(queryString.toString());
		query.setProperties(paramValues);
		List<E> list = null;
		list = query.list();
		releaseSession(session);
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<E> page(String queryString, Map<String, Object> paramValues, int pageSize, int pageNo)
			throws DataAccessException {
		if (MapUtils.isEmpty(paramValues)) {
			throw new Cm4jDataAccessException("查询参数不允许为空");
		}

		logger.debug("查询语句：{}", queryString);
		logger.debug("参数键值对：{}", paramValues);

		Session session = this.getSession();
		Query query = session.createQuery(queryString);
		query.setFirstResult((pageNo - 1) * pageSize);
		query.setMaxResults(pageSize);
		query.setProperties(paramValues);
		List<E> list = null;
		list = query.list();
		releaseSession(session);
		return list;
	}

	public List<E> findAllByPropertyLike(String property, String value) throws DataAccessException {
		List<E> resultList = null;
		if (StringUtils.isBlank(property)) {
			throw new Cm4jDataAccessException("查询参数名不允许为空");
		}
		StringBuilder hql = new StringBuilder("FROM ").append(this.getPersistentClass().getSimpleName())
				.append(" WHERE ").append(property).append(" LIKE :").append(property);
		resultList = this.findAll(hql.toString(), property, value);
		return resultList;
	}

	@SuppressWarnings("unchecked")
	public List<E> findAllByPropertyAndOthers(Map<String, Object> propertyValues, String queryString,
			Map<String, Object> otherValues) throws HibernateException {
		if (MapUtils.isEmpty(otherValues)) {
			throw new Cm4jDataAccessException("查询参数不允许为空");
		}
		StringBuilder hql = new StringBuilder("FROM ").append(this.getPersistentClass().getSimpleName()).append(
				" WHERE ");

		if (propertyValues != null) {
			String[] params = propertyValues.keySet().toArray(new String[0]);
			for (String param : params) {
				if (StringUtils.isBlank(param)) {
					throw new Cm4jDataAccessException("查询参数名不允许为空");
				}
				hql = hql.append(param).append(" = :").append(param).append(" and ");
			}
			// 将2个参数Map拼装起来
			otherValues.putAll(propertyValues);
		}

		hql.append(queryString);

		logger.debug("查询语句：{}", queryString);
		logger.debug("参数键值对：{}", otherValues);

		Session session = this.getSession();
		Query query = session.createQuery(hql.toString());
		query.setProperties(otherValues);
		List<E> list = null;
		list = query.list();
		releaseSession(session);
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<E> pageByProperty(String property, Object value, int pageSize, int pageNo, String orderBy, boolean isAsc)
			throws DataAccessException {
		if (StringUtils.isBlank(property)) {
			throw new Cm4jDataAccessException("查询参数名不允许为空");
		}

		if (pageSize == 0) {
			throw new Cm4jDataAccessException("参数pageSize不能为0");
		}
		String orderStr = StringUtils.isBlank(orderBy) ? "" : orderBy + (isAsc ? " ASC" : " DESC");
		StringBuilder hql = new StringBuilder("FROM ").append(this.getPersistentClass().getSimpleName())
				.append(" WHERE ").append(property).append(" = :").append(property).append(" ORDER BY ")
				.append(orderStr).append(StringUtils.isBlank(orderBy) ? "" : ",").append("ID DESC");

		logger.debug("查询语句：{}", hql);
		logger.debug("参数键值对：property：{}，value：{}", property, value);

		Session session = this.getSession();
		Query query = session.createQuery(hql.toString());
		query.setFirstResult((pageNo - 1) * pageSize);
		query.setMaxResults(pageSize);
		query.setParameter(property, value);
		List<E> list = null;
		list = query.list();
		releaseSession(session);
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<E> pageByProperty(Map<String, Object> paramValues, int pageSize, int pageNo, String orderBy,
			Boolean isAsc) throws DataAccessException {
		if (MapUtils.isEmpty(paramValues)) {
			throw new Cm4jDataAccessException("查询参数不允许为空");
		}

		if (pageSize == 0) {
			throw new Cm4jDataAccessException("参数pageSize不能为0");
		}
		String[] params = paramValues.keySet().toArray(new String[0]);
		StringBuilder paramStr = new StringBuilder();
		for (String param : params) {
			paramStr = paramStr.append(param).append(" = :").append(param).append(" and ");
		}
		String queryString = StringUtils.substringBeforeLast(paramStr.toString(), "and");
		String orderStr = StringUtils.isBlank(orderBy) ? "" : orderBy + (isAsc ? " asc" : " desc");
		StringBuilder hql = new StringBuilder("from ").append(this.getPersistentClass().getSimpleName())
				.append(" where ").append(queryString).append(" order by ").append(orderStr)
				.append(StringUtils.isBlank(orderBy) ? "" : ",").append("id desc");

		logger.debug("查询语句：{}", hql);
		logger.debug("参数键值对：{}", paramValues);

		Session session = this.getSession();
		Query query = session.createQuery(hql.toString());
		query.setFirstResult((pageNo - 1) * pageSize);
		query.setMaxResults(pageSize);
		query.setProperties(paramValues);
		List<E> list = null;
		list = query.list();
		releaseSession(session);
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<E> pageByPropertyAndOthers(Map<String, Object> propertyValues, String queryString,
			Map<String, Object> otherValues, int pageSize, int pageNo, String orderBy, Boolean isAsc)
			throws DataAccessException {
		if (MapUtils.isEmpty(otherValues)) {
			throw new Cm4jDataAccessException("查询参数不允许为空");
		}
		StringBuilder hql = new StringBuilder("FROM ").append(this.getPersistentClass().getSimpleName()).append(
				" WHERE ");
		String[] params = propertyValues.keySet().toArray(new String[0]);
		for (String param : params) {
			if (StringUtils.isBlank(param)) {
				throw new Cm4jDataAccessException("查询参数名不允许为空");
			}
			hql = hql.append(param).append(" = :").append(param).append(" AND ");
		}
		String orderStr = StringUtils.isBlank(orderBy) ? "" : orderBy + (isAsc ? " ASC" : " DESC");
		hql.append(queryString).append(" ORDER BY ").append(orderStr).append(StringUtils.isBlank(orderBy) ? "" : ",")
				.append("id DESC");

		// 将2个参数Map拼装起来
		propertyValues.putAll(otherValues);

		logger.debug("查询语句：{}", hql);
		logger.debug("参数键值对：{}", otherValues);

		Session session = this.getSession();
		Query query = session.createQuery(hql.toString());
		query.setFirstResult((pageNo - 1) * pageSize);
		query.setMaxResults(pageSize);
		query.setProperties(propertyValues);
		List<E> list = null;
		list = query.list();
		releaseSession(session);
		return list;
	}

	public int count(String queryString, Map<String, Object> paramValues) throws DataAccessException {
		if (paramValues == null || paramValues.isEmpty()) {
			throw new Cm4jDataAccessException("查询参数对至少为一对");
		}
		Session session = this.getSession();
		Query q = session.createQuery(queryString);
		q.setProperties(paramValues);

		logger.debug("查询语句：{}", queryString);
		logger.debug("参数键值对：{}", paramValues);

		int result = 0;
		try {
			result = Integer.parseInt(q.iterate().next().toString());
		} catch (NumberFormatException e) {
			logger.error("数字转换错误{}", e);
		} catch (HibernateException e) {
			logger.debug("查询记录数为0");
		}

		releaseSession(session);

		return result;
	}

	public int countByProperty(Map<String, Object> paramValues) throws DataAccessException {
		if (paramValues == null || paramValues.isEmpty()) {
			throw new Cm4jDataAccessException("查询参数对至少为一对");
		}
		String[] params = paramValues.keySet().toArray(new String[0]);
		StringBuilder hql = new StringBuilder("SELECT COUNT(*) FROM ")
				.append(this.getPersistentClass().getSimpleName()).append(" where ");
		for (String param : params) {
			if (StringUtils.isBlank(param)) {
				throw new Cm4jDataAccessException("查询参数名不允许为空");
			}
			hql = hql.append(param).append(" = :").append(param).append(" and ");
		}
		String queryString = StringUtils.substringBeforeLast(hql.toString(), "and");

		Session session = this.getSession();
		Query q = session.createQuery(queryString);
		q.setProperties(paramValues);

		logger.debug("查询语句：{}", queryString);
		logger.debug("参数键值对：{}", paramValues);

		int result = 0;
		try {
			result = Integer.parseInt(q.iterate().next().toString());
		} catch (NumberFormatException e) {
			logger.error("数字转换错误", e);
		} catch (HibernateException e) {
			logger.debug("查询记录数为0");
		}
		releaseSession(session);
		return result;
	}

	public int countByPropertyAndOthers(Map<String, Object> propertyValues, String queryString,
			Map<String, Object> otherValues) throws DataAccessException {
		if (MapUtils.isEmpty(otherValues)) {
			throw new Cm4jDataAccessException("查询参数不允许为空");
		}
		StringBuilder hql = new StringBuilder("SELECT COUNT(*) FROM ")
				.append(this.getPersistentClass().getSimpleName()).append(" WHERE ");
		String[] params = propertyValues.keySet().toArray(new String[0]);
		for (String param : params) {
			if (StringUtils.isBlank(param)) {
				throw new Cm4jDataAccessException("查询参数名不允许为空");
			}
			hql = hql.append(param).append(" = :").append(param).append(" AND ");
		}
		hql.append(queryString);
		// 将2个参数Map拼装起来
		propertyValues.putAll(otherValues);
		Session session = this.getSession();
		Query query = session.createQuery(hql.toString());
		query.setProperties(propertyValues);

		logger.debug("查询语句：{}", queryString);
		logger.debug("参数键值对：{}", propertyValues);

		int result = 0;
		try {
			result = Integer.parseInt(query.iterate().next().toString());
		} catch (NumberFormatException e) {
			logger.error("数字转换错误", e);
		} catch (HibernateException e) {
			logger.debug("查询记录数为0");
		}
		releaseSession(session);
		return result;
	}

	@SuppressWarnings("unchecked")
	public Iterator<E> iterate(String queryString) throws DataAccessException {
		return hibernateTemplate.iterate(queryString);
	}

	@SuppressWarnings("unchecked")
	public Iterator<E> iterate(String queryString, Object value) throws DataAccessException {
		return hibernateTemplate.iterate(queryString, value);
	}

	@SuppressWarnings("unchecked")
	public Iterator<E> iterate(String queryString, Object[] values) throws DataAccessException {
		return hibernateTemplate.iterate(queryString, values);
	}

	/**
	 * 未实现
	 */
	public List<Map<String, Object>> executeWithResult(String spName, Map<String, Object> parameters,
			Map<String, Integer> outParams, String cursorName) throws DataAccessException {
		throw new Cm4jDataAccessException("no implements");
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> findSecondLevelCacheEntities(String regionName) {
		return hibernateTemplate.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(regionName)
				.getEntries();
	}

	// **************************************************************************
	// *
	// * -------------------------------保存删除操作----------------------------- *
	// **************************************************************************
	// /
	@SuppressWarnings("unchecked")
	public ID save(E entity) throws DataAccessException {
		return (ID) hibernateTemplate.save(entity);
	}

	public void delete(E entity) throws DataAccessException {
		hibernateTemplate.delete(entity);
	}

	public void deleteById(ID id) throws DataAccessException {
		E entity = this.findById(id);
		if (entity == null) {
			logger.warn("删除对象异常：对象{}未查询到", this.getPersistentClass().getSimpleName());
			throw new Cm4jDataAccessException("删除对象异常：对象" + this.getPersistentClass().getSimpleName() + "未查询到");
		}
		this.delete(entity);
	};

	public void deleteAll(Collection<E> entities) throws DataAccessException {
		hibernateTemplate.deleteAll(entities);
	}

	public void update(E entity) throws DataAccessException {
		hibernateTemplate.update(entity);
	}

	public int update(String queryString) throws DataAccessException {
		return hibernateTemplate.bulkUpdate(queryString);
	}

	public int update(String queryString, Object value) throws DataAccessException {
		return hibernateTemplate.bulkUpdate(queryString, value);
	}

	public int update(String queryString, Object[] values) throws DataAccessException {
		return hibernateTemplate.bulkUpdate(queryString, values);
	}

	public void saveOrUpdate(E entity) throws DataAccessException {
		hibernateTemplate.saveOrUpdate(entity);
	};

	public void saveOrUpdateAll(Collection<E> entities) throws DataAccessException {
		hibernateTemplate.saveOrUpdateAll(entities);
	}

	/**
	 * spring中HibernateDaoSupport中提供的getSession方法
	 * 
	 * @return
	 */
	protected Session getSession() throws DataAccessResourceFailureException, IllegalStateException {
		boolean allowCreate = hibernateTemplate.isAllowCreate();
		return (!allowCreate ? SessionFactoryUtils.getSession(getSessionFactory(), false) : SessionFactoryUtils
				.getSession(getSessionFactory(), this.hibernateTemplate.getEntityInterceptor(),
						this.hibernateTemplate.getJdbcExceptionTranslator()));
	}

	/**
	 * 安全关闭session，不会关闭带事务的session Close the given Session, created via the
	 * given factory, if it is not managed externally (i.e. not bound to the
	 * thread).
	 * 
	 * @param session
	 */
	protected final void releaseSession(Session session) {
		SessionFactoryUtils.releaseSession(session, getSessionFactory());
	}

	/**
	 * Setter注入SessionFactory，同时获得HibernateTemplate
	 * 
	 * @param sessionFactory
	 */
	@Autowired(required = false)
	public void setSessionFactory(SessionFactory sessionFactory) {
		logger.debug("HibernateDao设置sessionFacotry，持久化类为：{}", persistentClass);
		// 首先，检查原来的hibernateTemplate实例是否还存在
		if (this.hibernateTemplate == null || sessionFactory != this.hibernateTemplate.getSessionFactory()) {
			this.hibernateTemplate = new HibernateTemplate(sessionFactory);
		}
	}

	/**
	 * Return the Hibernate SessionFactory used by this DAO.
	 */
	public final SessionFactory getSessionFactory() {
		return (this.hibernateTemplate != null ? this.hibernateTemplate.getSessionFactory() : null);
	}

	/**
	 * 获取HibernateTemplate
	 * 
	 * @return
	 */
	public HibernateTemplate getHibernateTemplate() {
		return hibernateTemplate;
	}

}
