package com.leekwars.generator.fight.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/*
 * Object to keep track of farmer statistics during the fight
 */
class FarmerStatistics {

    public int stashed = 0;
    public int summons = 0;
    public int roxxor = 0;
    public int maxEntityLife = 0;
    public int maxEntityTP = 0;
    public int maxEntityMP = 0;
	public int weaponShot = 0;
	public int usedChips = 0;
	public int suicides = 0;
	public int kills = 0;
	public int kamikaze = 0;
	public int killedAllies = 0;
	public int healedEnemies = 0;
	public int maxHurtEnemies = 0;
	public int maxKilledEnemies = 0;
	public int walkedDistance = 0;
	public long damage = 0;
	public int snipers = 0;
	public int lamas = 0;
	public int tooMuchOperations = 0;
	public int stackOverflows = 0;
	public LeekSet weaponsUsed = new LeekSet();
	public LeekSet chipsUsed = new LeekSet();
	public LeekValue endCells = new LeekValue();
	public LeekCellList walkedCells = new LeekCellList();

    public class LeekSet {
		private final Map<Integer, HashSet<Long>> data;

		public LeekSet() {
			data = new HashMap<Integer, HashSet<Long>>();
		}
		public void add(int leek, long value) {
			if (!data.containsKey(leek)) {
				data.put(leek, new HashSet<Long>());
			}
			data.get(leek).add(value);
		}
		public int getCount() {
			int retour = 0;
			for (HashSet<Long> list : data.values()) {
				if (list.size() > retour)
					retour = list.size();
			}
			return retour;
		}
	}

	public class LeekValue {
		private final Map<Integer, Long> data = new HashMap<Integer, Long>();
		public void set(int leek, long value) {
			data.put(leek, value);
		}
	}

	public class LeekCellList {
		private final Map<Integer, List<Boolean>> data;

		public LeekCellList() {
			data = new HashMap<Integer, List<Boolean>>();
		}
		public void set(int leek, int index) {
			if (!data.containsKey(leek)) {
				data.put(leek, new ArrayList<Boolean>(Collections.nCopies(613, false)));
			}
			data.get(leek).set(index, true);
		}
	}
}