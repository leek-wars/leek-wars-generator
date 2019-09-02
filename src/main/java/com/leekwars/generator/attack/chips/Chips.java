package com.leekwars.generator.attack.chips;

import java.util.Map;
import java.util.TreeMap;

import com.leekwars.generator.items.Items;

public class Chips {

	private static Map<Integer, Chip> chips = new TreeMap<Integer, Chip>();

	public static void addChip(Chip chip) {
		chips.put(chip.getId(), chip);
		Items.addChip(chip.getId());
	}

	public static Chip getChip(int id) {
		return chips.get(id);
	}

	public static Map<Integer, Chip> getTemplates() {
		return chips;
	}
}
