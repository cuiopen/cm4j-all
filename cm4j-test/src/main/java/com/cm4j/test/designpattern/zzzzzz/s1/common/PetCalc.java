package com.cm4j.test.designpattern.zzzzzz.s1.common;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.PetUtils;

public class PetCalc {

	public int calc(Pet pet, String type) {
		if ("A".equals(type)) {
			return PetUtils.calcA(pet);
		} else if ("B".equals(type)) {
			return PetUtils.calcB(pet);
		} else {
			throw new IllegalArgumentException();
		}
	}
}