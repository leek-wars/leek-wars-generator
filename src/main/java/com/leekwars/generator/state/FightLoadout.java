package com.leekwars.generator.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FightLoadout {

	private final String name;
	private final List<Integer> weapons;
	private final List<Integer> chips;
	private final Map<Integer, Integer> stats;

	public FightLoadout(String name, List<Integer> weapons, List<Integer> chips, Map<Integer, Integer> stats) {
		this.name = name;
		this.weapons = weapons;
		this.chips = chips;
		this.stats = stats == null ? new HashMap<>() : stats;
	}

	public String getName() {
		return name;
	}

	public List<Integer> getWeapons() {
		return weapons;
	}

	public List<Integer> getChips() {
		return chips;
	}

	public Map<Integer, Integer> getStats() {
		return stats;
	}
}
