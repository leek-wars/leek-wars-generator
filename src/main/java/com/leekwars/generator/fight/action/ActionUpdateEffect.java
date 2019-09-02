package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;

public class ActionUpdateEffect implements Action {
	
	private final int id;
	private final int value;

	public ActionUpdateEffect(int id, int value) {
		this.id = id;
		this.value = value;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.UPDATE_EFFECT);
		retour.add(id);
		retour.add(value);
		return retour;
	}
}