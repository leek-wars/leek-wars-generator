package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.fight.entity.Entity;

public class ActionAddEffect implements Action {

	private final int type;
	private final int attackID;
	private final int id;
	private final int caster;
	private final int target;
	private final int effectID;
	private final int value;
	private final int turns;

	public static int createEffect(Actions logs, int type, int attackID, Entity caster, Entity target, int effectID, int value, int turns) {

		int r = logs.getEffectId();
		ActionAddEffect effect = new ActionAddEffect(type, attackID, r, caster.getFId(), target.getFId(), effectID, value, turns);
		logs.log(effect);
		return r;
	}

	public ActionAddEffect(int type, int attackID, int id, int caster, int target, int effectID, int value, int turns) {

		if (type == Attack.TYPE_CHIP) {
			this.type = Action.ADD_CHIP_EFFECT;
		} else if (type == Attack.TYPE_WEAPON) {
			this.type = Action.ADD_WEAPON_EFFECT;
		} else {
			this.type = type;
		}
		this.attackID = attackID;
		this.id = id;
		this.caster = caster;
		this.target = target;
		this.effectID = effectID;
		this.value = value;
		this.turns = turns;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(type);
		retour.add(attackID);
		retour.add(id);
		retour.add(caster);
		retour.add(target);
		retour.add(effectID);
		retour.add(value);
		retour.add(this.turns);
		return retour;
	}
}
