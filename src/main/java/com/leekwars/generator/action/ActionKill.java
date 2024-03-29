package com.leekwars.generator.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.state.Entity;

public class ActionKill implements Action {

	private final int caster;
	private final int target;

	public ActionKill(Entity caster, Entity target) {
		this.caster = target.getFId();
		this.target = target.getFId();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.KILL);
		retour.add(caster);
		retour.add(target);
		return retour;
	}
}
