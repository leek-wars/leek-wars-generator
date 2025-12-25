package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;

public class ActionRemoveEffect implements Action {

	private final int id;

	public ActionRemoveEffect(int id) {
		this.id = id;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.REMOVE_EFFECT);
		retour.add(id);
		return retour;
	}
}
