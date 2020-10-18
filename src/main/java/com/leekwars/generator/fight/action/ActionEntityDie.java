package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionEntityDie implements Action {

	private final int id;
	private final int killer;

	public ActionEntityDie(Entity leek, Entity killer) {
		this.id = leek.getFId();
		this.killer = killer != null ? killer.getFId() : -1;
	}

	@Override
	public JSONArray getJSON() {

		JSONArray retour = new JSONArray();
		retour.add(Action.PLAYER_DEAD);
		retour.add(id);
		if (killer != -1) {
			retour.add(killer);
		}
		return retour;
	}
}
