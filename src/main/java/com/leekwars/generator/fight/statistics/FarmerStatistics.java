package com.leekwars.generator.fight.statistics;

import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/*
 * Object to keep track of farmer statistics during the fight
 */
public class FarmerStatistics {

    public int teleportations = 0;
    public int summons = 0;
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
	public int tooMuchOperations = 0;
	public int stackOverflows = 0;
	public LeekSet weaponsUsed = new LeekSet();
	public LeekSet chipsUsed = new LeekSet();
	public LeekValue endCells = new LeekValue();
	public LeekValue endLifes = new LeekValue();
	public LeekValue totalLifes = new LeekValue();
	public LeekCellList walkedCells = new LeekCellList();
	public LeekValue aiInstructions = new LeekValue(); // Nombre d'instructions des IA
	public LeekValue aiOperations = new LeekValue(); // Nombre d'opérations consommées
	public LeekValue aiTimes = new LeekValue(); // Temps d'éxécution des IA

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

	public class LeekCellList extends HashMap<Integer, BitSet> {
		private static final long serialVersionUID = -949384035567270356L;
		public void set(int leek, int index) {
			if (!containsKey(leek)) {
				put(leek, new BitSet(613));
			}
			get(leek).set(index, true);
		}
		public JSONObject toJson() {
			JSONObject json = new JSONObject();
			for (Entry<Integer, BitSet> entry : entrySet()) {
				json.put(String.valueOf(entry.getKey()), entry.getValue().toString());
			}
			return json;
		}

		public JSONObject toDBJson() {
			JSONObject json = new JSONObject();

			for (Entry<Integer, BitSet> entry : entrySet()) {
				BitSet cells = entry.getValue();

				int start = cells.nextSetBit(0);

				BitSet copy = new BitSet(613);
				for (int i = 0; i < 613; ++i) {
					if (cells.get(i)) {
						copy.set(i - start);
					}
				}

				System.out.println("BitSet = " + cells.toString());
				// System.out.println("BiCopy = " + copy.toString());
				// byte[] bytearray = cells.toByteArray();
				// System.out.println("ByteArray = " + Arrays.toString(bytearray));
				byte[] bytearraycopy = copy.toByteArray();
				// System.out.println("ByteACopy = " + Arrays.toString(bytearraycopy));

				String base64 = new String(Base64.getEncoder().encode(bytearraycopy));
				base64 += "," + start;

				System.out.println(base64);
				json.put(String.valueOf(entry.getKey()), base64);
			}
			return json;
		}
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("teleporatations", teleportations);
		json.put("summons", summons);
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
		json.put("tooMuchOperations", tooMuchOperations);
		json.put("stackOverflows", stackOverflows);
		json.put("weaponsUsed", weaponsUsed.toJson());
		json.put("chipsUsed", chipsUsed.toJson());
		json.put("endCells", endCells.toJson());
		json.put("endLifes", endLifes.toJson());
		json.put("totalLifes", totalLifes.toJson());
		json.put("walkedCells", walkedCells.toJson());
		json.put("aiInstructions", aiInstructions.toJson());
		json.put("aiOperations", aiOperations.toJson());
		json.put("aiTimes", aiTimes.toJson());
		return json;
	}

	public JSONArray toDBJson() {
		JSONArray array = new JSONArray();
		array.add(teleportations); // 1
		array.add(summons); // 2
		array.add(weaponShot); // 7
		array.add(usedChips); // 8
		array.add(suicides); // 9
		array.add(kills); // 10
		array.add(kamikaze); // 11
		array.add(killedAllies); // 12
		array.add(healedEnemies); // 13
		array.add(maxHurtEnemies); // 14
		array.add(maxKilledEnemies); // 15
		array.add(walkedDistance); // 16
		array.add(damage); // 17
		array.add(tooMuchOperations); // 20
		array.add(stackOverflows); // 21
		array.add(weaponsUsed.toJson()); // 22
		array.add(chipsUsed.toJson()); // 23
		array.add(endCells.toJson()); // 24
		array.add(endLifes.toJson()); // 25
		array.add(totalLifes.toJson()); // 26
		array.add(walkedCells.toDBJson()); // 27
		array.add(aiInstructions.toJson()); // 28
		array.add(aiOperations.toJson()); // 29
		array.add(aiTimes.toJson()); // 30
		return array;
	}
}