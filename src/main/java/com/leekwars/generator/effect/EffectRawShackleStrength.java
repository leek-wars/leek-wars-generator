package com.leekwars.generator.effect;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

/**
 * Entrave de force brute : retire une valeur fixe, sans la magie du lanceur (pendant négatif des
 * buffs RAW). Elle n'hérite pas d'EffectShackleStrength : la Libération ne l'enlève pas.
 */
public class EffectRawShackleStrength extends Effect {

	@Override
	public void apply(State state) {

		value = (int) Math.round((value1 + jet * value2) * aoe * criticalPower);
		if (value > 0) {
			stats.setStat(Entity.STAT_STRENGTH, -value);
			target.updateBuffStats(Entity.STAT_STRENGTH, -value, caster);
		}
	}
}
