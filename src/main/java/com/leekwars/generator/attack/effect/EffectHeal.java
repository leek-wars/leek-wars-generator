package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionHeal;

public class EffectHeal extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * (1 + (double) caster.getWisdom() / 100) * power * criticalPower);

		if (turns == 0) {
			if (target.getLife() + value > target.getTotalLife()) {
				value = target.getTotalLife() - target.getLife();
			}
			fight.log(new ActionHeal(target, value));
			target.addLife(value);
		}
	}

	@Override
	public void applyStartTurn(Fight fight) {

		int life = value;
		if (target.getLife() + life > target.getTotalLife()) {
			life = target.getTotalLife() - target.getLife();
		}
		fight.log(new ActionHeal(target, life));
		target.addLife(life);
	}

	public void reduce() {
		value /= 2;
	}
}
