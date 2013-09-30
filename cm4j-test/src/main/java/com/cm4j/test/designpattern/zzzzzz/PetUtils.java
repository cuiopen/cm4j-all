package com.cm4j.test.designpattern.zzzzzz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetUtils {

	private static final Logger logger = LoggerFactory.getLogger(PetUtils.class);

	/**
	 * 计算战斗力A
	 * 
	 * @param pet
	 * @return
	 */
	public static int calcA(Pet pet) {
		int result = pet.getX() + pet.getY() + pet.getZ();
		logger.debug("宠物[{}]，方法A[X+Y+Z={}]", pet, result);
		return result;
	}

	/**
	 * 计算战斗力B
	 * 
	 * @param pet
	 * @return
	 */
	public static int calcB(Pet pet) {
		int result = pet.getX() + pet.getY() - pet.getZ();
		logger.debug("宠物[{}]，方法B[X+Y-Z={}]", pet, result);
		return result;
	}

	/**
	 * 计算战斗力C
	 * 
	 * @param pet
	 * @return
	 */
	public static int calcC(Pet pet) {
		int result = pet.getX() + pet.getY() * pet.getZ();
		logger.debug("宠物[{}]，方法C[X+Y*Z={}]", pet, result);
		return result;
	}

	/**
	 * 对战斗力后续处理 +1000
	 * 
	 * @param result
	 * @return
	 */
	public static int calcAfterResult(int result) {
		logger.debug("战斗力{} + 1000 = {}", result, result + 1000);
		return result + 1000;
	}
}
