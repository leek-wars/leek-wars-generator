package com.leekwars.generator.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.state.Entity;

public class ActionRemovePoisons implements Action {

	private final int id;

	public ActionRemovePoisons(Entity target) {
		this.id = target.getFId();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.REMOVE_POISONS);
		retour.add(id);
		return retour;
	}
}