package com.cm4j.test.designpattern.listener;

import org.junit.Test;

public class EventMulticasterTest {

	@Test
	public void testEvent() {
		EventMulticaster multicaster = new EventMulticaster();
		multicaster.addListener(new ApplicationListener<Event1>() {
			@Override
			public void onEvent(Event1 event) {
				System.out.println(1);
			}
		});
		multicaster.multicastEvent(new Event1(this));

		multicaster.addListener(new ApplicationListener<Event1>() {
			@Override
			public void onEvent(Event1 event) {
				System.out.println(2);
			}
		});
		multicaster.multicastEvent(new Event1(this));
	}
}

class Event1 extends ApplicationEvent {
	private static final long serialVersionUID = 1L;

	public Event1(Object source) {
		super(source);
	}
}
