package com.leekwars.game.attack.weapons;

import java.util.Map;
import java.util.TreeMap;

public class Weapons {

	private static Map<Integer, WeaponTemplate> templates = new TreeMap<Integer, WeaponTemplate>();
	private static Map<Integer, WeaponTemplate> templatesByItem = new TreeMap<Integer, WeaponTemplate>();

	public static void addWeaponTemplate(WeaponTemplate template) {
		templates.put(template.getId(), template);
		templatesByItem.put(template.getTemplate(), template);
	}

	public static WeaponTemplate getWeaponTemplate(int id) {
		if (!templates.containsKey(id)) {
			return null;
		}
		return templates.get(id);
	}

	public static WeaponTemplate getWeaponTemplateByItem(int id) {
		if (!templatesByItem.containsKey(id))
			return null;
		return templatesByItem.get(id);
	}
}
