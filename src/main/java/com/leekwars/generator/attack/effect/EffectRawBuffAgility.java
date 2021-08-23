package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;

public class EffectRawBuffAgility extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_AGILITY, value);
			target.updateBuffStats(Entity.CHARAC_AGILITY, value, caster);
		}
	}
}
