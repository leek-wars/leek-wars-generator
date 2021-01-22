package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionRemoveShackles;

public class EffectRemoveShackles extends Effect {

	@Override
	public void apply(Fight fight) {
		target.removeShackles();

		// "Les entraves de X sont retirées"
		fight.log(new ActionRemoveShackles(target));
	}
}
