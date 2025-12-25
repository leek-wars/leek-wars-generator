package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionHeal;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

public class EffectStealLife extends Effect {

	@Override
	public void apply(State state) {

		if (target.hasState(EntityState.UNHEALABLE)) return;

		value = previousEffectTotalValue;
		if (value > 0) {

			if (target.getLife() + value > target.getTotalLife()) {
				value = target.getTotalLife() - target.getLife();
			}

			state.log(new ActionHeal(target, value));
			target.addLife(caster, value);
		}
	}
}
