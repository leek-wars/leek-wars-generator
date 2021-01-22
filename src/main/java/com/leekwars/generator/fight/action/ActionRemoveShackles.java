package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionRemoveShackles implements Action {

	private final int id;

	public ActionRemoveShackles(Entity target) {
		this.id = target.getFId();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.REMOVE_SHACKLES);
		retour.add(id);
		return retour;
	}
}