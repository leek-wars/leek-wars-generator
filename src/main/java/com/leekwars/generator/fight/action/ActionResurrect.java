package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionResurrect implements Action {
	private final int target;
	private final int cell;
	private final int owner;
	private final int life;
	private final int max_life;

	public ActionResurrect(Entity owner, Entity target) {
		this.owner = owner.getFId();
		this.target = target.getFId();
		this.cell = target.getCell().getId();
		this.life = target.getLife();
		this.max_life = target.getTotalLife();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.RESURRECT);
		retour.add(owner);
		retour.add(target);
		retour.add(cell);
		retour.add(life);
		retour.add(max_life);
		return retour;
	}

}
