package com.leekwars.generator.effect;

import com.leekwars.generator.state.State;

public class EffectPermutation extends Effect {

	@Override
	public void apply(State fight) {

		fight.invertEntities(caster, target);
	}
}
