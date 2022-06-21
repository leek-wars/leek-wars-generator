package com.leekwars.generator.classes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import com.leekwars.generator.fight.action.ActionShowCell;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.MapLeekValue;

public class UtilClass {

	public static String getDate(EntityAI ai) {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		return df.format(ai.getFight().getDate()).toString();
	}

	public static String getTime(EntityAI ai) {
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		return df.format(ai.getFight().getDate()).toString();
	}

	public static long getTimestamp(EntityAI ai) {
		return (ai.getFight().getDate().getTime() / 1000);
	}

	public static LegacyArrayLeekValue getRegisters_v1_3(EntityAI ai) throws LeekRunException {
		Map<String, String> registers;
		if (ai.getEntity().isSummon()) {
			registers = ai.getEntity().getSummoner().getAllRegisters();
		} else {
			registers = ai.getEntity().getAllRegisters();
		}
		var map = new LegacyArrayLeekValue();
		for (var e : registers.entrySet()) {
			map.set(ai, e.getKey(), e.getValue());
		}
		return map;
	}

	public static MapLeekValue getRegisters(EntityAI ai) throws LeekRunException {
		Map<String, String> registers;
		if (ai.getEntity().isSummon()) {
			registers = ai.getEntity().getSummoner().getAllRegisters();
		} else {
			registers = ai.getEntity().getAllRegisters();
		}
		var map = new MapLeekValue(ai);
		for (var e : registers.entrySet()) {
			map.set(ai, e.getKey(), e.getValue());
		}
		return map;
	}

	public static String getRegister(EntityAI ai, Object key) throws LeekRunException {
		String keyString = ai.string(key);
		String register;
		if (ai.getEntity().isSummon()) {
			register = ai.getEntity().getSummoner().getRegister(keyString);
		} else {
			register = ai.getEntity().getRegister(keyString);
		}
		if (register == null) {
			return null;
		}
		return register;
	}

	public static boolean setRegister(EntityAI ai, Object key, Object value) throws LeekRunException {
		String keyString = ai.string(key);
		String valueString = ai.string(value);
		boolean r;
		if (ai.getEntity().isSummon()) {
			r = ai.getEntity().getSummoner().setRegister(keyString, valueString);
		} else {
			r = ai.getEntity().setRegister(keyString, valueString);
		}
		return r;
	}

	public static Object deleteRegister(EntityAI ai, Object key) throws LeekRunException {
		String keyString = ai.string(key);
		if (ai.getEntity().isSummon()) {
			ai.getEntity().getSummoner().deleteRegister(keyString);
		} else {
			ai.getEntity().deleteRegister(keyString);
		}
		return null;
	}

	public static Object pause(EntityAI ai) {
		ai.getLogs().addPause();
		return null;
	}

	public static Object mark(EntityAI ai, Object cell) throws LeekRunException {
		return mark(ai, cell, null, null);
	}

	public static Object mark(EntityAI ai, Object cell, Object color) throws LeekRunException {
		return mark(ai, cell, color, null);
	}

	public static Object mark(EntityAI ai, Object cell, Object color, Object duration) throws LeekRunException {
		int d = 1;
		int col = 1;
		int[] cel = null;
		if (cell instanceof Number) {
			var id = ai.integer(cell);
			if (ai.getFight().getMap().getCell(id) == null)
				return false;
			cel = new int[] { ai.integer(cell) };
		} else if (cell instanceof GenericArrayLeekValue) {
			cel = new int[((GenericArrayLeekValue) cell).size()];
			int i = 0;
			var it = ai.iterator(cell);
			while (it.hasNext()) {
				var value = it.next().getValue();
				if (ai.getFight().getMap().getCell(ai.integer(value)) == null)
					continue;
				cel[i] = ai.integer(value);
				i++;
			}
			if (i == 0)
				return false;
		} else
			return false;

		if (color instanceof Number)
			col = ai.integer(color);
		if (duration instanceof Number)
			d = ai.integer(duration);

		ai.getLogs().addCell(cel, col, d);

		return true;
	}

	public static Object clearMarks(EntityAI ai) throws LeekRunException {
		ai.getLogs().addClearCells();
		return null;
	}

	public static Object markText(EntityAI ai, Object cell, Object text) throws LeekRunException {
		return markText(ai, cell, text, null, null);
	}
	public static Object markText(EntityAI ai, Object cell, Object text, Object color) throws LeekRunException {
		return markText(ai, cell, text, color, null);
	}
	public static Object markText(EntityAI ai, Object cell, Object text, Object color, Object duration) throws LeekRunException {
		int d = 1;
		int col = 0xffffff;
		int[] cel = null;
		if (cell instanceof Number) {
			var id = ai.integer(cell);
			if (ai.getFight().getMap().getCell(id) == null)
				return false;
			cel = new int[] { ai.integer(cell) };
		} else if (cell instanceof GenericArrayLeekValue) {
			cel = new int[((GenericArrayLeekValue) cell).size()];
			int i = 0;
			var it = ai.iterator(cell);
			while (it.hasNext()) {
				var value = it.next().getValue();
				if (ai.getFight().getMap().getCell(ai.integer(value)) == null)
					continue;
				cel[i] = ai.integer(value);
				i++;
			}
			if (i == 0)
				return false;
		} else
			return false;

		if (color instanceof Number)
			col = ai.integer(color);
		if (duration instanceof Number)
			d = ai.integer(duration);

		String userText = ai.string(text);
		String finalText = userText.substring(0, Math.min(userText.length(), 10));

		ai.getLogs().addCellText(cel, finalText, col, d);

		return true;
	}

	public static Object show(EntityAI ai, Object cell) throws LeekRunException {
		return show(ai, cell, null);
	}

	public static Object show(EntityAI ai, Object cell, Object color) throws LeekRunException {
		int cell_id = 1;
		int col = 0xFFFFFF;
		if (cell instanceof Number)
			cell_id = ((Number) cell).intValue();
		else
			return false;
		if (ai.getFight().getMap().getCell(cell_id) == null)
			return false;

		if (color instanceof Number)
			col = ((Number) color).intValue();

		if (ai.getEntity().getTP() < 1) {
			return false;
		}
		if (ai.getEntity().showsTurn >= Entity.SHOW_LIMIT_TURN) {
			return false;
		}
		ai.getEntity().useTP(1);
		ai.getEntity().showsTurn++;

		ai.getFight().log(new ActionShowCell(cell_id, col));
		ai.getFight().statistics.show(ai.getEntity(), cell_id);

		return true;
	}
}
