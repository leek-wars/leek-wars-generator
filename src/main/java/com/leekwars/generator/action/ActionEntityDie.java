package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionEntityDie implements Action {

	private final int id;
	private final int killer;

	public ActionEntityDie(Entity leek, Entity killer) {
		this.id = leek.getFId();
		this.killer = killer != null ? killer.getFId() : -1;
	}

	@Override
	public ArrayNode getJSON() {

		ArrayNode retour = Json.createArray();
		retour.add(Action.PLAYER_DEAD);
		retour.add(id);
		if (killer != -1) {
			retour.add(killer);
		}
		return retour;
	}
}
