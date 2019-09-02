package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;

public class EffectAntidote extends Effect {

	@Override
	public void apply(Fight fight) {
		target.clearPoisons();
	}
}
