package com.leekwars.generator.leek;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.action.Actions;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.Util;
import com.leekwars.generator.fight.Fight;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public class FarmerLog {

	private final static int MAX_LENGTH = 500000;

	private final ObjectNode mObject;
	private Actions mLogs;
	private int mAction = -1;
	private int mNb = 0;
	private ArrayNode mCurArray;
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
		mObject = Json.createObject();
		this.fight = fight;
		this.farmer = farmer;
	}

	public void setLogs(Actions logs) {
		mLogs = logs;
	}

	public void addAction(ArrayNode action) {
		int id = mLogs == null ? 0 : Math.max(0, mLogs.getNextId() - 1);
		if (mAction < id) {
			mCurArray = Json.createArray();
			mObject.set(String.valueOf(id), mCurArray);
			mAction = id;
		}
		mNb++;
		mCurArray.add(action);
	}

	public void addSystemLog(AI ai, int type, String error, int key, Object[] parameters) throws LeekRunException {
		throw new RuntimeException("not implemented");
	}

	public void addSystemLog(AI ai, Entity leek, int type, String error, int key, Object[] parameters) throws LeekRunException {

		if (!addSize(20)) {
			return;
		}

		String[] parametersString = parameters != null ? new String[parameters.length] : null;

		if (parameters != null) {
			for (int p = 0; p < parameters.length; ++p) {
				var parameterString = ai.string(parameters[p]);
				if (!addSize(parameterString.length())) {
					parametersString[p] = "[...]";
				} else {
					parametersString[p] = parameterString;
				}
			}
		}
		ArrayNode obj = Json.createArray();
		obj.add(leek.getFId());
		obj.add(type);
		obj.add(error);
		obj.add(key);
		if (parameters != null) {
			ArrayNode paramsArray = Json.createArray();
			for (String param : parametersString) {
				paramsArray.add(param);
			}
			obj.add(paramsArray);
		}
		addAction(obj);
	}

	public void addSystemLogString(Entity leek, int type, String error, int key, String[] parameters) {
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
		ArrayNode obj = Json.createArray();
		obj.add(leek.getFId());
		obj.add(type);
		obj.add(error);
		obj.add(key);
		if (parameters != null) {
			ArrayNode paramsArray = Json.createArray();
			for (String param : parameters) {
				paramsArray.add(param);
			}
			obj.add(paramsArray);
		}
		addAction(obj);
	}

	public void addCell(Entity leek, int[] cells, int color, int duration) {

		if (!addSize(cells.length * 5 + 8)) {
			return;
		}
		ArrayNode obj = Json.createArray();
		obj.add(leek.getFId());
		obj.add(MARK);
		ArrayNode cellsArray = Json.createArray();
		for (int cell : cells) {
			cellsArray.add(cell);
		}
		obj.add(cellsArray);
		obj.add(Util.getHexaColor(color));
		obj.add(duration);
		addAction(obj);
	}

	public void addClearCells(Entity leek) {

		if (!addSize(8)) {
			return;
		}
		ArrayNode obj = Json.createArray();
		obj.add(leek.getFId());
		obj.add(CLEAR_CELLS);
		addAction(obj);
	}

	public void addCellText(Entity leek, int[] cells, String text, int color, int duration) {

		if (!addSize(cells.length * 5 + 8 + text.length())) {
			return;
		}
		ArrayNode obj = Json.createArray();
		obj.add(leek.getFId());
		obj.add(MARK_TEXT);
		ArrayNode cellsArray = Json.createArray();
		for (int cell : cells) {
			cellsArray.add(cell);
		}
		obj.add(cellsArray);
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

		var ai = (AI) leek.getAI();
		var position = ai.getCurrentLeekScriptPosition();

		ArrayNode obj = Json.createArray();
		obj.add(leek.getFId());
		obj.add(type);
		obj.add(message);
		if (color != 0 || position != null) {
			obj.add(color);
		}
		if (position != null) {
			obj.add(position.file());
			obj.add(position.line());
		}
		
		addAction(obj);
	}

	public boolean addSize(int size) {
		if (mSize + size > MAX_LENGTH) {
			if (!tooMuchDebug) {
				fight.getState().statistics.tooMuchDebug(farmer);

				// Message : trop de logs
				ArrayNode obj = Json.createArray();
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

	public ObjectNode toJSON() {
		return mObject;
	}

	public void addPause(Entity leek) {
		if (!addSize(10)) {
			return;
		}
		ArrayNode obj = Json.createArray();
		obj.add(leek.getFId());
		obj.add(PAUSE);
		addAction(obj);
	}

	public boolean isFull() {
		return mSize >= MAX_LENGTH;
	}
}
