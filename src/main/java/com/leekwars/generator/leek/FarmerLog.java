package com.leekwars.generator.leek;

import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.Util;
import com.leekwars.generator.fight.action.Actions;
import com.leekwars.generator.fight.entity.Entity;

import leekscript.AILog;

public class FarmerLog extends AILog {

	private final JSONObject mObject;
	private Actions mLogs;
	private int mAction = -1;
	private int mNb = 0;
	private JSONArray mCurArray;
	private int mSize = 0;
	private final static int MAX_LENGTH = 500000;

	public final static int MARK = 4;
	public final static int PAUSE = 5;
	public final static int MARK_TEXT = 9;
	public final static int CLEAR_CELLS = 10;

	public static final int NO_WEAPON_EQUIPED = 1000;
	public static final int CHIP_NOT_EQUIPED = 1001;
	public static final int CHIP_NOT_EXISTS = 1002;

	public FarmerLog() {
		super();
		mObject = new JSONObject();
	}

	public void setLogs(Actions logs) {
		mLogs = logs;
	}

	public void addAction(Entity entity, JSONArray action) {
		action.set(0, entity.getFId());
		int id = mLogs == null ? 0 : Math.max(0, mLogs.getNextId() - 1);
		if (mAction < id) {
			mCurArray = new JSONArray();
			mObject.put(String.valueOf(id), mCurArray);
			mAction = id;
		}
		mNb++;
		mCurArray.add(action);
	}

	public void addSystemLog(Entity leek, int type, String error, int key, String[] parameters) {
		int paramSize = 0;
		if (parameters != null) {
			for (String p : parameters) {
				if (p != null) {
					paramSize += p.length();
				}
			}
		}
		if (!addSize(20 + paramSize)) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(type);
		obj.add(error);
		obj.add(key);
		if (parameters != null)
			obj.add(parameters);
		addAction(leek, obj);
	}

	public void addCell(Entity leek, int[] cells, int color, int duration) {

		if (!addSize(cells.length * 5 + 8)) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(MARK);
		obj.add(cells);
		obj.add(Util.getHexaColor(color));
		obj.add(duration);
		addAction(leek, obj);
	}

	public void addClearCells(Entity leek) {

		if (!addSize(8)) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(CLEAR_CELLS);
		addAction(leek, obj);
	}

	public void addCellText(Entity leek, int[] cells, String text, int color, int duration) {

		if (!addSize(cells.length * 5 + 8 + text.length())) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(MARK_TEXT);
		obj.add(cells);
		obj.add(text);
		obj.add(Util.getHexaColor(color));
		obj.add(duration);
		addAction(leek, obj);
	}

	public void addLog(Entity leek, int type, String message) {

		addLog(leek, type, message, 0);
	}

	public void addLog(Entity leek, int type, String message, int color) {

		if (message == null || !addSize(20 + message.length())) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(type);
		obj.add(new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
		if (color != 0) {
			obj.add(color);
		}
		addAction(leek, obj);
	}

	public boolean addSize(int size) {
		if (mSize + size > MAX_LENGTH) {
			return false;
		}
		mSize += size;
		return true;
	}

	public int size() {
		return mNb;
	}

	public JSONObject toJSON() {
		return mObject;
	}

	public void addPause(Entity leek) {
		if (!addSize(10)) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(PAUSE);
		addAction(leek, obj);
	}
}
