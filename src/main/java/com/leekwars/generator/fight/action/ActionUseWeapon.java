package com.leekwars.generator.fight.action;

import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.maps.Cell;

public class ActionUseWeapon implements Action {

	private final int caster;
	private final int cell;
	private final int weapon;
	private final int success;
	private int[] leeks;

	public ActionUseWeapon(Entity caster, Cell cell, Weapon weapon, int success) {

		this.caster = caster.getFId();
		this.cell = cell.getId();
		this.weapon = weapon.getTemplate();
		this.success = success;
		this.leeks = new int[0];
	}

	public void setEntities(List<Entity> leeks) {
		if (leeks != null) {
			this.leeks = new int[leeks.size()];
			for (int i = 0; i < leeks.size(); i++) {
				this.leeks[i] = leeks.get(i).getFId();
			}
		} else
			this.leeks = new int[0];
	}

	@Override
	public JSONArray getJSON() {

		JSONArray retour = new JSONArray();
		retour.add(Action.USE_WEAPON);
		retour.add(caster);
		retour.add(cell);
		retour.add(weapon);
		retour.add(success);
		retour.add(leeks);
		return retour;
	}

}
