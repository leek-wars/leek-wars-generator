package com.leekwars.generator.state;

/**
 * Compact stat container. Indexed by Entity.STAT_* constants (range 0..17).
 *
 * Hot path: getStat is called on every damage / range / effect computation. The
 * previous TreeMap<Integer,Integer> implementation paid boxing + log-lookup at
 * every read. The int[] backing makes get/set zero-allocation and O(1).
 *
 * setMask tracks which stats have been explicitly set. forEach iterates only
 * those, preserving the TreeMap.entrySet() semantics needed by Effect.reduce /
 * Effect.mergeWith (an untouched stat is NOT visited, even though its value is
 * zero, because the old code distinguished "absent" from "set to 0").
 */
public class Stats {

	private static final int SIZE = 18;

	private final int[] values;
	private long setMask;

	public Stats() {
		this.values = new int[SIZE];
	}

	public Stats(Stats other) {
		this.values = other.values.clone();
		this.setMask = other.setMask;
	}

	public int getStat(int stat) {
		if (stat < 0 || stat >= SIZE) return 0;
		return values[stat];
	}

	public void setStat(int key, int value) {
		if (key < 0 || key >= SIZE) return;
		values[key] = value;
		setMask |= 1L << key;
	}

	public void updateStat(int id, int delta) {
		if (id < 0 || id >= SIZE) return;
		values[id] += delta;
		setMask |= 1L << id;
	}

	public void addStats(Stats toAdd) {
		long mask = toAdd.setMask;
		while (mask != 0) {
			int i = Long.numberOfTrailingZeros(mask);
			updateStat(i, toAdd.values[i]);
			mask &= mask - 1;
		}
	}

	public void clear() {
		java.util.Arrays.fill(values, 0);
		setMask = 0L;
	}

	@FunctionalInterface
	public interface StatVisitor {
		void visit(int statId, int value);
	}

	/** Visit every set stat in ascending id order. */
	public void forEach(StatVisitor visitor) {
		long mask = setMask;
		while (mask != 0) {
			int i = Long.numberOfTrailingZeros(mask);
			visitor.visit(i, values[i]);
			mask &= mask - 1;
		}
	}
}
