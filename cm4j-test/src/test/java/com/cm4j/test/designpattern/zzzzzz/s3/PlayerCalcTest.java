package com.cm4j.test.designpattern.zzzzzz.s3;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.s2.strategy.PetCalcA;
import com.cm4j.test.designpattern.zzzzzz.s2.strategy.PetCalcB;
import com.cm4j.test.designpattern.zzzzzz.s2.strategy.PetCalcC;
import com.cm4j.test.designpattern.zzzzzz.s3.strategy.PlayerA;
import com.cm4j.test.designpattern.zzzzzz.s3.strategy.PlayerB;
import com.cm4j.test.designpattern.zzzzzz.s3.strategy.PlayerContext;

public class PlayerCalcTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void t1() {
		logger.debug("结果:{}\n", new PlayerContext(new PlayerA(new PetCalcA())).calc(new Pet(10, 10, 10)));
		logger.debug("结果:{}\n", new PlayerContext(new PlayerA(new PetCalcB())).calc(new Pet(10, 10, 10)));
		logger.debug("结果:{}\n", new PlayerContext(new PlayerA(new PetCalcC())).calc(new Pet(10, 10, 10)));

		logger.debug("结果:{}\n", new PlayerContext(new PlayerB(new PetCalcA())).calc(new Pet(10, 10, 10)));
		logger.debug("结果:{}\n", new PlayerContext(new PlayerB(new PetCalcB())).calc(new Pet(10, 10, 10)));
		logger.debug("结果:{}\n", new PlayerContext(new PlayerB(new PetCalcC())).calc(new Pet(10, 10, 10)));
	}
}
