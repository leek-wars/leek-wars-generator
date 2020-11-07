package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionDamage;
import com.leekwars.generator.fight.action.DamageType;

public class EffectNovaDamage extends Effect {

	@Override
	public void apply(Fight fight) {

		// Base damages
		double d = (value1 + jet * value2) * (1 + Math.max(0, caster.getScience()) / 100.0) * power * criticalPower;

		value = (int) Math.round(d);

		if (value > target.getTotalLife() - target.getLife()) {
			value = target.getTotalLife() - target.getLife();
		}

		fight.log(new ActionDamage(DamageType.NOVA, target, value, 0));
		target.removeLife(0, value, caster, true);
		target.onNovaDamage(value);
	}
}
