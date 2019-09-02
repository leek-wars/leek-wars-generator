package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionAIError implements Action {

	private final int id;

	public ActionAIError(Entity leek) {
		if (leek == null)
			this.id = -1;
		else
			this.id = leek.getFId();
	}

	@Override
	public JSONArray getJSON() {

		JSONArray retour = new JSONArray();
		retour.add(Action.AI_ERROR);
		retour.add(id);
		return retour;
	}
}
