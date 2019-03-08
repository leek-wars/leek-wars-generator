package com.leekwars.game.attack.weapons;

import com.leekwars.game.attack.Attack;

public class WeaponTemplate {

	private final int id;
	private final byte type;
	private final int cost;
	private final Attack attack;
	private final String name;
	private final int template;

	public WeaponTemplate(int id, byte type, int cost, int minRange, int maxRange, String effects, byte launchType, byte area, boolean los, int template, String name) {

		this.id = id;
		this.type = type;
		this.cost = cost;
		this.name = name;
		this.template = template;

		attack = new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_WEAPON, template);
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
