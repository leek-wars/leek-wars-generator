package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionReduceEffects;
import com.leekwars.generator.state.State;

public class EffectDebuff extends Effect {

	@Override
	public void apply(State state) {
		value = (int) ((value1 + jet * value2) * aoe * criticalPower * targetCount);
		target.reduceEffects((double) value / 100, caster);

		// "Les effets de X sont réduits de Y%"
		state.log(new ActionReduceEffects(target, value));
	}
}
