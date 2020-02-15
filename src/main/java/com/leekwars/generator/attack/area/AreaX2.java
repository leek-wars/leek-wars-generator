package com.leekwars.generator.attack.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaX2 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateXMask(2);

	public AreaX2(Attack attack) {
		super(attack, area);
	}
}
