package com.leekwars.generator.effect;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

public class EffectShackleStrength extends Effect {

	@Override
	public void apply(State state) {

		// Base shackle : base × (1 + magic / 100)
		value = (int) Math.round((value1 + jet * value2) * (1.0 + Math.max(0, caster.getMagic()) / 100.0) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_STRENGTH, -value);
			target.updateBuffStats(Entity.CHARAC_STRENGTH, -value, caster);
		}
	}
}
