package com.leekwars.generator.effect;

import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

public class EffectAddState extends Effect {


	@Override
	public void apply(State state) {

		this.value = (int) value1;
		this.state = EntityState.values()[(int) value1];
		target.addState(this.state);

		// Amputée, la cible lâche son arme : il faudra la rééquiper une fois l'état passé
		if (this.state == EntityState.AMPUTATED) {
			target.setWeapon(null);
		}
	}

	/**
	 * Un état est binaire : sa quantité vaut 1, pas `value`, qui porte l'identifiant de
	 * l'état. Mettre `value` à l'échelle donnait round(12 × 0,6) = 7 pour une Libération à
	 * 40 % sur Stérile, soit l'état magnétisé, et l'état disparaissait de l'affichage du
	 * combat faute d'icône. C'est donc à ce 1 que s'applique la réduction générique, avec
	 * le même arrondi que pour un effet qui ne vaudrait qu'un point de stat.
	 */
	@Override
	public void reduce(double percent, Entity caster) {
		if (Math.round(reductionFactor(percent)) == 0) {
			value = 0;
		}
	}
}