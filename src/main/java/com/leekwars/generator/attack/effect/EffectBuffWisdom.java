package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;

public class EffectBuffWisdom extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + value2 * jet) * (1 + (double) caster.getScience() / 100) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_WISDOM, value);
			target.updateBuffStats(Entity.CHARAC_WISDOM, value, caster);
		}
	}
}
