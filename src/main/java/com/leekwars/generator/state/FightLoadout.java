package com.leekwars.generator.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FightLoadout {

	private final String name;
	private final List<Integer> weapons;
	private final List<Integer> forgottenWeapons;
	private final List<Integer> chips;
	private final Map<Integer, Integer> stats;

	public FightLoadout(String name, List<Integer> weapons, List<Integer> forgottenWeapons, List<Integer> chips, Map<Integer, Integer> stats) {
		this.name = name;
		this.weapons = weapons;
		this.forgottenWeapons = forgottenWeapons == null ? java.util.Collections.emptyList() : forgottenWeapons;
		this.chips = chips;
		this.stats = stats == null ? new HashMap<>() : stats;
	}

	public String getName() {
		return name;
	}

	public List<Integer> getWeapons() {
		return weapons;
	}

	/** Ordered list of forgotten-weapon candidates: at apply time, the first one not
	 * already claimed by a teammate of the same farmer wins (with stickiness on the
	 * weapon currently equipped if it appears in this list). */
	public List<Integer> getForgottenWeapons() {
		return forgottenWeapons;
	}

	public List<Integer> getChips() {
		return chips;
	}

	public Map<Integer, Integer> getStats() {
		return stats;
	}
}
