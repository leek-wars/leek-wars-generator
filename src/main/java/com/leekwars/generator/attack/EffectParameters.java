package com.leekwars.game.attack;

import com.leekwars.game.attack.effect.Effect;

public class EffectParameters {

	private final int id;
	private final double value1;
	private final double value2;
	private final int turns;
	private final int targets;
	private final boolean stackable;
	
	public EffectParameters(int id, double value1, double value2, int turns, int targets) {

		this.id = id;
		this.value1 = value1;
		this.value2 = value2;
		this.turns = turns;
		this.targets = targets;
		this.stackable = Effect.isStackable(id);
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

	public boolean isStackable() {
		return stackable;
	}
}
