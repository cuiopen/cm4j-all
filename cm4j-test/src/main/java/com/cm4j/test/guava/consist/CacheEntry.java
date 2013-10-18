package com.cm4j.test.guava.consist;

import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.keys.Identity;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单个缓存值，例如数据库中的一行数据<br>
 * 注意：此类会被copy，所以不要包含大对象，如logger，以防止效率降低
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:25:24
 * 
 */
public abstract class CacheEntry {

	/**
	 * 缓存状态 - 默认持久化
	 */
	private volatile DBState dbState = DBState.P;
	/**
	 * 在更新队列中的数量
	 */
	private final AtomicInteger numInUpdateQueue = new AtomicInteger(0);

	/**
	 * 此对象所依附的key
	 */
	private AbsReference ref;

	/**
	 * 更新此对象
	 */
	public void update() {
		Preconditions.checkNotNull(this.ref,"缓存中不存在此对象，请调用Reference中方法添加到缓存中");
		this.ref.updateEntry(this);
	}

	/**
	 * 删除此对象
	 */
	public void delete() {
		Preconditions.checkNotNull(this.ref);
		this.ref.deleteEntry(this);
	}

	/**
	 * 根据当前缓存对象解析IEntity进行数据保存<br>
	 * 
	 * 注意：当CacheEntry是IEntity的一个接口，如果不是，则深拷贝IEntity<br>
	 * 1.子类直接返回this，此时会对IEntity直接深拷贝[性能略低，代码简单]<br>
	 * 2.子类新建IEntity对象，则不会深拷贝，直接用这个new出来的对象[性能稍高，代码臃肿了点]<br>
	 */
	public abstract IEntity parseEntity();

	/**
	 * 修改Entry状态且不为P的时候发送给更新队列
	 * 
	 * @param dbState
	 *            <font color=red>如果为P，则不发送到更新队列</font>
	 */
	protected void changeDbState(DBState dbState) {
		setDbState(dbState);
		if (DBState.P != dbState) {
			ConcurrentCache.getInstance().sendToUpdateQueue(this);
		}
	}

	/**
	 * 由子类覆盖
	 * 
	 * @return
	 */
	protected Object getIdentity() {
		return null;
	}

	/**
	 * 唯一标识的数值 1.子类覆写 {@link #getIdentity()} 2.子类实现标识注解ID在字段上
	 */
	public Object getID() {
		Object result = getIdentity();
		if (result != null) {
			return result;
		}
		Method[] methods = this.getClass().getDeclaredMethods();
		for (Method method : methods) {
			if (method.isAnnotationPresent(Identity.class)) {
				try {
					result = method.invoke(this);
					if (result != null) {
						return result;
					}
				} catch (Exception e) {
					// 获取CacheEntry.ID异常
					Throwables.propagate(e);
				}
			}
		}
		throw new RuntimeException("CacheEntry的标识不能为空");
	}

	/*
	 * ==================== getter and setter ===================
	 */

	public DBState getDbState() {
		return dbState;
	}

	synchronized void setDbState(DBState dbState) {
		this.dbState = dbState;
	}

	AtomicInteger getNumInUpdateQueue() {
		return numInUpdateQueue;
	}

	public AbsReference ref() {
		return this.ref;
	}

	public void resetRef(AbsReference ref) {
		this.ref = ref;
	}
}
