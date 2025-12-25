package com.leekwars.generator.scenario;

import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;

public class FarmerInfo {
	public int id;
	public String name;
	public String country;

	public ObjectNode toJson() {
		ObjectNode json = Json.createObject();
		json.put("id", id);
		json.put("name", name);
		json.put("country", country);
		return json;
	}
}