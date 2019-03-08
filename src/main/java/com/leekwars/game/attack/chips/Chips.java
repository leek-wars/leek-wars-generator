package com.leekwars.game.attack.chips;

import java.util.Map;
import java.util.TreeMap;

public class Chips {

	private static Map<Integer, Chip> templates = new TreeMap<Integer, Chip>();
	private static Map<Integer, Chip> templatesByItem = new TreeMap<Integer, Chip>();

	public static void addChipTemplate(Chip template) {
		templates.put(template.getId(), template);
		templatesByItem.put(template.getId(), template);
	}

	public static Chip getChipTemplateByItem(int id) {
		if (!templatesByItem.containsKey(id))
			return null;
		return templatesByItem.get(id);
	}

	public static Chip getChipTemplate(int id) {
		return templates.get(id);
	}

	public static Map<Integer, Chip> getTemplates() {
		return templates;
	}
}
