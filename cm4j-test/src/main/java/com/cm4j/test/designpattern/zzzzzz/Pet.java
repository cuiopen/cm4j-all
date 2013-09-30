package com.cm4j.test.designpattern.zzzzzz;

import com.google.common.base.Joiner;

public class Pet {

	private int x, y, z;

	public Pet(int x, int y, int z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	@Override
	public String toString() {
		return Joiner.on("_").join(this.x, this.y, this.z);
	}
}
