package com.leekwars.generator.action;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.util.Util;

public class ActionShowCell implements Action {

	private final int mCell;
	private final int mColor;

	public ActionShowCell(int cell, int color) {
		mCell = cell;
		mColor = color;
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.SHOW_CELL);
		retour.add(mCell);
		retour.add(Util.getHexaColor(mColor));

		return retour;
	}
}
