package com.leekwars.generator.effect;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

public class EffectRelativeShield extends Effect {

	@Override
	public void apply(State state) {
		value = (int) Math.round((value1 + jet * value2) * (1 + (double) caster.getResistance() / 100) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_RELATIVE_SHIELD, value);
			target.updateBuffStats(Entity.CHARAC_RELATIVE_SHIELD, value, caster);
		}
	}
}
