package com.leekwars.generator.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaX3 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateXMask(3);

	public AreaX3(Attack attack) {
		super(attack, area);
	}
}
