package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionRemovePoisons;
import com.leekwars.generator.state.State;

public class EffectAntidote extends Effect {

	@Override
	public void apply(State state) {

		target.clearPoisons(caster);

		// "Les poisons de X sont neutralisés"
		state.log(new ActionRemovePoisons(target));
	}
}
