package com.leekwars.generator.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaPlus3 extends MaskArea {

	private static int[][] area = MaskAreaCell.generatePlusMask(3);

	public AreaPlus3(Attack attack) {
		super(attack, area);
	}
}
