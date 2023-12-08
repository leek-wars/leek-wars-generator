package com.leekwars.generator.chips;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.items.Item;

public class Chip extends Item {

	private final int cooldown;
	private final boolean teamCooldown;
	private final int initialCooldown;
	private final int level;
	private final ChipType chipType; // Type in market

	public Chip(int id, int cost, int minRange, int maxRange, JSONArray effects, byte launchType, byte area, boolean los, int cooldown, boolean teamCooldown, int initialCooldown, int level, int template, String name, ChipType chipType) {
		super(id, cost, name, template, new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_CHIP, id));
		this.attack.setItem(this);

		this.cooldown = cooldown;
		this.teamCooldown = teamCooldown;
		this.initialCooldown = initialCooldown;
		this.level = level;
		this.chipType = chipType;
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

	public int getLevel() {
		return level;
	}

	public ChipType getChipType() {
		return chipType;
	}
}
