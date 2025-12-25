package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionEndTurn implements Action {

	private final int target;
	private final int pt;
	private final int pm;

	public ActionEndTurn(Entity target) {

		this.target = target.getFId();
		this.pt = target.getTP();
		this.pm = target.getMP();
	}

	@Override
	public ArrayNode getJSON() {

		ArrayNode json = Json.createArray();
		json.add(Action.END_TURN);
		json.add(target);
		json.add(pt);
		json.add(pm);
		return json;
	}
}
