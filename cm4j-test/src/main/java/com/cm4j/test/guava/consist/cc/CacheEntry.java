package com.cm4j.test.guava.consist.cc;

import com.cm4j.test.guava.consist.cc.persist.DBState;
import com.cm4j.test.guava.consist.entity.IEntity;
import com.cm4j.test.guava.consist.fifo.FIFOEntry;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;

/**
 * 单个缓存值，例如数据库中的一行数据<br>
 * 注意：此类会被copy，所以不要包含大对象，如logger，以防止效率降低
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:25:24
 * 
 */
public abstract class CacheEntry extends FIFOEntry<AbsReference> {

	/**
	 * 缓存状态 - 默认持久化
	 */
	private volatile DBState dbState = DBState.P;
    /**
     * 当前缓存的修改版本号，每修改一次+1
     * 用于设置状态P的时候判断版本是否一致，不一致代表被业务线程修改了，不能改为P
     */
    private volatile int version;

    /**
     * 放入队列时
     * 需要把当前状态和数据做一个镜像备份，保存db时就用备份数据，这样不会出现当前数据更改了，影响到保存DB的数据
     */
    public CacheMirror mirror() {
        IEntity mirror = null;
        IEntity parseEntity = this.parseEntity();
        if (this instanceof IEntity && (this != parseEntity)) {
            // 内存地址不同，创建了新对象
            mirror = parseEntity;
        } else {
            // 其他情况，属性拷贝
            try {
                IEntity entity = parseEntity.getClass().newInstance();
                BeanUtils.copyProperties(this, entity);
                mirror = entity;
            } catch (Exception e) {
                throw new RuntimeException("CacheEntry[" + this.ref() + "]不能被PropertyCopy", e);
            }
        }
        return new CacheMirror(this, mirror);
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
		this.ref().delete(this);
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

        // 这里只修改状态，不发送到queue了
		/*if (DBState.P != dbState) {
			ConcurrentCache.getInstance().sendToPersistQueue(this);
		}*/
	}

	/**
	 * 由子类覆盖
	 * 
	 * @return
	 */
	protected String dbUID() {
		return null;
	}

	/**
	 * 唯一标识的数值 1.子类覆写 {@link #dbUID()} 2.子类实现标识注解ID在字段上
	 */
	public String getID() {
		String uid = dbUID();
		if (StringUtils.isNotBlank(uid)) {
			return ref().getAttachedKey() + "`" + uid;
		}

		// todo 如果为空，可用反射获取主键ID
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

    public int getVersion() {
        return version;
    }

    synchronized void setVersion(int version) {
        this.version = version;
    }

    public AbsReference ref() {
		return super.getQueueEntry();
	}

	public void resetRef(AbsReference ref) {
		super.setQueueEntry(ref);
	}
}
