package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;

public class ActionDamage implements Action {

	private final DamageType type;
	private final int target;
	private final int pv;
	private final int erosion;

	public ActionDamage(DamageType type, Entity target, int pv, int erosion) {
		this.type = type;
		this.target = target.getFId();
		this.pv = pv;
		this.erosion = erosion;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(type.value);
		retour.add(target);
		retour.add(pv);
		retour.add(erosion);
		return retour;
	}
}
