package com.leekwars.generator.attack;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.GenericArrayLeekValue;

public class EffectParameters {

	private final int id;
	private final double value1;
	private final double value2;
	private final int turns;
	private final int targets;
	private final int modifiers;

	public EffectParameters(int id, double value1, double value2, int turns, int targets, int modifiers) {

		this.id = id;
		this.value1 = value1;
		this.value2 = value2;
		this.turns = turns;
		this.targets = targets;
		this.modifiers = modifiers;
	}

	public int getId() {
		return id;
	}

	public double getValue1() {
		return value1;
	}

	public double getValue2() {
		return value2;
	}

	public int getTurns() {
		return turns;
	}

	public int getTargets() {
		return targets;
	}

	public int getModifiers() {
		return modifiers;
	}

	public GenericArrayLeekValue getFeatureArray(AI ai) throws LeekRunException {
		var effect = ai.newArray();
		effect.push(ai, (long) this.getId());
		effect.push(ai, this.getValue1());
		effect.push(ai, this.getValue1() + this.getValue2());
		effect.push(ai, (long) this.getTurns());
		effect.push(ai, (long) this.getTargets());
		effect.push(ai, (long) this.getModifiers());
		return effect;
	}
}
