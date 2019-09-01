package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.entity.Entity;

public class EffectAbsoluteShield extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getResistance() / 100.0) * power * criticalPower);

		stats.setStat(Entity.CHARAC_ABSOLUTE_SHIELD, value);
		target.updateBuffStats(Entity.CHARAC_ABSOLUTE_SHIELD);
	}

	public void reduce() {
		value /= 2;
		stats.setStat(Entity.CHARAC_ABSOLUTE_SHIELD, value);
		target.updateBuffStats(Entity.CHARAC_ABSOLUTE_SHIELD);
	}
}
