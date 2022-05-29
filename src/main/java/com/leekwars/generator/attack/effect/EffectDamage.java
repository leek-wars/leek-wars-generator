package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionHeal;
import com.leekwars.generator.fight.action.DamageType;
import com.leekwars.generator.fight.action.ActionDamage;

public class EffectDamage extends Effect {

	private int returnDamage = 0;
	private int lifeSteal = 0;

	@Override
	public void apply(Fight fight) {

		// Base damages
		double d = (value1 + jet * value2) * (1 + Math.max(0, caster.getStrength()) / 100.0) * aoe * criticalPower * targetCount * (1 + caster.getPower() / 100.0);

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

		int erosion = (int) Math.round(value * erosionRate);

		fight.log(new ActionDamage(DamageType.DIRECT, target, value, erosion));
		target.removeLife(value, erosion, caster, DamageType.DIRECT, this);
		target.onDirectDamage(value);
		target.onNovaDamage(erosion);

		// Life steal
		if (!caster.isDead() && lifeSteal > 0 && caster.getLife() < caster.getTotalLife()) {

			if (caster.getLife() + lifeSteal > caster.getTotalLife()) {
				lifeSteal = caster.getTotalLife() - caster.getLife();
			}
			if (lifeSteal > 0) {
				fight.log(new ActionHeal(caster, lifeSteal));
				caster.addLife(caster, lifeSteal);
			}
		}

		// Return damage
		if (returnDamage > 0) {

			if (caster.getLife() < returnDamage) {
				returnDamage = caster.getLife();
			}

			int returnErosion = (int) Math.round(returnDamage * erosionRate);

			if (returnDamage > 0) {
				fight.log(new ActionDamage(DamageType.RETURN, caster, returnDamage, returnErosion));
				caster.removeLife(returnDamage, returnErosion, target, DamageType.RETURN, this);
				caster.onNovaDamage(returnErosion);
			}
		}
	}
}
