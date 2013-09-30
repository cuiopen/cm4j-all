package com.cm4j.test.designpattern.zzzzzz.s2.strategy;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.PetUtils;

public class PetCalcB extends AbsPetCalc {

	/**
	 * 覆写父类方法，以改变计算流程
	 */
	@Override
	public int calc(Pet pet) {
		int source = super.calc(pet);
		int result = source * 10;
		logger.error("覆写父类方法，结果{}*10={}", source, result);
		return result;
	}

	@Override
	public int calcInternal(Pet pet) {
		return PetUtils.calcB(pet);
	}

}
