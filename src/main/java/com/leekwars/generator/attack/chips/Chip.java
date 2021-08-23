package com.leekwars.generator.attack.chips;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.attack.Attack;

public class Chip {

	private final int id;
	private final int cost;
	private final Attack attack;
	private final int cooldown;
	private final boolean teamCooldown;
	private final int initialCooldown;
	private final int template;
	private final String name;
	private final int level;
	private final ChipType chipType; // Type in market

	public Chip(int id, int cost, int minRange, int maxRange, JSONArray effects, byte launchType, byte area, boolean los, int cooldown, boolean teamCooldown, int initialCooldown, int level, int template, String name, ChipType chipType) {

		this.id = id;
		this.cost = cost;
		this.cooldown = cooldown;
		this.teamCooldown = teamCooldown;
		this.initialCooldown = initialCooldown;
		this.template = template;
		this.name = name;
		this.level = level;
		this.chipType = chipType;
		attack = new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_CHIP, id);
	}

	public int getTemplate() {
		return template;
	}

	public int getId() {
		return id;
	}

	public int getCooldown() {
		return cooldown;
	}

	public boolean isTeamCooldown() {
		return teamCooldown;
	}

	public int getInitialCooldown() {
		return initialCooldown;
	}

	public int getCost() {
		return cost;
	}

	public Attack getAttack() {
		return attack;
	}

	public String getName() {
		return name;
	}

	public int getLevel() {
		return level;
	}

	public ChipType getChipType() {
		return chipType;
	}
}
