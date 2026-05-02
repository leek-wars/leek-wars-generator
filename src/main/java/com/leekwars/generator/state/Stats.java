package com.leekwars.generator.state;

/**
 * Compact stat container indexed by Entity.STAT_* (0..17). forEach visits only
 * stats touched via set/update — untouched stats with implicit value 0 are
 * skipped, matching the "absent vs explicit zero" distinction expected by
 * Effect.reduce / Effect.mergeWith.
 */
public class Stats {

	private static final int SIZE = 18; // Entity.STAT_RAM + 1

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
		// Bounds-checked because Entity.applyLoadout / EntityInfo can feed JSON-parsed
		// stat ids (cf. Entity.java:381, EntityInfo.java:177).
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
		toAdd.forEach(this::updateStat);
	}

	public void clear() {
		java.util.Arrays.fill(values, 0);
		setMask = 0L;
	}

	public interface StatVisitor {
		void visit(int statId, int value);
	}

	/**
	 * Visit every set stat in ascending id order. Snapshot semantics: setMask is
	 * captured at entry, so stats marked set during the visit are not observed
	 * (callers can safely call updateStat from within the visitor).
	 */
	public void forEach(StatVisitor visitor) {
		long mask = setMask;
		while (mask != 0) {
			int i = Long.numberOfTrailingZeros(mask);
			visitor.visit(i, values[i]);
			mask &= mask - 1;
		}
	}
}
