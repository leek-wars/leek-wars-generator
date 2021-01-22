package com.leekwars.generator.attack.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaSquare2 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateSquareMask(2);

	public AreaSquare2(Attack attack) {
		super(attack, area);
	}
}
