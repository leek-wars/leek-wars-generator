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

	// Clés
	public static final String NO_WEAPON_EQUIPED = "no_weapon_equipped";
	public static final String CHIP_NOT_EQUIPED = "chip_not_equipped";
	public static final String CHIP_NOT_EXISTS = "chip_not_exists";
	public static final String DEPRECATED_FUNCTION = "deprecated_function";
	public static final String UNKNOWN_FUNCTION = "unknown_function";
	public static final String DIVISION_BY_ZERO = "division_by_zero";
	public static final String CAN_NOT_EXECUTE_VALUE = "can_not_execute_value";
	public static final String CAN_NOT_EXECUTE_WITH_ARGUMENTS = "can_not_execute_with_arguments";
	public static final String NO_AI_EQUIPPED = "no_ai_equipped";
	public static final String CAN_NOT_COMPILE_AI = "can_not_compile_ai";
	public static final String AI_DISABLED = "ai_disabled";
	public static final String AI_INTERRUPTED = "ai_interrupted";
	public static final String AI_TIMEOUT = "ai_timeout";
	public static final String CODE_TOO_LARGE = "code_too_large";
	public static final String CODE_TOO_LARGE_FUNCTION = "code_too_large_function";
	public static final String NUMBER_OF_OPERATIONS = "number_of_operations";

	public FarmerLog() {
		super();
		mObject = new JSONObject();
	}

	public void setLogs(Actions logs) {
		mLogs = logs;
	}

	public void addAction(Entity entity, JSONArray action) {
		action.set(0, entity.getFId());
		int id = mLogs == null ? 0 : mLogs.getNextId();
		if (mAction < id) {
			mCurArray = new JSONArray();
			mObject.put(String.valueOf(id), mCurArray);
			mAction = id;
		}
		mNb++;
		mCurArray.add(action);
	}

	public void addSystemLog(Entity leek, int type, String trace, String key, String[] parameters) {
		int paramSize = 0;
		if (parameters != null) {
			for (String p : parameters) {
				paramSize += p.length();
			}
		}
		if (!addSize(20 + trace.length() + key.length() + paramSize)) {
			return;
		}
		JSONArray obj = new JSONArray();
		obj.add(leek.getFId());
		obj.add(type);
		obj.add(trace);
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
