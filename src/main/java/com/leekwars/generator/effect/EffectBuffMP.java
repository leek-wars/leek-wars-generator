package com.leekwars.generator.effect;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

public class EffectBuffMP extends Effect {

	@Override
	public void apply(State fight) {

		value = (int) Math.round((value1 + value2 * jet) * (1 + (double) caster.getScience() / 100) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_MP, value);
			target.updateBuffStats(Entity.CHARAC_MP, value, caster);
		}
	}
}
