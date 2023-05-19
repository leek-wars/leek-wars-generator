package com.leekwars.generator.action;

import com.alibaba.fastjson.JSONArray;

public class ActionSay implements Action {

	private final String message;

	public ActionSay(String message) {
		this.message = message;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.SAY);
		retour.add(message.replaceAll("\t", "    "));
		return retour;
	}
}
