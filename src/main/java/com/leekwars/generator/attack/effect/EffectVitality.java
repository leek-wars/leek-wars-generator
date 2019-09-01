package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionVitality;

public class EffectVitality extends Effect {

	@Override
	public void apply(Fight fight) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getWisdom() / 100.0) * power * criticalPower);

		fight.log(new ActionVitality(target, value));
		target.addTotalLife(value);
		target.addLife(value);
	}
}
