package com.leekwars.generator.fight.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.effect.Effect;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.maps.Cell;

public class FightStatistics {

	/*
	 * Global fight statistics
	 */
	private int sKills = 0;
	private int sBullets = 0;
	private int sUsedChips = 0;
	private int sSummons = 0;
	private long sDammages = 0;
	private long sHeal = 0;
	private long sDistance = 0;
	private int sStackOverflow = 0;
	private int sErrors = 0;
	private int sResurrects = 0;
	private long sDamagePoison = 0;
	private long sDamageReturn = 0;
	private int sCriticalHits = 0;
	private int sTPUsed = 0;
	private int sMPUsed = 0;
	private long sOperations = 0;
	private int sSays = 0;
	private long sSaysLength = 0;

	/*
	 * Statistics per farmer
	 */
	public final Map<Integer, FarmerStatistics> farmers;

	public FightStatistics() {
		farmers = new TreeMap<Integer, FarmerStatistics>();
	}

	public void initializeEntities(Collection<Entity> entities) {
		for (Entity entity : entities) {
			if (!this.farmers.containsKey(entity.getFarmer())) {
				this.farmers.put(entity.getFarmer(), new FarmerStatistics());
			}
			if (entity.getAI() != null) {
				this.farmers.get(entity.getFarmer()).aiInstructions.set(entity.getFId(), entity.getAI().getInstructions());
			}
		}
	}

	public void addStackOverflow(Entity entity) {
		sStackOverflow++;
		this.farmers.get(entity.getFarmer()).stackOverflows++;
	}

	public int getStackOverflow() {
		return sStackOverflow;
	}

	public void addDistance(int n) {
		sDistance += n;
	}

	public long getDistance() {
		return sDistance;
	}

	public void addHeal(int n) {
		sHeal += n;
	}

	public long getHeal() {
		return sHeal;
	}

	public long getDamage() {
		return sDammages;
	}

	public void summon(Entity caster, Entity summon) {
		sSummons += 1;
		farmers.get(caster.getFarmer()).summons++;
	}

	public int getSummons() {
		return sSummons;
	}

	public int getUsedChips() {
		return sUsedChips;
	}

	public int getBullets() {
		return sBullets;
	}

	public void addKills(int n) {
		sKills += n;
	}

	public int getKills() {
		return sKills;
	}

	public void addErrors(int errors) {
		sErrors += errors;
	}

	public int getErrors() {
		return sErrors;
	}

	public long getDamagePoison() {
		return sDamagePoison;
	}

	public void addDamagePoison(long damagePoison) {
		this.sDamagePoison += damagePoison;
	}

	public int getResurrects() {
		return sResurrects;
	}

	public void addResurrects(int resurrects) {
		this.sResurrects += resurrects;
	}

	public long getDamageReturn() {
		return sDamageReturn;
	}

	public void addDamageReturn(long damageReturn) {
		this.sDamageReturn += damageReturn;
	}

	public int getTPUsed() {
		return sTPUsed;
	}

	public void addTPUsed(int TPUsed) {
		this.sTPUsed += TPUsed;
	}

	public int getCriticalHits() {
		return sCriticalHits;
	}

	public void addCriticalHits(int criticalHits) {
		this.sCriticalHits += criticalHits;
	}

	public int getMPUsed() {
		return sMPUsed;
	}

	public void addMPUsed(int MPUsed) {
		this.sMPUsed += MPUsed;
	}

	public long getOperations() {
		return sOperations;
	}

	public void addOperations(long operations) {
		this.sOperations += operations;
	}

	public int getSays() {
		return sSays;
	}

	public void addSays(int says) {
		this.sSays += says;
	}

	public long getSaysLength() {
		return sSaysLength;
	}

	public void addSaysLength(long saysLength) {
		this.sSaysLength += saysLength;
	}

	public void teleportation(Entity caster) {
		this.farmers.get(caster.getFarmer()).teleportations++;
	}

	public void entityMove(Entity entity, Cell cell) {
		List<Cell> list = new ArrayList<Cell>();
		list.add(cell);
		entityMove(entity, list);
	}

	public void entityMove(Entity entity, List<Cell> path) {
		sDistance += path.size();
		FarmerStatistics stats = this.farmers.get(entity.getFarmer());
		stats.walkedDistance += path.size();
		// Save walked cells
		for (Cell c : path) {
			stats.walkedCells.set(entity.getFId(), c.getId());
		}
	}

	public void chipUsed(Entity caster, Chip chip, List<Entity> targets) {
		sUsedChips++;
		FarmerStatistics stats = this.farmers.get(caster.getFarmer());
		stats.usedChips++;
		stats.chipsUsed.add(caster.getFId(), chip.getId());
		attackUsed(caster, targets, chip.getAttack());
	}

	public void weaponUsed(Entity launcher, Weapon weapon, List<Entity> targets) {
		sBullets++;
		FarmerStatistics stats = this.farmers.get(launcher.getFarmer());
		stats.weaponShot++;
		stats.weaponsUsed.add(launcher.getId(), weapon.getId());
		attackUsed(launcher, targets, weapon.getAttack());
	}

