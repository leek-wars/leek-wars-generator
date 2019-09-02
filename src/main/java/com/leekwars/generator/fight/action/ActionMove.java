package com.leekwars.generator.fight.action;

import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.maps.Cell;

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
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.MOVE_TO);
		retour.add(leek);
		retour.add(end);
		retour.add(path);
		return retour;
	}

}
