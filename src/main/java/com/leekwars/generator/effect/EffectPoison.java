package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionDamage;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

public class EffectPoison extends Effect {

	@Override
	public void apply(State state) {
		value = (int) Math.round(((value1 + jet * value2)) * (1 + (double) Math.max(0, caster.getMagic()) / 100) * aoe * criticalPower * (1 + caster.getPower() / 100.0));
	}

	@Override
	public void applyStartTurn(State state) {

		int damages = value;
		if (target.getLife() < damages) {
			damages = target.getLife();
		}

		if (target.hasState(EntityState.INVINCIBLE)) {
			damages = 0;
		}

		if (damages > 0) {
			int erosion = (int) Math.round(damages * erosionRate);

			state.log(new ActionDamage(DamageType.POISON, target, damages, erosion));
			target.removeLife(damages, erosion, caster, DamageType.POISON, this, getItem());
			target.onPoisonDamage(damages);
			target.onNovaDamage(erosion);
		}
	}
}
