package com.leekwars.generator.effect;

import com.leekwars.generator.action.ActionNovaVitality;
import com.leekwars.generator.action.ActionVitality;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.state.State;

public class EffectVitality extends Effect {

	@Override
	public void apply(State state) {

		value = (int) Math.round((value1 + jet * value2) * (1 + caster.getWisdom() / 100.0) * aoe * criticalPower);

		value = Math.max(0, value); // Soin negatif si la sagesse est negative

		// « Non soignable » bloque les PV rendus, pas la jauge : la Vitalité agrandit
		// toujours la vie max, elle ne la remplit plus. C'était le dernier gain de vie
		// courante qui passait au travers de l'état (Soin, Soin brut, Vol de vie et
		// vampirisme le respectent tous), et il en passait beaucoup : la Vitalité rend
		// autant de PV qu'elle donne de vie max.
		//
		// Une cible non soignable reçoit donc exactement une Nova Vitalité — même effet,
		// et le client sait déjà rejouer cette action-là sans toucher à la vie courante
		// (`winMaxLife`). Rejouer un `VITALITY` amputé de son soin désynchroniserait la
		// barre de vie du combat rendu, qui ajoute la valeur aux deux.
		if (target.hasState(EntityState.UNHEALABLE)) {
			state.log(new ActionNovaVitality(target, value));
			target.addTotalLife(value, caster);
			return;
		}

		state.log(new ActionVitality(target, value));
		target.addTotalLife(value, caster);
		target.addLife(caster, value);
	}
}
