package com.leekwars.generator.area;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.state.Entity;

public class MaskArea extends Area {

	private int[][] area;

	public MaskArea(Attack attack, int[][] area) {
		super(attack);
		this.area = area;
	}

	@Override
	public List<Cell> getArea(Map map, Cell launchCell, Cell targetCell, Entity caster) {
		int x = targetCell.getX(), y = targetCell.getY();
		ArrayList<Cell> cells = new ArrayList<Cell>();
		for (int i = 0; i < area.length; i++) {
			Cell c = map.getCell(x + area[i][0], y + area[i][1]);
			if (c == null || !c.isWalkable())
				continue;
			cells.add(c);
		}
		return cells;
	}
}
