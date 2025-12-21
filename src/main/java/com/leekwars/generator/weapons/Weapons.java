package com.leekwars.generator.weapons;

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

	public static Weapon getWeapon(String name) {
		return weapons.values().stream().filter(w -> w.getName().equals(name)).findFirst().get();
	}

	public static Map<Integer, Weapon> getTemplates() {
		return weapons;
	}
}
