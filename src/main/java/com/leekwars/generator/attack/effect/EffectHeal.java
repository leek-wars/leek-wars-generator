package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionHeal;

public class EffectHeal extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * (1 + (double) caster.getWisdom() / 100) * aoe * criticalPower * targetCount);

		value = Math.max(0, value); // Soin negatif si la sagesse est negative

		if (turns == 0) {
			if (target.getLife() + value > target.getTotalLife()) {
				value = target.getTotalLife() - target.getLife();
			}
			fight.log(new ActionHeal(target, value));
			target.addLife(caster, value);
		}
	}

	@Override
	public void applyStartTurn(Fight fight) {

		int life = value;
		if (target.getLife() + life > target.getTotalLife()) {
			life = target.getTotalLife() - target.getLife();
		}
		fight.log(new ActionHeal(target, life));
		target.addLife(caster, life);
	}
}
