package com.leekwars.generator.area;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.state.Entity;

public class AreaAllies extends Area {

	public AreaAllies(Attack attack) {
		super(attack);
	}

	@Override
	public List<Cell> getArea(Map map, Cell launchCell, Cell targetCell, Entity caster) {
		var cells = new ArrayList<Cell>();
		if (caster != null) {
			for (var entity : map.getState().getEntities().values()) {
				if (entity.getTeam() == caster.getTeam() && entity.getName().contains("crystal")) continue;
				if (entity.getCell() != null && entity.getTeam() == caster.getTeam()) {
					cells.add(entity.getCell());
				}
			}
		}
		return cells;
	}
}
