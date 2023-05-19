package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionVitality;
import com.leekwars.generator.state.State;

public class EffectVitality extends Effect {

	@Override
	public void apply(State state) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getWisdom() / 100.0) * aoe * criticalPower);

		state.log(new ActionVitality(target, value));
		target.addTotalLife(value, caster);
		target.addLife(caster, value);
	}
}
