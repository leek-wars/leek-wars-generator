package com.leekwars.generator.area;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.state.Entity;

public class AreaSingleCell extends Area {

	public AreaSingleCell(Attack attack) {
		super(attack);
	}

	@Override
	public List<Cell> getArea(Map map, Cell launchCell, Cell targetCell, Entity caster) {
		ArrayList<Cell> area = new ArrayList<Cell>();
		area.add(targetCell);
		return area;
	}
}
