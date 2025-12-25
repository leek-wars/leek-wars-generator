package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;

public class ActionStartFight implements Action {

	int team1, team2;

	public ActionStartFight(int team1, int team2) {
		this.team1 = team1;
		this.team2 = team2;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.START_FIGHT);
		return retour;
	}

}
