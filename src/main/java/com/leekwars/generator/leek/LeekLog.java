package com.leekwars.generator.leek;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.action.Actions;
import com.leekwars.generator.fight.entity.Entity;

import leekscript.AILog;

public class LeekLog extends AILog {

	Entity entity;
	FarmerLog farmerLogs;

	public LeekLog(FarmerLog farmerLogs, Entity entity) {
		this.farmerLogs = farmerLogs;
		this.entity = entity;
		stream = new AILog.Stream() {
			@Override
			public void write(JSONArray array) {
				farmerLogs.addAction(entity, array);
			}
		};
	}

	public void addLog(int warning, String string) {
		farmerLogs.addLog(entity, warning, string);
	}

	public void addSystemLog(int type, String errorMessage, String key, String[] parameters) {
		farmerLogs.addSystemLog(entity, type, errorMessage, key, parameters);
	}

	public void setLogs(Actions actions) {
		farmerLogs.setLogs(actions);
	}

	public void addCell(int[] cells, int color, int duration) {
		farmerLogs.addCell(entity, cells, color, duration);
	}

	public void addCellText(int[] cells, String text, int color, int duration) {
		farmerLogs.addCellText(entity, cells, text, color, duration);
	}

	public void addPause() {
		farmerLogs.addPause(entity);
	}
}