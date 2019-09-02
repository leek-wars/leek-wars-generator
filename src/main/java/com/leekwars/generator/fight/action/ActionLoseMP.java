package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionLoseMP implements Action {

	private final int target;
	private final int mp;

	public ActionLoseMP(Entity target, int pm) {

		this.target = target.getFId();
		this.mp = pm;
	}

	@Override
	public JSONArray getJSON() {

		JSONArray json = new JSONArray();
		json.add(Action.LOST_PM);
		json.add(target);
		json.add(mp);

		return json;
	}

}
