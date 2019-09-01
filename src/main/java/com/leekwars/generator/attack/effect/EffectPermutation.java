package com.leekwars.game.attack.effect;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.maps.Cell;

public class EffectPermutation extends Effect {

	@Override
	public void apply(Fight fight) {

		Cell start = caster.getCell();
		Cell end = target.getCell();
		if (start == null || end == null) {
			return;
		}

		caster.setHasMoved(true);

		target.setCell(start);
		start.setCellPlayer(target);

		end.setCellPlayer(caster);
		caster.setCell(end);
	}
}
