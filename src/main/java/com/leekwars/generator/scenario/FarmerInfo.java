package com.leekwars.generator.scenario;

import com.alibaba.fastjson.JSONObject;

public class FarmerInfo {
	public int id;
	public String name;
	public String country;

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		json.put("country", country);
		return json;
	}
}