package com.leekwars.game.items;

import java.util.TreeMap;

public class Items {
	private static TreeMap<Integer, ItemTemplate> sItemTemplates = new TreeMap<Integer, ItemTemplate>();
	private static TreeMap<Integer, ItemTemplate> sByWeapon = new TreeMap<Integer, ItemTemplate>();
	private static TreeMap<Integer, ItemTemplate> sByChip = new TreeMap<Integer, ItemTemplate>();

	public static void addItemTemplate(ItemTemplate template) {
		sItemTemplates.put(template.getId(), template);
		if (template.getType() == Item.TYPE_CHIP)
			sByChip.put(Integer.parseInt(template.getParams()), template);
		if (template.getType() == Item.TYPE_WEAPON)
			sByWeapon.put(Integer.parseInt(template.getParams()), template);
	}

	public static ItemTemplate getItemTemplate(int id) {
		return sItemTemplates.get(id);
	}

	public static ItemTemplate getWeaponItemTemplate(int id) {
		return sByWeapon.get(id);
	}

	public static ItemTemplate getChipItemTemplate(int id) {
		return sByChip.get(id);
	}
}
