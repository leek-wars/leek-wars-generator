package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.entity.Entity;

public class EffectBuffResistance extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + value2 * jet) * (1 + (double) caster.getScience() / 100) * power * criticalPower);

		stats.setStat(Entity.CHARAC_RESISTANCE, value);
		target.updateBuffStats(Entity.CHARAC_RESISTANCE);
	}
	
	public void reduce() {
		value /= 2;
		stats.setStat(Entity.CHARAC_RESISTANCE, value);
		target.updateBuffStats(Entity.CHARAC_RESISTANCE);
	}
}
