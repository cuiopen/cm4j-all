package com.cm4j.test.designpattern.zzzzzz.s2.strategy;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.PetUtils;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.IPetCalc;

public abstract class AbsPetCalc implements IPetCalc {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 原来calc()方法
	 * 
	 * @param pet
	 * @return
	 */
	protected abstract int calcInternal(Pet pet);

	@Override
	public int calc(Pet pet) {
		int result = calcInternal(pet);
		// return result;
		return isAfterDeal() ? PetUtils.calcAfterResult(result) : result;
	}

	/**
	 * 钩子方法：子类可覆写此方法来实现对父类流程的控制
	 * 
	 * @return
	 */
	protected boolean isAfterDeal() {
		return true;
	}

}
