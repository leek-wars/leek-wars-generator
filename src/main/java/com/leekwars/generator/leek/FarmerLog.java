package com.leekwars.generator.leek;

import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.Util;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.Actions;
import com.leekwars.generator.fight.entity.Entity;

import leekscript.AILog;

public class FarmerLog extends AILog {

	private final static int MAX_LENGTH = 500000;

	private final JSONObject mObject;
	private Actions mLogs;
	private int mAction = -1;
	private int mNb = 0;
	private JSONArray mCurArray;
	private int mSize = 0;
	private Fight fight;
	private int farmer;

	public final static int MARK = 4;
	public final static int PAUSE = 5;
	public final static int MARK_TEXT = 9;
	public final static int CLEAR_CELLS = 10;
	public final static int TOO_MUCH_DEBUG = 11;

	public static final int NO_WEAPON_EQUIPPED = 1000;
	public static final int CHIP_NOT_EQUIPPED = 1001;
	public static final int CHIP_NOT_EXISTS = 1002;
	public static final int WEAPON_NOT_EXISTS = 1003;
	public static final int WEAPON_NOT_EQUIPPED = 1004;
	public static final int BULB_WITHOUT_AI = 1005;

	private boolean tooMuchDebug = false;

	public FarmerLog(Fight fight, int farmer) {
		super();
		mObject = new JSONObject();
		this.fight = fight;
		this.farmer = farmer;
	}

	public void setLogs(Actions logs) {
		mLogs = logs;
	}

	public void addAction(JSONArray action) {
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
		addAction(obj);
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
		addAction(obj);
	}

	public void addClearCells(Entity leek) {

		if (!addSize(8)) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(CLEAR_CELLS);
		addAction(obj);
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
		addAction(obj);
	}

	public void addLog(Entity leek, int type, String message) {

		addLog(leek, type, message, 0);
	}

	public void addLog(Entity leek, int type, String message, int color) {

		if (message == null) return;

		if (!tooMuchDebug && mSize != MAX_LENGTH && mSize + 20 + message.length() > MAX_LENGTH) {
			// On peut couper le message pour le faire tenir dans la limite restante
			message = message.substring(0, Math.max(0, MAX_LENGTH - (mSize + 20 + 6))) + " [...]";
		}
		if (!addSize(20 + message.length())) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(type);
		obj.add(new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
		if (color != 0) {
			obj.add(color);
		}
		addAction(obj);
	}

	public boolean addSize(int size) {
		if (mSize + size > MAX_LENGTH) {
			if (!tooMuchDebug) {
				fight.statistics.tooMuchDebug(farmer);

				// Message : trop de logs
				JSONArray obj = new JSONArray();
				obj.add(0);
				obj.add(TOO_MUCH_DEBUG);
				addAction(obj);

				tooMuchDebug = true;
				mSize = MAX_LENGTH;
			}
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
		addAction(obj);
	}
}
