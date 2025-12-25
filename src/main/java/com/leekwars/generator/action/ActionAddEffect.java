package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.state.Entity;

public class ActionAddEffect implements Action {

	private final int type;
	private final int itemID;
	private final int id;
	private final int caster;
	private final int target;
	private final int effectID;
	private final int value;
	private final int turns;
	private final int modifiers;

	public static int createEffect(Actions logs, int type, int itemID, Entity caster, Entity target, int effectID, int value, int turns, int modifiers) {

		int r = logs.getEffectId();
		ActionAddEffect effect = new ActionAddEffect(type, itemID, r, caster.getFId(), target.getFId(), effectID, value, turns, modifiers);
		logs.log(effect);
		return r;
	}

	public ActionAddEffect(int type, int itemID, int id, int caster, int target, int effectID, int value, int turns, int modifiers) {
		if (type == Attack.TYPE_CHIP) {
			this.type = Action.ADD_CHIP_EFFECT;
		} else if (type == Attack.TYPE_WEAPON) {
			this.type = Action.ADD_WEAPON_EFFECT;
		} else {
			this.type = type;
		}
		this.itemID = itemID;
		this.id = id;
		this.caster = caster;
		this.target = target;
		this.effectID = effectID;
		this.value = value;
		this.turns = turns;
		this.modifiers = modifiers;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(type);
		retour.add(itemID);
		retour.add(id);
		retour.add(caster);
		retour.add(target);
		retour.add(effectID);
		retour.add(value);
		retour.add(turns);
		if (modifiers != 0) {
			retour.add(modifiers);
		}
		return retour;
	}
}
