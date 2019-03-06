package com.leekwars.game.attack.chips;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.leekwars.game.Util;

public class Chips {

	private static Map<Integer, ChipTemplate> templates = new TreeMap<Integer, ChipTemplate>();
	private static Map<Integer, ChipTemplate> templatesByItem = new TreeMap<Integer, ChipTemplate>();

	public static void addChipTemplate(ChipTemplate template) {
		templates.put(template.getId(), template);
		templatesByItem.put(template.getTemplate().getId(), template);
	}

	public static ChipTemplate getChipTemplateByItem(int id) {
		if (!templatesByItem.containsKey(id))
			return null;
		return templatesByItem.get(id);
	}

	public static ChipTemplate getChipTemplate(int id) {
		return templates.get(id);
	}

	public static ChipTemplate getAleatChipForLevel(int level) {
		List<ChipTemplate> tmp = new ArrayList<ChipTemplate>();
		for (ChipTemplate t : templates.values()) {
			if (level >= t.getTemplate().getLevel())
				tmp.add(t);
		}
		int id = Util.getRandom(0, tmp.size() - 1);
		if (id < 0 || id >= tmp.size())
			id = 0;
		if (tmp.size() == 0)
			return null;
		return tmp.get(id);
	}

	public static Map<Integer, ChipTemplate> getTemplates() {
		return templates;
	}
}
