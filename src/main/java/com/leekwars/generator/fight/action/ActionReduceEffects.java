package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionReduceEffects implements Action {

	private final int id;
	private final int value;

	public ActionReduceEffects(Entity target, int value) {
		this.id = target.getFId();
		this.value = value;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.REDUCE_EFFECTS);
		retour.add(id);
		retour.add(value);
		return retour;
	}
}