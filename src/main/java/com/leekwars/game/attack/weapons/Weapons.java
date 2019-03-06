package com.leekwars.game.attack.weapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.leekwars.game.Util;

public class Weapons {

	private static Map<Integer, WeaponTemplate> templates = new TreeMap<Integer, WeaponTemplate>();
	private static Map<Integer, WeaponTemplate> templatesByItem = new TreeMap<Integer, WeaponTemplate>();

	public static void addWeaponTemplate(WeaponTemplate template) {
		templates.put(template.getId(), template);
		templatesByItem.put(template.getTemplate().getId(), template);
	}

	public static WeaponTemplate getBestWeaponForLevel(int level) {
		int high = 0;
		WeaponTemplate retour = null;
		for (WeaponTemplate tmp : templates.values()) {
			if (tmp.getTemplate().getLevel() < level && tmp.getTemplate().getLevel() > high) {
				retour = tmp;
				high = tmp.getTemplate().getLevel();
			}
		}
		return retour;
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

	public static WeaponTemplate getAleatWeaponForLevel(int level, int difficulty) {

		List<WeaponTemplate> tmp = new ArrayList<WeaponTemplate>();
		for (WeaponTemplate t : templates.values()) {
			if (level >= t.getTemplate().getLevel())
				tmp.add(t);
		}
		int nb = tmp.size() - difficulty - 1;
		if (nb < 0)
			nb = 0;
		int id = Util.getRandom(nb, tmp.size() - 1);
		if (id < 0 || id >= tmp.size())
			id = 0;
		if (tmp.size() == 0)
			return null;
		return tmp.get(id);
	}
}
