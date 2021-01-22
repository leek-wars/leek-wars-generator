package com.leekwars.generator.attack.area;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaSquare1 extends MaskArea {

	private static int[][] area = MaskAreaCell.generateSquareMask(1);

	public AreaSquare1(Attack attack) {
		super(attack, area);
	}
}
