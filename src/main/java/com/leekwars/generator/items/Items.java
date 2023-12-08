package com.leekwars.generator.items;

import java.util.TreeMap;

public class Items {

	public final static int TYPE_WEAPON = 1;
	public final static int TYPE_CHIP = 2;
	public final static int TYPE_POTION = 3;
	public final static int TYPE_COMPONENT = 8;

	private static TreeMap<Integer, Integer> items = new TreeMap<Integer, Integer>();

	public static void addWeapon(int id) {
		items.put(id, TYPE_WEAPON);
	}
	public static void addChip(int id) {
		items.put(id, TYPE_CHIP);
	}
	public static void addComponent(int id) {
		items.put(id, TYPE_COMPONENT);
	}

	public static Integer getType(int item) {
		return items.get(item);
	}
}
