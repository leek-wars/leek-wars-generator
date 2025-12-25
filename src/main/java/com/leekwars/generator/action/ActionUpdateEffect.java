package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;

public class ActionUpdateEffect implements Action {

	private final int id;
	private final int value;

	public ActionUpdateEffect(int id, int value) {
		this.id = id;
		this.value = value;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.UPDATE_EFFECT);
		retour.add(id);
		retour.add(value);
		return retour;
	}
}