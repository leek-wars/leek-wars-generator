package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionDamage;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

/**
 * Surinfection : fait détoner les poisons actifs de la cible. Ils infligent d'un coup
 * value1 % (50 % pour la puce Surinfection, 65 % en critique) de leur somme à l'instant T
 * — la somme de leurs valeurs par tour — et perdent chacun UN tour.
 *
 * Un poison ne compte donc que pour ce qu'il vaut au moment où on le fait détoner, jamais
 * multiplié par les tours qu'il lui restait à courir : on avance un demi-tour de poison
 * contre un tour entier, et l'échange est perdant de la même quantité quel que soit le
 * moment choisi. `turns` est le nombre de coups qu'il reste au poison (cf. PeriodicEffect) :
 * le tour brûlé ici en retire toujours un vrai.
 *
 * Un poison qui tombe à 0 tour disparaît, les autres restent en place avec leur valeur
 * intacte : la Surinfection ne nettoie plus la cible, c'est le rôle de l'Antidote.
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
			// n'a pas de tour à perdre.
			if (e.getTurns() == -1) continue;

			poisons += e.value;

			// Le poison brûle un tour : un coup de moins à venir.
			if (target.loseEffectTurn(e)) {
				i--;
			} else {
				// Le client tient sa propre horloge (décrément à chaque coup) : sans ça il
				// afficherait un tour de trop jusqu'à la fin du poison.
				target.updateEffectTurns(e);
			}
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
