package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.fight.entity.Entity;

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

	public static int createEffect(Actions logs, int type, int itemID, Entity caster, Entity target, int effectID, int value, int turns, boolean stacked, int modifiers) {

		int r = logs.getEffectId();
		ActionAddEffect effect = new ActionAddEffect(type, itemID, r, caster.getFId(), target.getFId(), effectID, value, turns, stacked, modifiers);
		logs.log(effect);
		return r;
	}

	public ActionAddEffect(int type, int itemID, int id, int caster, int target, int effectID, int value, int turns, boolean stacked, int modifiers) {

		if (type == Attack.TYPE_CHIP) {
			if (stacked) {
				this.type = Action.ADD_STACKED_EFFECT;
			} else {
				this.type = Action.ADD_CHIP_EFFECT;
			}
		} else if (type == Attack.TYPE_WEAPON) {
			if (stacked) {
				this.type = Action.ADD_STACKED_EFFECT;
			} else {
				this.type = Action.ADD_WEAPON_EFFECT;
			}
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
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(type);
		retour.add(itemID);
		retour.add(id);
		retour.add(caster);
		retour.add(target);
		retour.add(effectID);
		retour.add(value);
		retour.add(turns);
		retour.add(modifiers);
		return retour;
	}
}
