package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;

public class ActionRemoveEffect implements Action {

	private final int id;

	public ActionRemoveEffect(int id) {
		this.id = id;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.REMOVE_EFFECT);
		retour.add(id);
		return retour;
	}
}
