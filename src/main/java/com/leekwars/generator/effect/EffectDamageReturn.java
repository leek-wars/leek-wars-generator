package com.leekwars.generator.effect;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

public class EffectDamageReturn extends Effect {

	@Override
	public void apply(State state) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getAgility() / 100.0) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_DAMAGE_RETURN, value);
			target.updateBuffStats(Entity.CHARAC_DAMAGE_RETURN, value, caster);
		}
	}
}
