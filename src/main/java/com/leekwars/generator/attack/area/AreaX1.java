package com.leekwars.generator.attack.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaX1 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateXMask(1);

	public AreaX1(Attack attack) {
		super(attack, area);
	}
}
