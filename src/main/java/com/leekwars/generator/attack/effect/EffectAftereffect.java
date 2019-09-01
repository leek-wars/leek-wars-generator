package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionLoseLife;

public class EffectAftereffect extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + value2 * jet) * (1 + (double) caster.getScience() / 100) * power * criticalPower);
		value = Math.max(0, value);

		if (target.getLife() < value) {
			value = target.getLife();
		}
		int erosion = (int) Math.round(value * erosionRate);

		fight.log(new ActionLoseLife(target, value, erosion));
		target.removeLife(value, erosion, caster, false);
	}
}
