package com.leekwars.generator.fight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.turret.Turret;

public class Team {

	private int id;
	private final List<Entity> entities;
	private final TreeMap<Integer, Integer> cooldowns;
	private final HashSet<Integer> flags;

	public Team() {

		entities = new ArrayList<Entity>();
		cooldowns = new TreeMap<Integer, Integer>();
		flags = new HashSet<Integer>();
	}

	public Map<Integer, Integer> getCooldowns() {
		return cooldowns;
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public HashSet<Integer> getFlags() {
		return flags;
	}

	public void addFlag(int flag) {
		flags.add(flag);
	}

	public void setID(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	public void addEntity(Entity entity) {
		entities.add(entity);
	}

	public void removeEntity(Entity invoc) {
		entities.remove(invoc);
	}

	/*
	 * Are all entities in the team dead?
	 */
	public boolean isDead() {
		for (Entity entity : entities) {
			// The team is dead if the turret is dead
			if (entity.getType() == Entity.TYPE_TURRET && entity.isDead()) {
				return true;
			}
		}
		for (Entity entity : entities) {
			// The team is not dead if there is an alive leek
			if (entity.getType() != Entity.TYPE_TURRET && !entity.isDead()) {
				return false;
			}
		}
		return true;
	}

	/*
	 * !isDead()
	 */
	public boolean isAlive() {
		return !isDead();
	}

	public int size() {
		return entities.size();
	}

	// Add a team cooldown (for example for summons chips)
	public void addCooldown(Chip chip, int cooldown) {

		cooldowns.put(chip.getId(), cooldown == -1 ? Fight.MAX_TURNS + 2 : cooldown);
	}

	// Team has cooldown for this chip?
	public boolean hasCooldown(int chipID) {
		return cooldowns.containsKey(chipID);
	}

	// Get current cooldown for a chip
	public int getCooldown(int chipID) {
		if (!hasCooldown(chipID)) {
			return 0;
		}
		return cooldowns.get(chipID);
	}

	// Decrement cooldowns
	public void applyCoolDown() {
		Map<Integer, Integer> cooldown = new TreeMap<Integer, Integer>();
		cooldown.putAll(cooldowns);
		for (Entry<Integer, Integer> chip : cooldown.entrySet()) {
			if (chip.getValue() <= 1)
				cooldowns.remove(chip.getKey());
			else
				cooldowns.put(chip.getKey(), chip.getValue() - 1);
		}
	}

	public int getSummonCount() {
		int nb = 0;
		for (Entity e : entities) {
			if (!e.isDead() && e.isSummon()) {
				nb++;
			}
		}
		return nb;
	}

	public double getDeadRatio() {
		int dead = 0;
		int total = 0;
		for (Entity entity : entities) {
			if (entity.isSummon()) continue;
			total++;
			if (entity.isDead()) dead++;
		}
		return (double) dead / total;
	}

	public double getLifeRatio() {
		int life = 0;
		int total = 0;
		for (Entity entity : entities) {
			if (entity.isSummon()) continue;
			if (entity instanceof Turret) continue;
			total += entity.getTotalLife();
			life += entity.getLife();
		}
		return (double) life / total;
	}

	public boolean containsChest() {
		for (var entity : entities) {
			if (entity.getType() == Entity.TYPE_CHEST) return true;
		}
		return false;
	}

	public int getLife() {
		int life = 0;
		for (Entity entity : entities) {
			if (entity.isSummon()) continue;
			if (entity instanceof Turret) continue;
			life += entity.getLife();
		}
		return life;
	}
}
