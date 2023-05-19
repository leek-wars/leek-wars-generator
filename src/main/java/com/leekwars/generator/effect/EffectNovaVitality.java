package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionNovaVitality;
import com.leekwars.generator.state.State;

public class EffectNovaVitality extends Effect {

	@Override
	public void apply(State state) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getScience() / 100.0) * aoe * criticalPower);

		state.log(new ActionNovaVitality(target, value));
		target.addTotalLife(value, caster);
	}
}
