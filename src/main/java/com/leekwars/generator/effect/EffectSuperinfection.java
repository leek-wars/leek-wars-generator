package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionDamage;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

/**
 * Surinfection : convertit une partie des poisons actifs de la cible en dégâts
 * immédiats. Chaque effet poison inflige tout de suite value1 % (50 % pour la
 * puce Surinfection, 65 % en critique) de ses dégâts totaux restants, et sa
 * valeur par tour est réduite d'autant : c'est une conversion, pas une
 * duplication — le total de poison subi est conservé, seulement avancé.
 *
 * Les dégâts convertis sont des dégâts de poison (érosion de poison, crédités
 * au lanceur de la Surinfection). La conversion ignore le flag irréductible
 * des poisons : ce n'est pas une réduction subie par le poison, c'est son
 * activation anticipée.
 */
public class EffectSuperinfection extends Effect {

	@Override
	public void apply(State state) {

		// Ratio de conversion (50 % de base), borné à 100 %.
		double ratio = Math.min(1.0, ((value1 + jet * value2) / 100.0) * criticalPower);

		int converted = 0;

		var effects = target.getEffects();
		for (int i = 0; i < effects.size(); ++i) {
			var e = effects.get(i);
			if (!(e instanceof EffectPoison)) continue;
			// Poison infini (turns == -1) : pas de « total restant » défini, on l'ignore.
			if (e.getTurns() <= 0) continue;

			int before = e.value;
			e.reduce(ratio, caster);
			// Dégâts convertis = exactement ce qui a été retiré du poison.
			converted += (before - e.value) * e.getTurns();

			if (e.value <= 0) {
				e.getCaster().removeLaunchedEffect(e);
				target.removeEffect(e);
				i--;
			} else {
				target.updateEffect(e);
			}
		}
		target.updateBuffStats();

		int damages = converted;
		if (target.getLife() < damages) {
			damages = target.getLife();
		}
		if (target.hasState(EntityState.INVINCIBLE)) {
			damages = 0;
		}

		if (damages > 0) {
			// Érosion de poison, avec le bonus critique habituel.
			double rate = EROSION_POISON + (critical ? EROSION_CRITICAL_BONUS : 0);
			int erosion = (int) Math.round(damages * rate);

			state.log(new ActionDamage(DamageType.POISON, target, damages, erosion));
			target.removeLife(damages, erosion, caster, DamageType.POISON, this, getItem());
			target.onPoisonDamage(damages);
			target.onNovaDamage(erosion);
		}

		value = damages;
	}
}
