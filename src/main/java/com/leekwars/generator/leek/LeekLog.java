package com.leekwars.generator.leek;

import com.alibaba.fastjson.JSONArray;
import com.leekwars.generator.fight.action.Actions;
import com.leekwars.generator.fight.entity.Entity;
import leekscript.common.Error;

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
				array.set(0, entity.getFId());
				farmerLogs.addAction(array);
			}
		};
	}

	public void addLog(int warning, String string) {
		farmerLogs.addLog(entity, warning, string);
	}


	public void addLog(int type, String message, int color) {
		farmerLogs.addLog(entity, AILog.STANDARD, message, color);
	}

	public void addSystemLog(int type, Error error) {
		addSystemLog(type, "", error.ordinal(), null);
	}

	public void addSystemLog(int type, Error error, String[] parameters) {
		addSystemLog(type, "", error.ordinal(), parameters);
	}

	public void addSystemLog(int type, String trace, int key, String[] parameters) {
		farmerLogs.addSystemLog(entity, type, trace, key, parameters);
	}

	public void setLogs(Actions actions) {
		farmerLogs.setLogs(actions);
	}

	public void addCell(int[] cells, int color, int duration) {
		farmerLogs.addCell(entity, cells, color, duration);
	}

	public void addClearCells() {
		farmerLogs.addClearCells(entity);
	}

	public void addCellText(int[] cells, String text, int color, int duration) {
		farmerLogs.addCellText(entity, cells, text, color, duration);
	}

	public void addPause() {
		farmerLogs.addPause(entity);
	}
}