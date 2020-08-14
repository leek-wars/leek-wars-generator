package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;

public class EffectDebuff extends Effect {

	@Override
	public void apply(Fight fight) {
		value = (int) ((value1 + jet * value2) * power * criticalPower * targetCount);
		target.reduceEffects((double) value / 100);
	}
}
