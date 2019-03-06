package com.leekwars.game.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.game.attack.weapons.Weapon;
import com.leekwars.game.fight.entity.Entity;

public class ActionSetWeapon implements Action {

	public int leek;
	public int weapon;

	public ActionSetWeapon(Entity leek, Weapon weapon) {
		this.leek = leek.getFId();
		this.weapon = weapon.getWeaponTemplate().getId();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.SET_WEAPON);
		retour.add(leek);
		retour.add(weapon);
		return retour;
	}

}
