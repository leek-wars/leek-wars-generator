package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.Util;

public class ActionShowCell implements Action {

	private final int mCell;
	private final int mColor;

	public ActionShowCell(int cell, int color) {
		mCell = cell;
		mColor = color;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.SHOW_CELL);
		retour.add(mCell);
		retour.add(Util.getHexaColor(mColor));

		return retour;
	}
}
