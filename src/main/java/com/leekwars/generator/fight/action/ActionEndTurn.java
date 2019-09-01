package com.leekwars.game.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.game.fight.entity.Entity;

public class ActionEndTurn implements Action {

	private final int target;
	private final int pt;
	private final int pm;
	private final int strength;
	private final int magic;

	public ActionEndTurn(Entity target) {

		this.target = target.getFId();
		this.pt = target.getTP();
		this.pm = target.getMP();
		this.strength = target.getStrength();
		this.magic = target.getMagic();
	}

	@Override
	public JSONArray getJSON() {

		JSONArray json = new JSONArray();
		json.add(Action.END_TURN);
		json.add(target);
		json.add(pt);
		json.add(pm);
		json.add(strength);
		json.add(magic);
		return json;
	}
}
