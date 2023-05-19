package com.leekwars.generator.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.maps.Cell;

public class ActionUseWeapon implements Action {

	private final int cell;
	private final int success;

	public ActionUseWeapon(Cell cell, int success) {
		this.cell = cell.getId();
		this.success = success;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.USE_WEAPON);
		retour.add(cell);
		retour.add(success);
		return retour;
	}

}
