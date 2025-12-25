package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;

public class ActionStackEffect implements Action {

	private final int id;
	private final int value;

	public ActionStackEffect(int id, int value) {
		this.id = id;
		this.value = value;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.STACK_EFFECT);
		retour.add(id);
		retour.add(value);
		return retour;
	}
}
