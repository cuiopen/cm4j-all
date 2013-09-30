package com.cm4j.test.spring.dal.ibatis.dao;

import com.cm4j.test.spring.dal.ibatis.pojo.Passport;

/**
 * 因为包依赖关系（SqlSessionDaoSupport），注释以保证编译通过
 */
public class PassportDao {// extends SqlSessionDaoSupport {
	private static final String NAMESPACE = "com.woniu.spring.dal.ibatis.dao.PassportDao.";
	private static final String STMT_QUERY_PASSPORT = NAMESPACE + "queryPassport";

	public Passport queryPassport(String userName) {
		// return (Passport) getSqlSession().selectOne(STMT_QUERY_PASSPORT, StringUtils.upperCase(userName));
        return null;
	}
}
