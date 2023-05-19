package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionKill;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.state.State;

public class EffectKill extends Effect {

	@Override
	public void apply(State state) {

		value = target.getLife();
		state.log(new ActionKill(caster, target));
		target.removeLife(value, 0, caster, DamageType.DIRECT, this);
	}
}
