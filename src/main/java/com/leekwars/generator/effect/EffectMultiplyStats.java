package com.leekwars.generator.effect;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

/**
 * Multiplies all stats of the target by value1.
 * Used for Colossus mode to boost the colossus's stats.
 */
public class EffectMultiplyStats extends Effect {

	@Override
	public void apply(State state) {

		int factor = (int) value1;
		if (factor <= 1) return;

		value = factor;

		// Multiply all base stats (except life, handled separately)
		int[] statIds = {
			Entity.STAT_STRENGTH, Entity.STAT_AGILITY,
			Entity.STAT_RESISTANCE, Entity.STAT_WISDOM, Entity.STAT_SCIENCE,
			Entity.STAT_MAGIC, Entity.STAT_FREQUENCY, Entity.STAT_TP, Entity.STAT_MP
		};

		for (int statId : statIds) {
			int base = target.getBaseStats().getStat(statId);
			int buff = base * (factor - 1);
			if (buff > 0) {
				stats.setStat(statId, buff);
				target.updateBuffStats(statId, buff, caster);
			}
		}

		// Life: add 1x base life to max life per factor, preserving ratio and erosion.
		// On first apply (factor=5): add 4x base life
		// On replacement (factor=6 after remove of factor=5): mTotalLife still has
		// the old bonus since removeEffect doesn't undo addTotalLife. The old bonus
		// was (oldFactor-1)*base, so mTotalLife = base*oldFactor - erosion.
		// We need to add exactly 1x base (the delta between factors).
		// For the first apply, there's no previous effect, so we add (factor-1)*base.
		// We detect first apply by checking if mTotalLife <= lifeBase (no prior boost).
		int lifeBase = target.getBaseStats().getStat(Entity.STAT_LIFE);
		int lifeDelta;
		if (target.getTotalLife() <= lifeBase) {
			// First apply: no previous boost
			lifeDelta = lifeBase * (factor - 1);
		} else {
			// Replacement: previous boost still in mTotalLife, just add 1x base
			lifeDelta = lifeBase;
		}

		double ratio = target.getTotalLife() > 0 ? (double) target.getLife() / target.getTotalLife() : 1.0;
		target.addTotalLife(lifeDelta, caster);
		int targetLife = (int) Math.round(target.getTotalLife() * ratio);
		int healAmount = targetLife - target.getLife();
		if (healAmount > 0) {
			target.addLife(caster, healAmount);
		}
	}
}
