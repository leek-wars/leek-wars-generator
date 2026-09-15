package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionDamage;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

/**
 * Surinfection : fait détoner les poisons actifs de la cible. Ils disparaissent tous et
 * infligent d'un coup value1 % (50 % pour la puce Surinfection, 65 % en critique) de leur
 * somme à l'instant T — la somme de leurs valeurs par tour.
 *
 * Un poison ne compte donc que pour ce qu'il vaut au moment où on le fait détoner, jamais
 * multiplié par les tours qu'il lui restait à courir : on échange des poisons en cours
 * contre des dégâts tout de suite, et cet échange est perdant si on le fait trop tôt.
 * Multiplier par les tours restants faisait rendre à un poison bien plus que sa valeur
 * affichée — jusqu'à 100 % de son total quand la cible venait de le subir, car `turns` ne
 * baisse qu'au tour du LANCEUR du poison, pas à celui de la cible qui le subit.
 *
 * Les dégâts sont des dégâts de poison (érosion de poison, crédités au lanceur de la
 * Surinfection). Ils ignorent le flag irréductible des poisons : ce n'est pas une réduction
 * subie par le poison, c'est son activation anticipée.
 */
public class EffectSuperinfection extends Effect {

	@Override
	public void apply(State state) {

		// Part de la somme des poisons qui part en dégâts (50 % de base), bornée à 100 %.
		double ratio = Math.min(1.0, ((value1 + jet * value2) / 100.0) * criticalPower);

		int poisons = 0;

		var effects = target.getEffects();
		for (int i = 0; i < effects.size(); ++i) {
			var e = effects.get(i);
			if (!(e instanceof EffectPoison)) continue;
			// Aucun item n'en pose aujourd'hui, mais un poison infini ne détone pas : il
			// n'a pas de fin, le faire disparaître serait un cadeau à la cible.
			if (e.getTurns() == -1) continue;

			poisons += e.value;
			e.getCaster().removeLaunchedEffect(e);
			target.removeEffect(e);
			i--;
		}

		int damages = (int) Math.round(poisons * ratio);
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
