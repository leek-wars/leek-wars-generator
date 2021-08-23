package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionNovaVitality;

public class EffectNovaVitality extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getScience() / 100.0) * aoe * criticalPower);

		fight.log(new ActionNovaVitality(target, value));
		target.addTotalLife(value, caster);
	}
}
