package com.leekwars.game.fight.action;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.game.fight.entity.Entity;

public class ActionEntityTurn implements Action {

	private final int id;
	private int mTP = 0;
	private int mMP = 0;

	public ActionEntityTurn(Entity leek) {
		if (leek == null)
			this.id = -1;
		else {
			this.id = leek.getFId();
			mTP = leek.getTP();
			mMP = leek.getMP();
		}
	}

	public void setPM(int pm) {
		mMP = pm;
	}

	public void setTP(int tp) {
		mTP = tp;
	}

	@Override
	public JSONArray getJSON() {
		JSONArray retour = new JSONArray();
		retour.add(Action.LEEK_TURN);
		retour.add(id);
		retour.add(mTP);
		retour.add(mMP);
		return retour;
	}
}
