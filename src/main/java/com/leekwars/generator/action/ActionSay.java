package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;

public class ActionSay implements Action {

	private final String message;

	public ActionSay(String message) {
		this.message = message;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.SAY);
		retour.add(message.replaceAll("\t", "    "));
		return retour;
	}
}
