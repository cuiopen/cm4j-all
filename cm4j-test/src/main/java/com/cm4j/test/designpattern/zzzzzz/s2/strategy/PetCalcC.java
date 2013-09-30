package com.cm4j.test.designpattern.zzzzzz.s2.strategy;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.PetUtils;

public class PetCalcC extends AbsPetCalc {

	@Override
	protected boolean isAfterDeal() {
		return false;
	}

	@Override
	public int calcInternal(Pet pet) {
		return PetUtils.calcC(pet);
	}

}
