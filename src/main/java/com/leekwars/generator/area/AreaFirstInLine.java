package com.leekwars.generator.area;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;

public class AreaFirstInLine extends Area {

	public AreaFirstInLine(Attack attack) {
		super(attack);
	}

	@Override
	public List<Cell> getArea(Map map, Cell launchCell, Cell targetCell) {
		List<Cell> cells = new ArrayList<>();
		Cell cell = map.getFirstEntity(launchCell, targetCell, mAttack.getMinRange(), mAttack.getMaxRange());
		if (cell != null) {
			cells.add(cell);
		}
		return cells;
	}
}
