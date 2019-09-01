package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;

public class EffectDebuff extends Effect {

	@Override
	public void apply(Fight fight) {
		target.reduceEffects();
	}
}
