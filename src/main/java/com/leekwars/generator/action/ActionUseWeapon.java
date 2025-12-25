package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.maps.Cell;

public class ActionUseWeapon implements Action {

	private final int cell;
	private final int success;

	public ActionUseWeapon(Cell cell, int success) {
		this.cell = cell.getId();
		this.success = success;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.USE_WEAPON);
		retour.add(cell);
		retour.add(success);
		return retour;
	}

}
