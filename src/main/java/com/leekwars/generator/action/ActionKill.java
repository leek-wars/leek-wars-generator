package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionKill implements Action {

	private final int caster;
	private final int target;

	public ActionKill(Entity caster, Entity target) {
		this.caster = target.getFId();
		this.target = target.getFId();
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.KILL);
		retour.add(caster);
		retour.add(target);
		return retour;
	}
}
