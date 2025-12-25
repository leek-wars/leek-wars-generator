package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionRemovePoisons implements Action {

	private final int id;

	public ActionRemovePoisons(Entity target) {
		this.id = target.getFId();
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.REMOVE_POISONS);
		retour.add(id);
		return retour;
	}
}