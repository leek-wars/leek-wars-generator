package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionVitality;

public class EffectVitality extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getWisdom() / 100.0) * aoe * criticalPower);

		fight.log(new ActionVitality(target, value));
		target.addTotalLife(value, caster);
		target.addLife(caster, value);
	}
}
