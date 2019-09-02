package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionHeal implements Action {

	private final int target;
	private final int life;

	public ActionHeal(Entity target, int life) {
		this.target = target.getFId();
		this.life = life;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.HEAL);
		retour.add(target);
		retour.add(life);
		return retour;
	}
}
