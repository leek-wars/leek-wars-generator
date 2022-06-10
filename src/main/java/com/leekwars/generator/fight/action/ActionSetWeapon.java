package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.attack.weapons.Weapon;

public class ActionSetWeapon implements Action {

	public int leek;
	public int weapon;

	public ActionSetWeapon(Weapon weapon) {
		this.weapon = weapon.getTemplate();
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.SET_WEAPON);
		retour.add(weapon);
		return retour;
	}

}
