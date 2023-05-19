package com.leekwars.generator.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaCircle2 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateCircleMask(0, 2);

	public AreaCircle2(Attack attack) {
		super(attack, area);
	}
}
