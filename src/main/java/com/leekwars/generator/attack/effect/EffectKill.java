package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionLoseLife;

public class EffectKill extends Effect {

	@Override
	public void apply(Fight fight) {

		value = target.getLife();
		fight.log(new ActionLoseLife(target, value, 0));
		target.removeLife(value, 0, caster, true);
	}
}
