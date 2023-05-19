package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionHeal;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

public class EffectRawHeal extends Effect {

	@Override
	public void apply(State state) {

		if (target.hasState(EntityState.UNHEALABLE)) return;

		value = (int) Math.round((value1 + jet * value2) * aoe * criticalPower * targetCount);

		if (target.getLife() + value > target.getTotalLife()) {
			value = target.getTotalLife() - target.getLife();
		}
		state.log(new ActionHeal(target, value));
		target.addLife(caster, value);
	}
}
