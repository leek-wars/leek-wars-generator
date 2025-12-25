package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionRemoveShackles implements Action {

	private final int id;

	public ActionRemoveShackles(Entity target) {
		this.id = target.getFId();
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.REMOVE_SHACKLES);
		retour.add(id);
		return retour;
	}
}