package com.leekwars.generator.attack.weapons;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.attack.Attack;

public class Weapon {

	private final int id;
	private final byte type;
	private final int cost;
	private final Attack attack;
	private final String name;
	private final int template;

	public Weapon(int id, byte type, int cost, int minRange, int maxRange, JSONArray effects, byte launchType, byte area, boolean los, int template, String name) {

		this.id = id;
		this.type = type;
		this.cost = cost;
		this.name = name;
		this.template = template;

		attack = new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_WEAPON, id);
	}

	public int getId() {
		return id;
	}

	public int getTemplate() {
		return template;
	}

	public byte getType() {
		return type;
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
}
