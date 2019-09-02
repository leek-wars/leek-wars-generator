package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionLoseLife implements Action {

	private final int target;
	private final int pv;
	private final int erosion;

	public ActionLoseLife(Entity target, int pv, int erosion) {
		this.target = target.getFId();
		this.pv = pv;
		this.erosion = erosion;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.LOST_LIFE);
		retour.add(target);
		retour.add(pv);
		retour.add(erosion);
		return retour;
	}
}
