package com.cm4j.test.designpattern.zzzzzz.s1.strategy;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.PetUtils;

public class PetCalcB implements IPetCalc {

	@Override
	public int calc(Pet pet) {
		return PetUtils.calcB(pet);
	}
}
