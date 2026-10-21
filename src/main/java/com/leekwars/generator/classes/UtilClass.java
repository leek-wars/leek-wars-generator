package com.leekwars.generator.classes;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.regex.Pattern;

import com.leekwars.generator.action.ActionShowCell;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.fight.entity.EntityAI;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.MapLeekValue;

public class UtilClass {

	public static LegacyArrayLeekValue getRegisters_v1_3(EntityAI ai) throws LeekRunException {
		Map<String, String> registers;
		if (ai.getEntity().isSummon()) {
			registers = ai.getEntity().getSummoner().getAllRegisters();
		} else {
			registers = ai.getEntity().getAllRegisters();
		}
		var map = new LegacyArrayLeekValue(ai);
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

	// En points de code, comme les compte le serveur (mb_substr)
	private static final int NOTIFICATION_TITLE_LIMIT = 50;
	private static final int NOTIFICATION_MESSAGE_LIMIT = 200;

	public static boolean sendNotification(EntityAI ai, Object title) throws LeekRunException {
		return sendNotification(ai, title, null);
	}

	/**
	 * Notification pour l'éleveur du poireau qui porte l'IA (celui d'un bulbe est son invocateur),
	 * remise après le combat dans la limite de son quota du jour. Une tourelle ou un mob n'a pas
	 * d'éleveur à prévenir.
	 */
	public static boolean sendNotification(EntityAI ai, Object title, Object message) throws LeekRunException {
		var entity = ai.getEntity().isSummon() ? ai.getEntity().getSummoner() : ai.getEntity();
		if (entity == null || entity.getType() != Entity.TYPE_LEEK || entity.getFarmer() <= 0) return false;
		// Plafond vérifié avant de convertir : un appel refusé ne coûte rien de plus
		if (title == null || !ai.getFight().canNotify(entity.getFarmer())) return false;
		String titleText = notificationText(ai.string(title), NOTIFICATION_TITLE_LIMIT);
		if (titleText.isEmpty()) return false;
		String messageText = message == null ? "" : notificationText(ai.string(message), NOTIFICATION_MESSAGE_LIMIT);
		ai.getFight().addNotification(entity.getFarmer(), entity.getId(), titleText, messageText);
		return true;
	}

	private static final Pattern NOTIFICATION_BREAKS = Pattern.compile("[\\p{Cc}\\u2028\\u2029]");

	/**
	 * Le texte sur une ligne, comme une notification l'affiche, coupé à `limit` points de code.
	 * Couper d'abord à 2 × limit unités UTF-16 borne le travail, quelle que soit la longueur reçue.
	 * Les caractères de contrôle et les séparateurs de ligne deviennent des espaces, un demi-emoji
	 * laissé par un substring devient « ? » : le texte part en JSON, il doit rester de l'UTF-8 valide.
	 */
	private static String notificationText(String text, int limit) {
		if (text.length() > 2 * limit) text = text.substring(0, 2 * limit);
		text = NOTIFICATION_BREAKS.matcher(new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).replaceAll(" ").strip();
		if (text.codePointCount(0, text.length()) > limit) text = text.substring(0, text.offsetByCodePoints(0, limit)).strip();
		return text;
	}

	public static Object pause(EntityAI ai) {
		ai.getLogs().addPause();
		return null;
	}

	public static boolean mark(EntityAI ai, Object cell) throws LeekRunException {
		return mark(ai, cell, 0x000000, 1);
	}

	public static boolean mark(EntityAI ai, Object cell, long color) throws LeekRunException {
		return mark(ai, cell, color, 1);
	}

	public static boolean mark(EntityAI ai, Object cell, long color, long duration) throws LeekRunException {
		int[] cel = null;
		if (cell instanceof Number) {
			var id = ai.integer(cell);
			if (ai.getState().getMap().getCell(id) == null)
				return false;
			cel = new int[] { ai.integer(cell) };
		} else if (cell instanceof GenericArrayLeekValue) {
			cel = new int[((GenericArrayLeekValue) cell).size()];
			int i = 0;
			var it = ai.iterator(cell);
			while (it.hasNext()) {
				var value = it.next().getValue();
				if (ai.getState().getMap().getCell(ai.integer(value)) == null)
					continue;
				cel[i] = ai.integer(value);
				i++;
			}
			if (i == 0)
				return false;
		} else
			return false;

		ai.getLogs().addCell(cel, (int) color, (int) duration);

		return true;
	}

	public static Object clearMarks(EntityAI ai) throws LeekRunException {
		ai.getLogs().addClearCells();
		return null;
	}

	// public static boolean markText(EntityAI ai, MapLeekValue cells) throws LeekRunException {
	// 	return markText(ai, cells, 0xffffff, 1);
	// }

	// public static boolean markText(EntityAI ai, MapLeekValue cells, long color) throws LeekRunException {
	// 	return markText(ai, cells, color, 1);
	// }

	// public static boolean markText(EntityAI ai, MapLeekValue cells, long color, long duration) throws LeekRunException {
	// 	for (var entry : (MapLeekValue) cells) {
	// 		var cellText = ai.string(entry.getValue());
	// 		String finalText = cellText.substring(0, Math.min(cellText.length(), 10));
	// 		ai.getLogs().addCellText(new int[] { ai.integer(entry.getKey()) }, finalText, (int) color, (int) duration);
	// 	}
	// 	return true;
	// }

	public static boolean markText(EntityAI ai, Object cell) throws LeekRunException {
		return markText(ai, cell, "X", 0xffffff, 1);
	}
	public static boolean markText(EntityAI ai, Object cell, Object text) throws LeekRunException {
		return markText(ai, cell, text, 0xffffff, 1);
	}
	public static boolean markText(EntityAI ai, Object cell, Object text, long color) throws LeekRunException {
		return markText(ai, cell, text, color, 1);
	}
	public static boolean markText(EntityAI ai, Object cell, Object text, long color, long duration) throws LeekRunException {
		// Pour une map, on affiche les textes de chaque cellule (map<cell, text>)
		if (cell instanceof MapLeekValue) {
			for (var entry : (MapLeekValue) cell) {
				var cellText = ai.string(entry.getValue());
				String finalText = cellText.substring(0, Math.min(cellText.length(), 10));
				ai.getLogs().addCellText(new int[] { ai.integer(entry.getKey()) }, finalText, (int) color, (int) duration);
			}
			return true;
		}

		int[] cel = null;
		if (cell instanceof Number) {
			var id = ai.integer(cell);
			if (ai.getState().getMap().getCell(id) == null) {
				return false;
			}
			cel = new int[] { ai.integer(cell) };
		} else if (cell instanceof GenericArrayLeekValue) {
			cel = new int[((GenericArrayLeekValue) cell).size()];
			int i = 0;
			var it = ai.iterator(cell);
			while (it.hasNext()) {
				var value = it.next().getValue();
				if (ai.getState().getMap().getCell(ai.integer(value)) == null)
					continue;
				cel[i] = ai.integer(value);
				i++;
			}
			if (i == 0)
				return false;
		} else {
			return false;
		}
		String userText = ai.string(text);
		String finalText = userText.substring(0, Math.min(userText.length(), 10));

		ai.getLogs().addCellText(cel, finalText, (int) color, (int) duration);

		return true;
	}

	public static boolean show(EntityAI ai, long cell) throws LeekRunException {
		return show(ai, cell, 0xffffff);
	}

	public static boolean show(EntityAI ai, long cell, long color) throws LeekRunException {

		if (ai.getState().getMap().getCell((int) cell) == null)
			return false;

		if (ai.getEntity().getTP() < 1) {
			return false;
		}
		if (ai.getEntity().showsTurn >= Entity.SHOW_LIMIT_TURN) {
			return false;
		}
		ai.getEntity().useTP(1);
		ai.getEntity().showsTurn++;

		ai.getFight().log(new ActionShowCell((int) cell, (int) color));
		ai.getFight().getState().statistics.show(ai.getEntity(), (int) cell);

		return true;
	}
}
