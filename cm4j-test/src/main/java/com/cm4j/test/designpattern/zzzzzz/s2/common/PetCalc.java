package com.cm4j.test.designpattern.zzzzzz.s2.common;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.PetUtils;

public class PetCalc {

	public int calc(Pet pet, String type) {
		if ("A".equals(type)) {
			int result = PetUtils.calcA(pet);
			return PetUtils.calcAfterResult(result);
		} else if ("B".equals(type)) {
			int result = PetUtils.calcB(pet);
			return PetUtils.calcAfterResult(result);
		} else if ("C".equals(type)) {
			int result = PetUtils.calcC(pet);
			return result;
		} else {
			throw new IllegalArgumentException();
		}
	}
}
