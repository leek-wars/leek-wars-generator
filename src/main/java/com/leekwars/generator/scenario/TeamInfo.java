package com.leekwars.generator.scenario;

import com.alibaba.fastjson.JSONObject;

public class TeamInfo {
	public int id;
	public String name;

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		return json;
	}
}