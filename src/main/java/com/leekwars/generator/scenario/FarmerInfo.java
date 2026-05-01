package com.leekwars.generator.scenario;

import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;

public class FarmerInfo {
	public int id;
	public String name;
	public String country;
	/** Nombre de potions de restat possédées au début du combat — utilisé par setLoadout(). */
	public int restatPotions;

	public ObjectNode toJson() {
		ObjectNode json = Json.createObject();
		json.put("id", id);
		json.put("name", name);
		json.put("country", country);
		json.put("restat_potions", restatPotions);
		return json;
	}
}