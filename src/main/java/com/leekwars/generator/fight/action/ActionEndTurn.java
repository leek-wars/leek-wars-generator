package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionEndTurn implements Action {

	private final int target;
	private final int pt;
	private final int pm;

	public ActionEndTurn(Entity target) {

		this.target = target.getFId();
		this.pt = target.getTP();
		this.pm = target.getMP();
	}

	@Override
	public JSONArray getJSON() {

		JSONArray json = new JSONArray();
		json.add(Action.END_TURN);
		json.add(target);
		json.add(pt);
		json.add(pm);
		return json;
	}
}
