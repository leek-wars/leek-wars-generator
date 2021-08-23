package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;

public class EffectStealAbsoluteShield extends Effect {

	@Override
	public void apply(Fight fight) {

		value = previousEffectTotalValue;
		if (value > 0) {
			stats.setStat(Entity.CHARAC_ABSOLUTE_SHIELD, value);
			target.updateBuffStats(Entity.CHARAC_ABSOLUTE_SHIELD, value, caster);
		}
	}
}