	private void attackUsed(Entity caster, List<Entity> targets, Attack attack) {

		FarmerStatistics stats = this.farmers.get(caster.getFarmer());

		int hurt_enemies = 0;
		int healed_enemies = 0;
		int killed_enemies = 0;

		for (Entity target : targets) {
			if (target.getTeam() != caster.getTeam()) { // Ennemi
				if (target.isDead()) {
					killed_enemies++;
				}
				if (attack.isDamageAttack(Effect.TARGET_ENEMIES)) {
					hurt_enemies++;
				}
				if (attack.isHealAttack(Effect.TARGET_ENEMIES) && !attack.isDamageAttack(Effect.TARGET_ENEMIES)) {
					healed_enemies++;
				}
			}
		}
		// Kamikaze ?
		if (caster.isDead() && killed_enemies > 0) {
			stats.kamikaze++;
		}
		// Soigner un ennemi
		if (healed_enemies > 0) {
			stats.healedEnemies++;
		}
		// Toucher plusieurs ennemis
		if (hurt_enemies > stats.maxHurtEnemies) {
			stats.maxHurtEnemies = hurt_enemies;
		}
		// Tuer plusieurs ennemis
		if (killed_enemies > stats.maxKilledEnemies) {
			stats.maxKilledEnemies = killed_enemies;
		}
	}

	public void entityDied(Entity dead, Entity killer) {

		FarmerStatistics stats = this.farmers.get(killer.getFarmer());

		// Le mec s'est suicidé avec son attaque ?
		if (dead == killer) {
			stats.suicides++;
		}

		// Compteur de kills
		stats.kills++;

		// Tuer un allié
		if (dead.getTeam() == killer.getTeam()) {
			stats.killedAllies++;
		}
	}

	public void endFight(Collection<Entity> entities) {
		// Save end cells of each entity
		for (Entity entity : entities) {
			FarmerStatistics farmer = this.farmers.get(entity.getFarmer());
			if (entity.getCell() != null) {
				farmer.endCells.set(entity.getId(), entity.getCell().getId());
			}
			farmer.endLifes.set(entity.getId(), entity.getLife());
			farmer.totalLifes.set(entity.getId(), entity.getTotalLife());
		}
	}

	public void addDamage(Entity attacker, int amount, boolean isEnemy) {
		sDammages += amount;
		if (isEnemy) {
			this.farmers.get(attacker.getFarmer()).damage += amount;
		}
	}

	public void tooMuchOperations(Entity entity) {
		this.farmers.get(entity.getFarmer()).tooMuchOperations++;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		JSONObject global = new JSONObject();
		global.put("kills", sKills);
		global.put("bullets", sBullets);
		global.put("used_chips", sUsedChips);
		global.put("summons", sSummons);
		global.put("damage", sDammages);
		global.put("heal", sHeal);
		global.put("distance", sDistance);
		global.put("stack_overflows", sStackOverflow);
		global.put("errors", sErrors);
		global.put("resurrects", sResurrects);
		global.put("damage_poison", sDamagePoison);
		global.put("damage_return", sDamageReturn);
		global.put("critical_hits", sCriticalHits);
		global.put("tp_used", sTPUsed);
		global.put("mp_used", sMPUsed);
		global.put("operations", sOperations);
		global.put("says", sSays);
		global.put("says_length", sSaysLength);
		json.put("global", global);
		JSONObject farmers = new JSONObject();
		for (Entry<Integer, FarmerStatistics> farmer : this.farmers.entrySet()) {
			farmers.put(String.valueOf(farmer.getKey()), farmer.getValue().toJson());
		}
		json.put("farmers", farmers);
		return json;
	}

	public JSONArray toDBJson() {
		JSONArray array = new JSONArray();
		JSONObject farmers = new JSONObject();
		for (Entry<Integer, FarmerStatistics> farmer : this.farmers.entrySet()) {
			farmers.put(String.valueOf(farmer.getKey()), farmer.getValue().toDBJson());
		}
		array.add(farmers);
		array.add(sKills);
		array.add(sBullets);
		array.add(sUsedChips);
		array.add(sSummons);
		array.add(sDammages);
		array.add(sHeal);
		array.add(sDistance);
		array.add(sStackOverflow);
		array.add(sErrors);
		array.add(sResurrects);
		array.add(sDamagePoison);
		array.add(sDamageReturn);
		array.add(sCriticalHits);
		array.add(sTPUsed);
		array.add(sMPUsed);
		array.add(sOperations);
		array.add(sSays);
		array.add(sSaysLength);
		return array;
	}

	public void addTimes(Entity entity, long time, long operations) {

		FarmerStatistics farmer = this.farmers.get(entity.getFarmer());

		int owner = entity.isSummon() ? entity.getSummoner().getFId() : entity.getFId();

		farmer.aiOperations.add(owner, operations);
		farmer.aiTimes.add(owner, time);
	}
}
