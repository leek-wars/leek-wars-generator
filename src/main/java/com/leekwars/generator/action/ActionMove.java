package com.leekwars.generator.action;

import java.util.List;

import tools.jackson.databind.node.ArrayNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.Entity;

public class ActionMove implements Action {

	private final int leek;
	private final int[] path;
	private final int end;

	public ActionMove(Entity leek, List<Cell> path) {
		this.leek = leek.getFId();
		this.path = new int[path.size()];
		for (int i = 0; i < path.size(); i++) {
			this.path[i] = path.get(i).getId();
		}
		end = path.get(path.size() - 1).getId();
	}

	@Override
	public ArrayNode getJSON() {
		ArrayNode retour = Json.createArray();
		retour.add(Action.MOVE_TO);
		retour.add(leek);
		retour.add(end);
		ArrayNode pathArray = Json.createArray();
		for (int cell : path) {
			pathArray.add(cell);
		}
		retour.add(pathArray);
		return retour;
	}

}
