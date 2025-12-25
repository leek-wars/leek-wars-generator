package com.leekwars.generator.action;

import java.util.Map;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.state.Entity;

public class ActionChestOpened implements Action {

	private final Entity killer;
	private final Entity chest;
	private Map<Integer, Integer> resources;

	public ActionChestOpened(Entity killer, Entity chest, Map<Integer, Integer> resources) {
		this.killer = killer;
		this.chest = chest;
		this.resources = resources;
	}

	@Override
	public ArrayNode getJSON() {

		ArrayNode retour = Json.createArray();
		retour.add(Action.CHEST_OPENED);
		retour.add(killer.getFId());
		retour.add(chest.getFId());

		var res = Json.createObject();
		for (var r : resources.entrySet()) {
			res.put(String.valueOf(r.getKey()), r.getValue());
		}
		retour.add(res);
		return retour;
	}
}
