package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionEntityTurn implements Action {

	private final int id;

	public ActionEntityTurn(Entity leek) {
		if (leek == null)
			this.id = -1;
		else {
			this.id = leek.getFId();
		}
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.LEEK_TURN);
		retour.add(id);
		return retour;
	}
}
