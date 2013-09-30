package com.cm4j.test.designpattern.zzzzzz.s1.strategy;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.PetUtils;

public class PetCalcA implements IPetCalc {

	@Override
	public int calc(Pet pet) {
		return PetUtils.calcA(pet);
	}

}
