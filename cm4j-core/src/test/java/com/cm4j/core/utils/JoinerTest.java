package com.cm4j.core.utils;

import org.junit.Test;

import com.google.common.base.Joiner;

public class JoinerTest {

	@Test
	public void test() {
		System.out.println(Joiner.on("_").join(1, 2, 3));
	}
}
