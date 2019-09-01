package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionHeal;
import com.leekwars.game.fight.action.ActionLoseLife;

public class EffectDamage extends Effect {

	private int returnDamage = 0;
	private int lifeSteal = 0;

	@Override
	public void apply(Fight fight) {

		// Base damages
		double d = (value1 + jet * value2) * (1 + Math.max(0, caster.getStrength()) / 100.0) * power * criticalPower;

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

		// Life steal
		if (target != caster) {
			lifeSteal = (int) Math.round(value * caster.getWisdom() / 1000.0);
		}

		// One shoot
		if (target.getTotalLife() == value && caster != target) {
			fight.getTrophyManager().roxxor(caster);
		}

		int erosion = (int) Math.round(value * erosionRate);

		fight.log(new ActionLoseLife(target, value, erosion));
		target.removeLife(value, erosion, caster, true);

		// Life steal
		if (lifeSteal > 0) {

			if (caster.getLife() + lifeSteal > caster.getTotalLife()) {
				lifeSteal = caster.getTotalLife() - caster.getLife();
			}
			if (lifeSteal > 0) {
				fight.log(new ActionHeal(caster, lifeSteal));
				caster.addLife(lifeSteal);
			}
		}

		// Return damage 
		if (returnDamage > 0) {

			if (caster.getLife() < returnDamage) {
				returnDamage = caster.getLife();
			}

			int returnErosion = (int) Math.round(returnDamage * erosionRate);

			if (returnDamage > 0) {
				fight.log(new ActionLoseLife(caster, returnDamage, returnErosion));
				caster.removeLife(returnDamage, returnErosion, target, false);
			}
		}
	}
}
