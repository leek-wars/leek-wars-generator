package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;

public class ActionNewTurn implements Action {

	private final int count;

	public ActionNewTurn(int count) {
		this.count = count;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.NEW_TURN);
		retour.add(count);
		return retour;
	}

}
