package com.cm4j.test.designpattern.bridge.draw;

/**
 * 多种方式画线和画圆
 * 
 * 对应多个子类
 * 
 * @author Yang.hao
 * @since 2012-10-22 下午02:05:35
 *
 */
public abstract class Drawing {
	
	abstract public void drawLine (double x1,double y1,double x2,double y2);
	
	abstract public void drawCircle (double x,double y,double z);
	
}
