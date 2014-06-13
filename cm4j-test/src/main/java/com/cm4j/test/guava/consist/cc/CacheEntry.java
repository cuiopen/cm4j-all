package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.fifo.FIFOEntry;
import com.cm4j.test.guava.consist.keys.Identity;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单个缓存值，例如数据库中的一行数据<br>
 * 注意：此类会被copy，所以不要包含大对象，如logger，以防止效率降低
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:25:24
 * 
 */
abstract class CacheEntry extends FIFOEntry<AbsReference> {

	/**
	 * 缓存状态 - 默认持久化
	 */
	private volatile DBState dbState = DBState.P;
	/**
	 * 放到persist队列则设置为true，每次迭代到则设为false
     * 修改完成时，检测是否是false，如果不是，则代表有其他线程修改了，这时不能设置DBState为P
	 */
	private final AtomicBoolean isChanged = new AtomicBoolean(false);

    private IEntity mirrorEntity;
    private DBState mirrorDBState;

    public IEntity mirrorEntity() {
        return mirrorEntity;
    }

    public DBState mirrorDBState() {
        return mirrorDBState;
    }

    /**
     * 放入队列时
     * 需要把当前状态和数据做一个镜像备份，保存db时就用备份数据，这样不会出现当前数据更改了，影响到保存DB的数据
     */
    public void mirror() {
        getIsChanged().set(true);

        IEntity parseEntity = this.parseEntity();
        if (this instanceof IEntity && (this != parseEntity)) {
            // 内存地址不同，创建了新对象
            this.mirrorEntity = parseEntity;
        } else {
            // 其他情况，属性拷贝
            try {
                IEntity entity = parseEntity.getClass().newInstance();
                BeanUtils.copyProperties(this, entity);
                this.mirrorEntity = entity;
            } catch (Exception e) {
                throw new RuntimeException("CacheEntry[" + this.ref() + "]不能被PropertyCopy", e);
            }
        }
        this.mirrorDBState = this.dbState;
    }

    /**
	 * 更新此对象
	 */
	public void update() {
		Preconditions.checkNotNull(this.ref(),"缓存中不存在此对象，请调用Reference中方法添加到缓存中");
		this.ref().updateEntry(this);
	}

	/**
	 * 删除此对象
	 */
	public void delete() {
		Preconditions.checkNotNull(this.ref());
		this.ref().deleteEntry(this);
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
			ConcurrentCache.getInstance().sendToPersistQueue(this);
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

	AtomicBoolean getIsChanged() {
		return isChanged;
	}

	public AbsReference ref() {
		return super.getQueueEntry();
	}

	public void resetRef(AbsReference ref) {
		super.setQueueEntry(ref);
	}
}
