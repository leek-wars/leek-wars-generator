package com.leekwars.generator.leek;

import com.leekwars.generator.action.Actions;
import com.leekwars.generator.state.Entity;

import leekscript.common.Error;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.AILog;

public class LeekLog extends AILog {

	Entity entity;
	FarmerLog farmerLogs;

	public LeekLog(FarmerLog farmerLogs, Entity entity) {
		this.farmerLogs = farmerLogs;
		this.entity = entity;
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
		farmerLogs.addSystemLogString(entity, type, trace, key, parameters);
	}

	@Override
	public void addSystemLog(AI ai, int type, String trace, int key, Object[] parameters) throws LeekRunException {
		farmerLogs.addSystemLog(ai, entity, type, trace, key, parameters);
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

	@Override
	public boolean isFull() {
		return farmerLogs.isFull();
	}

	@Override
	public void setStream(Stream stream) {

	}
}