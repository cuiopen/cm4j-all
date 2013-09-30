package com.cm4j.test.designpattern.zzzzzz.s1.strategy;

import com.cm4j.test.designpattern.zzzzzz.Pet;

public class PetCalcContext {

	private IPetCalc petCalc;

	public PetCalcContext(IPetCalc petCalc) {
		super();
		this.petCalc = petCalc;
	}

	public int calc(Pet pet) {
		return petCalc.calc(pet);
	}
}
