package com.leekwars.generator.action;

import com.alibaba.fastjson.JSONArray;

public class ActionLama implements Action {

	public ActionLama() {}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.LAMA);
		return retour;
	}
}
