package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionNovaVitality implements Action {

	private final int target;
	private final int life;

	public ActionNovaVitality(Entity target, int life) {
		this.target = target.getFId();
		this.life = life;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.NOVA_VITALITY);
		retour.add(target);
		retour.add(life);
		return retour;
	}
}
