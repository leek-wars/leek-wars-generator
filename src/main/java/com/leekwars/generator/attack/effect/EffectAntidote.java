package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionRemovePoisons;

public class EffectAntidote extends Effect {

	@Override
	public void apply(Fight fight) {

		target.clearPoisons(caster);

		// "Les poisons de X sont neutralisés"
		fight.log(new ActionRemovePoisons(target));
	}
}
