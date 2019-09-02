package com.leekwars.generator.attack.area;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.MaskAreaCell;

public class AreaCircle2 extends Area {

	private static int[][] area = MaskAreaCell.generateCircleMask(0, 2);

	public AreaCircle2(Attack attack) {
		super(attack);
	}

	@Override
	public List<Cell> getArea(Cell launchCell, Cell targetCell) {
		int x = targetCell.getX(), y = targetCell.getY();
		ArrayList<Cell> cells = new ArrayList<Cell>();
		for (int i = 0; i < area.length; i++) {
			Cell c = targetCell.getMap().getCell(x + area[i][0], y + area[i][1]);
			if (c == null || !c.isWalkable())
				continue;
			cells.add(c);
		}
		return cells;
	}

	@Override
	public int getRadius() {
		return 2;
	}
}
