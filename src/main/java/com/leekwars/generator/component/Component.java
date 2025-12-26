package com.leekwars.generator.component;

import java.util.HashMap;

import tools.jackson.databind.node.ArrayNode;

public class Component {

	private final int id;
	private final String name;
	private final HashMap<String, Integer> stats = new HashMap<>();
	private final int template;

	public Component(int id, String name, ArrayNode stats, int template) {
		this.id = id;
		this.name = name;
		for (var stat : stats) {
			this.stats.put(((ArrayNode) stat).get(0).asString(), ((ArrayNode) stat).get(1).intValue());
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
