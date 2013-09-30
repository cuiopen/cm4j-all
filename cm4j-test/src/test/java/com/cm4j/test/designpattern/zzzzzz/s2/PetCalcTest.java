package com.cm4j.test.designpattern.zzzzzz.s2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.PetCalcContext;
import com.cm4j.test.designpattern.zzzzzz.s2.common.PetCalc;
import com.cm4j.test.designpattern.zzzzzz.s2.strategy.PetCalcA;
import com.cm4j.test.designpattern.zzzzzz.s2.strategy.PetCalcB;
import com.cm4j.test.designpattern.zzzzzz.s2.strategy.PetCalcC;

public class PetCalcTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void t1() {
		PetCalc petCalc = new PetCalc();
		logger.debug("结果:{}\n", petCalc.calc(new Pet(10, 10, 10), "A"));
		logger.debug("结果:{}\n", petCalc.calc(new Pet(10, 10, 10), "B"));
		logger.debug("结果:{}\n", petCalc.calc(new Pet(10, 10, 10), "C"));
	}

	@Test
	public void t2() {
		logger.debug("结果:{}\n", new PetCalcContext(new PetCalcA()).calc(new Pet(10, 10, 10)));
		logger.debug("结果:{}\n", new PetCalcContext(new PetCalcB()).calc(new Pet(10, 10, 10)));
		logger.debug("结果:{}\n", new PetCalcContext(new PetCalcC()).calc(new Pet(10, 10, 10)));
	}
}
