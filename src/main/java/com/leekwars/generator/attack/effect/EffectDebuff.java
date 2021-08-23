package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionReduceEffects;

public class EffectDebuff extends Effect {

	@Override
	public void apply(Fight fight) {
		value = (int) ((value1 + jet * value2) * aoe * criticalPower * targetCount);
		target.reduceEffects((double) value / 100, caster);

		// "Les effets de X sont réduits de Y%"
		fight.log(new ActionReduceEffects(target, value));
	}
}
