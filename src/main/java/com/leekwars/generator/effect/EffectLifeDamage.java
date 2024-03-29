package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionDamage;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

public class EffectLifeDamage extends Effect {

	private int returnDamage = 0;

	@Override
	public void apply(State state) {

		// Base damages
		double d = ((value1 + jet * value2) / 100) * caster.getLife() * aoe * criticalPower * (1 + caster.getPower() / 100.0);

		if (target.hasState(EntityState.INVINCIBLE)) {
			d = 0;
		}

		// Return damage
		if (target != caster) {
			returnDamage = (int) Math.round(d * target.getDamageReturn() / 100.0);
		}

		// Shields
		d -= d * (target.getRelativeShield() / 100.0) + target.getAbsoluteShield();
		d = Math.max(0, d);

		value = (int) Math.round(d);

		if (target.getLife() < value) {
			value = target.getLife();
		}

		int erosion = (int) Math.round(value * erosionRate);

		state.log(new ActionDamage(DamageType.LIFE, target, value, erosion));
		target.removeLife(value, erosion, caster, DamageType.LIFE, this, getItem());
		target.onDirectDamage(value);
		target.onNovaDamage(erosion);

		// Return damage
		if (returnDamage > 0 && !caster.hasState(EntityState.INVINCIBLE)) {

			if (caster.getLife() < returnDamage) {
				returnDamage = caster.getLife();
			}

			int returnErosion = (int) Math.round(returnDamage * erosionRate);

			if (returnDamage > 0) {
				state.log(new ActionDamage(DamageType.RETURN, caster, returnDamage, returnErosion));
				caster.removeLife(returnDamage, returnErosion, target, DamageType.RETURN, this, getItem());
			}
		}
	}
}
