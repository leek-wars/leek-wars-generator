package com.leekwars.generator.attack.weapons;

import java.util.Map;
import java.util.TreeMap;

import com.leekwars.generator.items.Items;

public class Weapons {

	private static Map<Integer, Weapon> weapons = new TreeMap<Integer, Weapon>();

	public static void addWeapon(Weapon weapon) {
		weapons.put(weapon.getId(), weapon);
		Items.addWeapon(weapon.getId());
	}

	public static Weapon getWeapon(int id) {
		return weapons.get(id);
	}

	public static Map<Integer, Weapon> getTemplates() {
		return weapons;
	}
}
