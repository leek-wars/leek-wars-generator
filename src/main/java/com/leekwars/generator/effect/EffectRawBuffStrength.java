package com.leekwars.generator.effect;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

public class EffectRawBuffStrength extends Effect {

	@Override
	public void apply(State state) {

		value = (int) Math.round((value1 + jet * value2) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_STRENGTH, value);
			target.updateBuffStats(Entity.CHARAC_STRENGTH, value, caster);
		}
	}
}
