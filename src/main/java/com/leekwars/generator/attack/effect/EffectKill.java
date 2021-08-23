package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionKill;
import com.leekwars.generator.fight.action.DamageType;

public class EffectKill extends Effect {

	@Override
	public void apply(Fight fight) {

		value = target.getLife();
		fight.log(new ActionKill(caster, target));
		target.removeLife(value, 0, caster, DamageType.DIRECT, this);
	}
}
