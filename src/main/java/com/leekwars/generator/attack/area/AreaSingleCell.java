package com.leekwars.game.attack.area;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.game.attack.Attack;
import com.leekwars.game.maps.Cell;

public class AreaSingleCell extends Area {

	public AreaSingleCell(Attack attack) {
		super(attack);
	}

	@Override
	public List<Cell> getArea(Cell launchCell, Cell targetCell) {
		ArrayList<Cell> area = new ArrayList<Cell>();
		area.add(targetCell);
		return area;
	}

	@Override
	public int getRadius() {
		return 0;
	}
}
