package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionRemoveShackles;
import com.leekwars.generator.state.State;

public class EffectRemoveShackles extends Effect {

	@Override
	public void apply(State state) {
		target.removeShackles();

		// "Les entraves de X sont retirées"
		state.log(new ActionRemoveShackles(target));
	}
}
