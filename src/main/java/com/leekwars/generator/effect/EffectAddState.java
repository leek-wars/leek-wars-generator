package com.leekwars.generator.effect;

import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

public class EffectAddState extends Effect {


	@Override
	public void apply(State state) {

		this.state = EntityState.values()[(int) value1];
		target.addState(this.state);
	}
}