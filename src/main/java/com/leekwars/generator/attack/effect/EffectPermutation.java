package com.leekwars.generator.attack.effect;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.maps.Cell;

public class EffectPermutation extends Effect {

	@Override
	public void apply(Fight fight) {

		fight.invertEntities(caster, target);
	}
}
