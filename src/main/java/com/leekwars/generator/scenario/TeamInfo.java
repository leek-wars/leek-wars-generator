package com.leekwars.generator.scenario;

import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;

public class TeamInfo {
	public int id;
	public String name = "";
	public int level;
	public int turretAI;

	public ObjectNode toJson() {
		ObjectNode json = Json.createObject();
		json.put("id", id);
		json.put("name", name);
		json.put("level", level);
		return json;
	}
}