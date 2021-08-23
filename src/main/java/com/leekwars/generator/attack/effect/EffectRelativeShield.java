package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;

public class EffectRelativeShield extends Effect {

	@Override
	public void apply(Fight fight) {
		value = (int) Math.round((value1 + jet * value2) * (1 + (double) caster.getResistance() / 100) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.CHARAC_RELATIVE_SHIELD, value);
			target.updateBuffStats(Entity.CHARAC_RELATIVE_SHIELD, value, caster);
		}
	}
}
