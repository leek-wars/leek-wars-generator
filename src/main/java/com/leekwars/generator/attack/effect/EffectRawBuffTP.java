package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;

public class EffectRawBuffTP extends Effect {

	@Override
	public void apply(Fight fight) {
		value = (int) Math.round((value1 + value2 * jet) * targetCount);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_TP, value);
			target.updateBuffStats(Entity.CHARAC_TP, value);
		}
	}
}
