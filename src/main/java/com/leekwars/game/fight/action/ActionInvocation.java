package com.leekwars.game.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.game.fight.entity.Entity;

public class ActionInvocation implements Action {
	private final int target;
	private final int cell;
	private final int owner;

	public ActionInvocation(Entity target) {
		this.owner = target.getOwnerId();
		this.target = target.getFId();
		this.cell = target.getCell().getId();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.SUMMON);
		retour.add(owner);
		retour.add(target);
		retour.add(cell);
		return retour;
	}

}
