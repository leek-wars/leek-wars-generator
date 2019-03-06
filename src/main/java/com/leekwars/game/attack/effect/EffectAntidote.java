package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;

public class EffectAntidote extends Effect {

	@Override
	public void apply(Fight fight) {
		target.clearPoisons();
	}
}
