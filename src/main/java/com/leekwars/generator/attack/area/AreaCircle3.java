package com.leekwars.generator.attack.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaCircle3 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateCircleMask(0, 3);

	public AreaCircle3(Attack attack) {
		super(attack, area);
	}
}
