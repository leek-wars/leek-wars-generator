package com.leekwars.generator.fight.action;

import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.fight.entity.Entity;

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
	public JSONArray getJSON() {

		JSONArray retour = new JSONArray();
		retour.add(Action.CHEST_OPENED);
		retour.add(killer.getFId());
		retour.add(chest.getFId());

		var res = new JSONObject();
		for (var r : resources.entrySet()) {
			res.put(String.valueOf(r.getKey()), r.getValue());
		}
		retour.add(res);
		return retour;
	}
}
