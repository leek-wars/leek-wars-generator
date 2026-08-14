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
	}

	/**
	 * Un état est binaire : on ne peut pas en retirer 40 %. La réduction générique mettait
	 * la valeur à l'échelle, or c'est l'identifiant de l'état — une Libération à 40 % sur
	 * Stérile donnait round(12 × 0,6) = 7, l'état magnétisé, et l'état disparaissait de
	 * l'affichage du combat. Seule une réduction totale retire l'état.
	 */
	@Override
	public void reduce(double percent, Entity caster) {
		if (percent >= 1) {
			value = 0;
		}
	}
}