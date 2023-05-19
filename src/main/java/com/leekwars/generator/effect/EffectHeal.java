package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionHeal;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

public class EffectHeal extends Effect {

	@Override
	public void apply(State state) {

		value = (int) Math.round((value1 + jet * value2) * (1 + (double) caster.getWisdom() / 100) * aoe * criticalPower * targetCount);

		value = Math.max(0, value); // Soin negatif si la sagesse est negative

		if (turns == 0) {

			if (target.hasState(EntityState.UNHEALABLE)) return;

			if (target.getLife() + value > target.getTotalLife()) {
				value = target.getTotalLife() - target.getLife();
			}
			state.log(new ActionHeal(target, value));
			target.addLife(caster, value);
		}
	}

	@Override
	public void applyStartTurn(State state) {

		if (target.hasState(EntityState.UNHEALABLE)) return;

		int life = value;
		if (target.getLife() + life > target.getTotalLife()) {
			life = target.getTotalLife() - target.getLife();
		}
		state.log(new ActionHeal(target, life));
		target.addLife(caster, life);
	}
}
