package com.cm4j.test.guava.consist.keys;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cm4j.test.guava.consist.cc.CacheEntry;

/**
 * {@link CacheEntry}的ID标识符，基于此标识来获取唯一标识
 * 
 * @author Yang.hao
 * @since 2013-4-9 上午10:22:43
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
// 将此注解包含在 javadoc 中
public @interface Identity {

}
