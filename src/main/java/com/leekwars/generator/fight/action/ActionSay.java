package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionSay implements Action {

	private final int leek;
	private final String message;

	public ActionSay(Entity leek, String message) {
		this.leek = leek.getFId();
		this.message = message;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.SAY);
		retour.add(leek);
		retour.add(message.replaceAll("\t", "    "));
		return retour;
	}
}
