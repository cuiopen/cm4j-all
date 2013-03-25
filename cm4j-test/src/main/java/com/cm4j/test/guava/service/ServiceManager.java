package com.cm4j.test.guava.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * 服务管理静态类
 * 
 * @author Yang.hao
 * @since 2013-1-19 上午11:10:40
 * 
 */
@Service
public class ServiceManager implements ApplicationContextAware {

	private static ServiceManager instance = null;

	private ApplicationContext ctx;

	@PostConstruct
	public void init() {
		instance = this;
	}

	public static ServiceManager getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public final <V> V getSpringBean(String beanName) {
		return (V) ctx.getBean(beanName);
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.ctx = ctx;
	}
}
