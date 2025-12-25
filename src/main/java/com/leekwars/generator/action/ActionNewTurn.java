package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;

public class ActionNewTurn implements Action {

	private final int count;

	public ActionNewTurn(int count) {
		this.count = count;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.NEW_TURN);
		retour.add(count);
		return retour;
	}

}
