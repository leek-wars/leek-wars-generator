package com.leekwars.game.attack.weapons;

import java.util.Map;
import java.util.TreeMap;

public class Weapons {

	private static Map<Integer, Weapon> templates = new TreeMap<Integer, Weapon>();
	private static Map<Integer, Weapon> templatesByItem = new TreeMap<Integer, Weapon>();

	public static void addWeaponTemplate(Weapon template) {
		templates.put(template.getId(), template);
		templatesByItem.put(template.getTemplate(), template);
	}

	public static Weapon getWeaponTemplate(int id) {
		if (!templates.containsKey(id)) {
			return null;
		}
		return templates.get(id);
	}

	public static Weapon getWeaponTemplateByItem(int id) {
		if (!templatesByItem.containsKey(id))
			return null;
		return templatesByItem.get(id);
	}
}
