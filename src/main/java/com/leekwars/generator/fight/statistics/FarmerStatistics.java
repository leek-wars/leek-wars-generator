package com.leekwars.generator.fight.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/*
 * Object to keep track of farmer statistics during the fight
 */
public class FarmerStatistics {

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
	public LeekValue endLifes = new LeekValue();
	public LeekValue totalLifes = new LeekValue();
	public LeekCellList walkedCells = new LeekCellList();

    public class LeekSet extends HashMap<Integer, HashSet<Long>> {
		private static final long serialVersionUID = 404567813674607806L;
		public void add(int leek, long value) {
			if (!containsKey(leek)) {
				put(leek, new HashSet<Long>());
			}
			get(leek).add(value);
		}
		public int getCount() {
			int retour = 0;
			for (HashSet<Long> list : values()) {
				if (list.size() > retour)
					retour = list.size();
			}
			return retour;
		}
		public JSONObject toJson() {
			JSONObject json = new JSONObject();
			for (Entry<Integer, HashSet<Long>> set : entrySet()) {
				JSONArray array = new JSONArray();
				for (Long value : set.getValue()) {
					array.add(value);
				}
				json.put(String.valueOf(set.getKey()), array);
			}
			return json;
		}
	}

	public class LeekValue extends HashMap<Integer, Long> {
		private static final long serialVersionUID = -641938448092752857L;
		public void set(int leek, long value) {
			put(leek, value);
		}
		public JSONObject toJson() {
			JSONObject json = new JSONObject();
			for (Entry<Integer, Long> value : entrySet()) {
				json.put(String.valueOf(value.getKey()), value.getValue());
			}
			return json;
		}
		public void add(int id, long amount) {
			merge(id, amount, Long::sum);
		}
	}

	public class LeekCellList extends HashMap<Integer, List<Boolean>> {
		private static final long serialVersionUID = -949384035567270356L;
		public void set(int leek, int index) {
			if (!containsKey(leek)) {
				put(leek, new ArrayList<Boolean>(Collections.nCopies(613, false)));
			}
			get(leek).set(index, true);
		}
		public JSONObject toJson() {
			JSONObject json = new JSONObject();
			for (Entry<Integer, List<Boolean>> list : entrySet()) {
				JSONArray array = new JSONArray();
				for (Boolean value : list.getValue()) {
					array.add(value ? 1 : 0);
				}
				json.put(String.valueOf(list.getKey()), array);
			}
			return json;
		}
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("stashed", stashed);
		json.put("summons", summons);
		json.put("roxxor", roxxor);
		json.put("maxEntityLife", maxEntityLife);
		json.put("maxEntityTP", maxEntityTP);
		json.put("maxEntityMP", maxEntityMP);
		json.put("weaponShot", weaponShot);
		json.put("usedChips", usedChips);
		json.put("suicides", suicides);
		json.put("kills", kills);
		json.put("kamikaze", kamikaze);
		json.put("killedAllies", killedAllies);
		json.put("healedEnemies", healedEnemies);
		json.put("maxHurtEnemies", maxHurtEnemies);
		json.put("maxKilledEnemies", maxKilledEnemies);
		json.put("walkedDistance", walkedDistance);
		json.put("damage", damage);
		json.put("snipers", snipers);
		json.put("lamas", lamas);
		json.put("tooMuchOperations", tooMuchOperations);
		json.put("stackOverflows", stackOverflows);
		json.put("weaponsUsed", weaponsUsed.toJson());
		json.put("chipsUsed", chipsUsed.toJson());
		json.put("endCells", endCells.toJson());
		json.put("walkedCells", walkedCells.toJson());
		return json;
	}
}