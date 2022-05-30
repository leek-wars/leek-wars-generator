package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionHeal;

public class EffectRawHeal extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * aoe * criticalPower * targetCount);

		if (target.getLife() + value > target.getTotalLife()) {
			value = target.getTotalLife() - target.getLife();
		}
		fight.log(new ActionHeal(target, value));
		target.addLife(caster, value);
	}
}
