package com.cm4j.test.designpattern.zzzzzz.s3.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.IPetCalc;

public abstract class AbsPlayer {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected IPetCalc petCalc;

	public AbsPlayer(IPetCalc petCalc) {
		this.petCalc = petCalc;
	}

	public int calc(Pet pet) {
		petReset(pet);
		return petCalc.calc(pet);
	}

	/**
	 * 宠物数值重置
	 */
	protected abstract void petReset(Pet pet);

}