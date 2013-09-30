package com.cm4j.test.designpattern.zzzzzz.s1;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.s1.common.PetCalc;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.IPetCalc;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.PetCalcA;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.PetCalcB;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.PetCalcContext;

public class PetCalcTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void t1() {
		PetCalc petCalc = new PetCalc();
		logger.debug("结果:{}\n", petCalc.calc(new Pet(10, 10, 10), "A"));
		logger.debug("结果:{}\n", petCalc.calc(new Pet(10, 10, 10), "B"));
	}

	@Test
	public void t2() {
		logger.debug("结果:{}\n", new PetCalcContext(new PetCalcA()).calc(new Pet(10, 10, 10)));
		logger.debug("结果:{}\n", new PetCalcContext(new PetCalcB()).calc(new Pet(10, 10, 10)));
	}

	@Test
	public void t3() {
		final int random = RandomUtils.nextInt(100);

		// 延迟设计与匿名内部类
		logger.debug("结果:{}\n", new PetCalcContext(new IPetCalc() {
			@Override
			public int calc(Pet pet) {
				int result = pet.getX() * pet.getY() * pet.getZ() * random;
				logger.debug("宠物[{}]，方法O[X*Y*Z*{}={}]", new Object[] { pet, random, result });
				return result;
			}
		}).calc(new Pet(10, 10, 10)));
	}
}
