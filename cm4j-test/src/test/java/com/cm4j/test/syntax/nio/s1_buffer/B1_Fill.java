package com.cm4j.test.syntax.nio.s1_buffer;

import org.junit.Test;

import java.nio.CharBuffer;

/**
 * 对缓存进行填充和释放
 * 
 * @author yanghao
 * 
 */
public class B1_Fill {

	@Test
	public void fillAndClear() {
		CharBuffer buffer = CharBuffer.allocate(10000);
		fillBuffer(buffer);
		buffer.flip();
		System.out.println(buffer.toString());
		buffer.clear();// 只是对位置等调整，里面的char值不做修改
		System.out.println(buffer.toString());
	}

	@Test
	public void wrap() {
		CharBuffer buffer = CharBuffer.wrap(new char[] { 'A', 'B' });
		System.out.println(buffer.position());
	}

	private static String[] strings = { "A random string value", "The product of an infinite number of monkeys",
			"Hey hey we're the Monkees", "Opening act for the Monkees: Jimi Hendrix",
			"'Scuse me while I kiss this fly", // Sorry Jimi ;-)
			"Help Me!  Help Me!", };

	private static void fillBuffer(CharBuffer buffer) {
		for (String string : strings) {
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
		}
	}
}
