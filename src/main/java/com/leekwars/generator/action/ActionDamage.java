package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.state.Entity;

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
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(type.value);
		retour.add(target);
		retour.add(pv);
		retour.add(erosion);
		return retour;
	}
}
