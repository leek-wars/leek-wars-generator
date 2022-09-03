package com.leekwars.generator.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.effect.Effect;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.DamageType;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.statistics.StatisticsManager;
import com.leekwars.generator.maps.Cell;

public class LocalTrophyManager implements StatisticsManager {

	@Override
	public void say(Entity entity, String message) {}

	@Override
	public void teleportation(Entity entity, Entity caster, Cell start, Cell end) {}

	@Override
	public void lama(Entity entity) {}

	@Override
	public void characteristics(Entity entity) {}

	@Override
	public void tooMuchOperations(Entity entity) {
	}

	@Override
	public void stackOverflow(Entity entity) {}

	@Override
	public void damage(Entity entity, Entity attacker, int damage, DamageType type, Effect effect) {}

	@Override
	public void summon(Entity entity, Entity summon) {}

	@Override
	public void useTP(int tp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void heal(Entity healer, Entity entity, int pv) {
		// TODO Auto-generated method stub

	}

	@Override
	public void error(Entity mEntity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void useChip(Entity caster, Chip template, Cell cell, List<Entity> targets, Entity cellEntity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void useWeapon(Entity caster, Weapon weapon, Cell cell, List<Entity> targets, Entity cellEntity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void kill(Entity killer, Entity entity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void critical(Entity launcher) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endFight(Collection<Entity> values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTimes(Entity current, long l, long operations) {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(Entity mover, Entity entity, Cell start, List<Cell> path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resurrect(Entity caster, Entity target) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<Integer, Long> getOperationsByEntity() {
		return new TreeMap<Integer, Long>();
	}

	@Override
	public int getKills() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBullets() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getUsedChips() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getSummons() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDirectDamage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getHeal() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDistance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getStackOverflow() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getErrors() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getResurrects() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDamagePoison() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDamageReturn() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCriticalHits() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTPUsed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getMPUsed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getOperations() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getSays() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getSaysLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setGeneratorFight(Fight fight) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tooMuchDebug(int farmer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(Entity mEntity, int cell_id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void slide(Entity entity, Entity caster, Cell start, Cell cell) {
		// TODO Auto-generated method stub

	}

	@Override
	public void useInvalidPosition(Entity caster, Attack attack, Cell target) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCharacteristic(Entity entity, int characteristic, int delta, Entity caster) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(Entity entity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void effect(Entity entity, Entity caster, Effect effect) {
		// TODO Auto-generated method stub

	}

	@Override
	public void entityTurn(Entity entity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void antidote(Entity entity, Entity caster, int poisonsRemoved) {
		// TODO Auto-generated method stub

	}

	@Override
	public void vitality(Entity entity, Entity caster, int vitality) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerWrite(Entity entity, String key, String value) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setWeapon(Entity entity, Weapon w) {
		// TODO Auto-generated method stub
	}

	@Override
	public void chest() {
	}

	@Override
	public void chestKilled(Entity killer, Entity entity, Map<Integer, Integer> resources) {
		// TODO Auto-generated method stub
	}

	@Override
	public Map<Integer, Map<Integer, Integer>> getLeekResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getChests() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getChestsKills() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void chestKill() {
		// TODO Auto-generated method stub

	}
}
