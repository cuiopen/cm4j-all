package com.cm4j.test.designpattern.zzzzzz.s3.strategy;

import com.cm4j.test.designpattern.zzzzzz.Pet;

public class PlayerContext {

	private AbsPlayer player;

	public PlayerContext(AbsPlayer player) {
		super();
		this.player = player;
	}

	public int calc(Pet pet) {
		return player.calc(pet);
	}
}
