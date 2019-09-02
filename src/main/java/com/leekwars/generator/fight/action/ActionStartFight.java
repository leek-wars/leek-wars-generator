package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;

public class ActionStartFight implements Action {

	int team1, team2;

	public ActionStartFight(int team1, int team2) {
		this.team1 = team1;
		this.team2 = team2;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.START_FIGHT);
		return retour;
	}

}
