package com.leekwars.generator.leek;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Stats {

	public final Map<Integer, Integer> stats;

	public Stats() {
		this.stats = new TreeMap<Integer, Integer>();
	}

	public int getStat(int stat) {
		Integer retour = stats.get(stat);
		if (retour == null)
			return 0;
		return retour;
	}

	public void addStats(Stats to_add) {
		for (Entry<Integer, Integer> entry : to_add.stats.entrySet()) {
			updateStat(entry.getKey(), entry.getValue());
		}
	}

	public void setStat(int key, int value) {
		stats.put(key, value);
	}

	public void clear() {
		stats.clear();
	}

	public void updateStat(int id, int delta) {
		stats.merge(id, delta, Integer::sum);
	}
}
