package com.cm4j.test.designpattern.zzzzzz.s3.strategy;

import com.cm4j.test.designpattern.zzzzzz.Pet;
import com.cm4j.test.designpattern.zzzzzz.s1.strategy.IPetCalc;

public class PlayerB extends AbsPlayer {

	public PlayerB(IPetCalc petCalc) {
		super(petCalc);
	}

	@Override
	public void petReset(Pet pet) {
		String source = pet.toString();

		pet.setX(pet.getX() / 2);
		pet.setY(pet.getY() / 2);
		pet.setZ(pet.getZ() / 2);

		logger.debug("宠物[{}]基础数值/2后为[{}]", source, pet.toString());
	}
}
