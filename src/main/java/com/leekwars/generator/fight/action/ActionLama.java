package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionLama implements Action {

	private final int caster;

	public ActionLama(Entity caster) {
		this.caster = caster.getFId();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.LAMA);
		retour.add(caster);
		return retour;
	}
}
