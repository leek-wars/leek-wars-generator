package com.leekwars.generator.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.maps.Cell;

public class ActionUseChip implements Action {

	private final int cell;
	private final int chip;
	private final int success;

	public ActionUseChip(Cell cell, Chip chip, int success) {
		this.cell = cell.getId();
		this.chip = chip.getTemplate();
		this.success = success;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.USE_CHIP);
		retour.add(chip);
		retour.add(cell);
		retour.add(success);
		return retour;
	}
}
