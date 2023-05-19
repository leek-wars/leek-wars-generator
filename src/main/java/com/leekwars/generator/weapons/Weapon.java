package com.leekwars.generator.weapons;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.effect.EffectParameters;
import com.leekwars.generator.items.Item;

public class Weapon extends Item {

	private final List<EffectParameters> passiveEffects = new ArrayList<EffectParameters>();

	public Weapon(int id, int cost, int minRange, int maxRange, JSONArray effects, byte launchType, byte area, boolean los, int template, String name, JSONArray passiveEffects) {
		super(id, cost, name, template, new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_WEAPON, id));

		for (Object e : passiveEffects) {
			JSONObject effect = (JSONObject) e;
			int etype = effect.getIntValue("id");
			double value1 = effect.getDoubleValue("value1");
			double value2 = effect.getDoubleValue("value2");
			int turns = effect.getIntValue("turns");
			int targets = effect.getIntValue("targets");
			int modifiers = effect.getIntValue("modifiers");
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
