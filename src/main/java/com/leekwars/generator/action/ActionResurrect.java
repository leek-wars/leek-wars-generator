package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionResurrect implements Action {
	private final int target;
	private final int cell;
	private final int owner;
	private final int life;
	private final int max_life;

	public ActionResurrect(Entity owner, Entity target) {
		this.owner = owner.getFId();
		this.target = target.getFId();
		this.cell = target.getCell().getId();
		this.life = target.getLife();
		this.max_life = target.getTotalLife();
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.RESURRECT);
		retour.add(owner);
		retour.add(target);
		retour.add(cell);
		retour.add(life);
		retour.add(max_life);
		return retour;
	}

}
