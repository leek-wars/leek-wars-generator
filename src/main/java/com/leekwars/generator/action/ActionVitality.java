package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionVitality implements Action {

	private final int target;
	private final int life;

	public ActionVitality(Entity target, int life) {
		this.target = target.getFId();
		this.life = life;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.VITALITY);
		retour.add(target);
		retour.add(life);
		return retour;
	}
}
