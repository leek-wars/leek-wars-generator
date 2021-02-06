package com.leekwars.generator.attack.area;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;

public class AreaFirstInLine extends Area {

	public AreaFirstInLine(Attack attack) {
		super(attack);
	}

	@Override
	public List<Cell> getArea(Cell launchCell, Cell targetCell) {
		List<Cell> cells = new ArrayList<>();
		Cell cell = Pathfinding.getFirstEntity(launchCell, targetCell, mAttack.getMinRange(), mAttack.getMaxRange());
		if (cell != null) {
			cells.add(cell);
		}
		return cells;
	}
}
