package com.leekwars.generator.items;

import com.leekwars.generator.attack.Attack;

public class Item {

	protected final int id;
	protected final int cost;
	protected final Attack attack;
	protected final String name;
	protected final int template;

	public Item(int id, int cost, String name, int template, Attack attack) {
		this.id = id;
		this.cost = cost;
		this.name = name;
		this.template = template;
		this.attack = attack;
	}

	public int getTemplate() {
		return template;
	}

	public int getId() {
		return id;
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
