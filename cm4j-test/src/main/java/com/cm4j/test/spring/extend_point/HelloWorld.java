package com.cm4j.test.spring.extend_point;

import com.cm4j.test.spring.extend_point.events.Animals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

/**
 * none-lazy-init
 * 
 * @author yanghao
 * 
 */
public class HelloWorld {

    private String demoProperty;

    private static final Logger logger = LoggerFactory.getLogger(HelloWorld.class);

    public static void main(String[] args) throws Exception {
        // spring加载过程
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");

        // FactoryBean的使用
        ctx.getBean("factoryBeanCreator");

        // beanFactoryPostProcessor
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) ctx.getBean("selfDefined");
        logger.debug("beanfactory-extend.xml的map1值:{}", map.get("key1"));
        logger.debug("beanfactory-extend.xml的map2值:{}", map.get("key2"));

        // IOC的扩展点
        DemoAwareObject awareObject = ctx.getBean(DemoAwareObject.class);
        awareObject.exec();

        // 事件监听 - 触发
        Animals animals = ctx.getBean(Animals.class);
        animals.speak();
    }

    public void setDemoProperty(String demoProperty) {
        this.demoProperty = demoProperty;
    }

    public String getDemoProperty() {
        return demoProperty;
    }
}
