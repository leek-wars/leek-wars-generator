package com.leekwars.generator.state;

/**
 * Compact stat container indexed by Entity.STAT_* (0..17). forEach visits only
 * non-zero stats — semantically a 0 stat is treated as absent.
 */
public class Stats {

	private static final int SIZE = 18; // Entity.STAT_RAM + 1

	private final int[] values;

	public Stats() {
		this.values = new int[SIZE];
	}

	public Stats(Stats other) {
		this.values = other.values.clone();
	}

	public int getStat(int stat) {
		if (stat < 0 || stat >= SIZE) return 0;
		return values[stat];
	}

	public void setStat(int key, int value) {
		// Bounds-checked because Entity.applyLoadout / EntityInfo can feed JSON-parsed
		// stat ids (cf. Entity.java:381, EntityInfo.java:177).
		if (key < 0 || key >= SIZE) return;
		values[key] = value;
	}

	public void updateStat(int id, int delta) {
		if (id < 0 || id >= SIZE) return;
		values[id] += delta;
	}

	public void addStats(Stats toAdd) {
		toAdd.forEach(this::updateStat);
	}

	public void clear() {
		java.util.Arrays.fill(values, 0);
	}

	public interface StatVisitor {
		void visit(int statId, int value);
	}

	/** Visit every non-zero stat in ascending id order. */
	public void forEach(StatVisitor visitor) {
		for (int i = 0; i < SIZE; i++) {
			if (values[i] != 0) visitor.visit(i, values[i]);
		}
	}
}
