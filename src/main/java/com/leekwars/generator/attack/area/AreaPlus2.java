package com.leekwars.generator.attack.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaPlus2 extends MaskArea {

	private static int[][] area = MaskAreaCell.generatePlusMask(2);

	public AreaPlus2(Attack attack) {
		super(attack, area);
	}
}
