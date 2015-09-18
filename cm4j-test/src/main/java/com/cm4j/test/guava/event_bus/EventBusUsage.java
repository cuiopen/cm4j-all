package com.cm4j.test.guava.event_bus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Created by yanghao on 2015/6/5.
 */
public class EventBusUsage {

    final EventBus eventBus = new EventBus();

    public void test() {
        eventBus.register(new Object() {
            // @AllowConcurrentEvents：默认不是线程安全的，加了注解保证线多线程安全调用
            @AllowConcurrentEvents
            @Subscribe
            public void lister(Integer intVal) {
                System.out.printf("%s from int%n", intVal);
            }

            @Subscribe
            public void lister2(Integer intVal) {
                System.out.printf("%s from int2%n", intVal);
            }

            @Subscribe
            public void lister(Long longVal) {
                System.out.printf("%s from long%n", longVal);
            }

            // 父类监听，可同时监听到int与long
            @Subscribe
            public void lister(Number num) {
                System.out.printf("%s from Number%n", num);
            }

            @Subscribe
            public void lister(String str) {
                System.out.printf("%s from stromg%n", str);
            }

            // DeadEvent:捕获到无监听器的事件
            @Subscribe
            public void lister(DeadEvent event) {
                System.out.println("not found event:" + event.getEvent());
            }
        });

        eventBus.register(new Object(){
            // @AllowConcurrentEvents：默认不是线程安全的，加了注解保证线多线程安全调用
            @AllowConcurrentEvents
            @Subscribe
            public void lister(Integer intVal) {
                System.out.printf("%s from int NEW %n", intVal);
            }
        });

        // print
        // 1 from int
        // 1 from Number
        eventBus.post(1);

        // 1 from long
        // 1 from Number
        eventBus.post(1L);

        // xyz from stromg
        eventBus.post("xyz");

        // not found event:java.lang.Object@c42916
        eventBus.post(new Object());
    }

    public static void main(String[] args) {
        EventBusUsage usage = new EventBusUsage();
        usage.test();
    }
}
