package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionLoseTP implements Action {

	private final int target;
	private final int tp;

	public ActionLoseTP(Entity target, int pt) {

		this.target = target.getFId();
		this.tp = pt;
	}

	@Override
	public JSONArray getJSON() {

		JSONArray json = new JSONArray();
		json.add(Action.LOST_PT);
		json.add(target);
		json.add(tp);
		return json;
	}

}
