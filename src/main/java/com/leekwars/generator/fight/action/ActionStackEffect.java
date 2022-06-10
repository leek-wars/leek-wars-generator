package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;

public class ActionStackEffect implements Action {

	private final int id;
	private final int value;

	public ActionStackEffect(int id, int value) {
		this.id = id;
		this.value = value;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.STACK_EFFECT);
		retour.add(id);
		retour.add(value);
		return retour;
	}
}
