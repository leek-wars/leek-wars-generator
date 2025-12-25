package com.leekwars.generator.weapons;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.effect.EffectParameters;
import com.leekwars.generator.items.Item;

public class Weapon extends Item {

	private final List<EffectParameters> passiveEffects = new ArrayList<EffectParameters>();

	public Weapon(int id, int cost, int minRange, int maxRange, ArrayNode effects, byte launchType, byte area, boolean los, int template, String name, ArrayNode passiveEffects, int maxUses) {
		super(id, cost, name, template, new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_WEAPON, id, maxUses));
		this.attack.setItem(this);

		for (var e : passiveEffects) {
			ObjectNode effect = (ObjectNode) e;
			int etype = effect.get("id").intValue();
			double value1 = effect.get("value1").doubleValue();
			double value2 = effect.get("value2").doubleValue();
			int turns = effect.get("turns").intValue();
			int targets = effect.get("targets").intValue();
			int modifiers = effect.get("modifiers").intValue();
			this.passiveEffects.add(new EffectParameters(etype, value1, value2, turns, targets, modifiers));
		}
	}

	public int getId() {
		return id;
	}

	public int getTemplate() {
		return template;
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

	public List<EffectParameters> getPassiveEffects() {
		return passiveEffects;
	}

	public boolean isHandToHandWeapon() {
		return attack.getMinRange() == 1 && attack.getMaxRange() == 1;
	}

	@Override
	public String toString() {
		return name;
	}
}
