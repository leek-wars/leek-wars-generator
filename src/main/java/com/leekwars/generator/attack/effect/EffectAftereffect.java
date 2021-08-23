package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionDamage;
import com.leekwars.generator.fight.action.DamageType;

public class EffectAftereffect extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + value2 * jet) * (1 + (double) caster.getScience() / 100) * aoe * criticalPower);
		value = Math.max(0, value);

		if (target.getLife() < value) {
			value = target.getLife();
		}
		int erosion = (int) Math.round(value * erosionRate);

		fight.log(new ActionDamage(DamageType.AFTEREFFECT, target, value, erosion));
		target.removeLife(value, erosion, caster, DamageType.AFTEREFFECT, this);
	}

	@Override
	public void applyStartTurn(Fight fight) {

		if (target.getLife() < value) {
			value = target.getLife();
		}
		int erosion = (int) Math.round(value * erosionRate);

		fight.log(new ActionDamage(DamageType.AFTEREFFECT, target, value, erosion));
		target.removeLife(value, erosion, caster, DamageType.AFTEREFFECT, this);
	}
}
