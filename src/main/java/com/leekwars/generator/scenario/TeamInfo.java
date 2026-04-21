package com.leekwars.generator.scenario;

import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;

public class TeamInfo {
	public int id;
	public String name = "";
	public String compositionName = null;
	public int level;
	public String turretAIPath;
	public int turretAIOwner;

	public ObjectNode toJson() {
		ObjectNode json = Json.createObject();
		json.put("id", id);
		json.put("name", name);
		if (compositionName != null) {
			json.put("composition_name", compositionName);
		}
		json.put("level", level);
		return json;
	}
}