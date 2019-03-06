package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.entity.Entity;

public class EffectBuffStrength extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + value2 * jet) * (1 + (double) caster.getScience() / 100) * power * criticalPower);

		stats.setStat(Entity.CHARAC_STRENGTH, value);
		target.updateBuffStats(Entity.CHARAC_STRENGTH);
	}
	
	public void reduce() {
		value /= 2;
		stats.setStat(Entity.CHARAC_STRENGTH, value);
		target.updateBuffStats(Entity.CHARAC_STRENGTH);
	}
}
