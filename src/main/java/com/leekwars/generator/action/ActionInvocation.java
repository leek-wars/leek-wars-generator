package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionInvocation implements Action {
	private final int target;
	private final int cell;
	private final int owner;
	private final int result;

	public ActionInvocation(Entity target, int result) {
		this.owner = target.getSummoner().getFId();
		this.target = target.getFId();
		this.cell = target.getCell().getId();
		this.result = result;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.SUMMON);
		retour.add(owner);
		retour.add(target);
		retour.add(cell);
		retour.add(result);
		return retour;
	}

}
