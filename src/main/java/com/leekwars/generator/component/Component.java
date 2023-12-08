package com.leekwars.generator.component;

import java.util.HashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class Component {

	private final int id;
	private final String name;
	private final HashMap<String, Integer> stats = new HashMap<>();
	private final int template;

	public Component(int id, String name, String stats, int template) {
		this.id = id;
		this.name = name;
		for (var stat : JSON.parseArray(stats)) {
			this.stats.put(((JSONArray) stat).getString(0), ((JSONArray) stat).getInteger(1));
		}
		// System.out.println(this.stats);
		this.template = template;
	}

	public int getId() {
		return id;
	}
	public int getTemplate() {
		return template;
	}
	public HashMap<String, Integer> getStats() {
		return stats;
	}
	public String getName() {
		return name;
	}
}
