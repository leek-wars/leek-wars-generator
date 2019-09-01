package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionLoseLife;

public class EffectPoison extends Effect {

	@Override
	public void apply(Fight fight) {
		value = (int) Math.round(((value1 + jet * value2)) * (1 + (double) Math.max(0, caster.getMagic()) / 100) * power * criticalPower);
	}

	@Override
	public void applyStartTurn(Fight fight) {

		int damages = value;
		if (target.getLife() < damages) {
			damages = target.getLife();
		}

		int erosion = (int) Math.round(damages * erosionRate);

		fight.log(new ActionLoseLife(target, damages, erosion));
		target.removeLife(damages, erosion, caster, false);
		fight.statistics.addDamagePoison(damages);
	}
	
	public void reduce() {
		value /= 2;
	}
}
