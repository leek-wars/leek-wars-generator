package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionReduceEffects implements Action {

	private final int id;
	private final int value;

	public ActionReduceEffects(Entity target, int value) {
		this.id = target.getFId();
		this.value = value;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.REDUCE_EFFECTS);
		retour.add(id);
		retour.add(value);
		return retour;
	}
}