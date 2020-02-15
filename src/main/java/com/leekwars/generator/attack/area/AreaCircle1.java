package com.leekwars.generator.attack.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaCircle1 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateCircleMask(0, 1);

	public AreaCircle1(Attack attack) {
		super(attack, area);
	}
}
