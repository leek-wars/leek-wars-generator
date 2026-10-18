package com.leekwars.generator.classes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import com.leekwars.generator.Generator;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.component.Components;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.state.FightLoadout;
import com.leekwars.generator.weapons.Weapons;

import leekscript.AILog;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.util.Json;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Équipements (« loadouts ») écrits en JSON dans les fichiers du joueur, au format d'export de
 * la page du poireau : les items y sont désignés par leur nom (`weapon_m_laser`, `chip_mutation`,
 * `core3`, ou `m_laser` sans préfixe) ou par leur id. `getLoadout("builds/tank.json")` reçoit le
 * CONTENU du fichier, lu à la compilation (LeekFunctions.setFileArgument), et rend les items
 * convertis en ids : ceux des constantes WEAPON_* et CHIP_*, le template pour les composants.
 */
public class LoadoutClass {

	private static final int NAME_MAX_LENGTH = 60;
	private static final Pattern DIGITS = Pattern.compile("\\d{1,9}");
	private static final List<String> STATS = List.of("life", "strength", "wisdom", "agility", "resistance", "science", "magic", "frequency", "tp", "mp", "cores", "ram");

	public static Object getLoadout(EntityAI ai, String json) throws LeekRunException {
		var loadouts = readLoadouts(ai, json);
		return loadouts == null ? null : LeekValueManager.parseJSON(loadouts, ai);
	}

	/**
	 * Le ou les équipements d'un JSON, items convertis en ids (un objet, ou un tableau si le JSON en
	 * décrit plusieurs) ; null, avec une erreur au rapport, s'il n'en décrit aucun. Partagé par
	 * getLoadout en LeekScript et en JS / Python (rendu en JSON à ces derniers).
	 */
	public static JsonNode readLoadouts(EntityAI ai, String json) throws LeekRunException {
		ai.ops((long) json.length() * 10);
		JsonNode data;
		try {
			data = Json.parse(json);
		} catch (Exception e) {
			ai.getLogs().addLog(AILog.ERROR, "getLoadout : JSON invalide");
			return null;
		}
		if (data != null && data.isObject() && data.has("loadouts") && data.get("loadouts").isArray()) {
			data = data.get("loadouts");
		}
		if (data == null || !(data.isObject() || data.isArray())) {
			ai.getLogs().addLog(AILog.ERROR, "getLoadout : aucun équipement dans ce JSON");
			return null;
		}
		var unknown = new ArrayList<String>();
		JsonNode result;
		try {
			if (data.isArray()) {
				var array = Json.createArray();
				for (var entry : data) {
					if (entry.isObject()) array.add(normalize(entry, unknown));
				}
				result = array;
			} else {
				result = normalize(data, unknown);
			}
		} catch (RuntimeException e) {
			ai.getLogs().addLog(AILog.ERROR, "getLoadout : équipement illisible");
			return null;
		}
		if (!unknown.isEmpty()) {
			ai.getLogs().addLog(AILog.WARNING, "getLoadout : objets inconnus ignorés : " + String.join(", ", unknown));
		}
		return result;
	}

	/** Équipement construit en code, et s'il porte une répartition de capital (clé stats). */
	public record ObjectLoadout(FightLoadout loadout, boolean withStats) {}

	// Emplacements de composants, comme au début du combat (Leek.create côté serveur) : les modèles
	// 406 et 407 en prennent trois.
	private static final int MAX_COMPONENTS = 8;
	private static final java.util.Set<Integer> TRIPLE_SLOT_COMPONENTS = java.util.Set.of(406, 407);

	/**
	 * setLoadout(objet) : équipement construit en code (au format de getLoadout, ids ou noms),
	 * résolu par les règles du serveur au niveau du poireau. Un exemplaire par poireau : ce que
	 * l'éleveur n'a plus de libre (ses autres entités du combat le portent) est retiré, avec un
	 * avertissement. null si ce n'est pas un équipement, ou si les règles ou l'inventaire manquent.
	 */
	public static ObjectLoadout loadoutFromObject(EntityAI ai, JsonNode source) throws LeekRunException {
		var rules = Generator.getLoadoutRules();
		var available = ai.getFight().getState().availableCopies(ai.getEntity());
		if (rules == null || available == null) {
			ai.getLogs().addLog(AILog.WARNING, "setLoadout : un équipement en objet n'est pas disponible dans ce combat");
			return null;
		}
		var loadouts = readLoadouts(ai, source.toString());
		if (loadouts == null) return null;
		if (!loadouts.isObject()) {
			ai.getLogs().addLog(AILog.ERROR, "setLoadout : un seul équipement à la fois");
			return null;
		}
		var loadout = (ObjectNode) loadouts;
		var missing = new ArrayList<String>();
		var taken = new java.util.HashMap<Integer, Integer>();
		IntPredicate take = template -> {
			if (taken.getOrDefault(template, 0) >= available.getOrDefault(template, 0)) {
				missing.add(String.valueOf(template));
				return false;
			}
			taken.merge(template, 1, Integer::sum);
			return true;
		};
		// Une arme oubliée n'est qu'une candidate (une seule portée par poireau, unique pour l'éleveur) :
		// placée parmi les armes, elle rejoint les candidates, comme à l'enregistrement d'un ensemble.
		var weapons = Json.createArray();
		var forgotten = Json.createArray();
		for (var id : loadout.get("weapons")) {
			if (Weapons.getWeapon(id.asInt()).isForgotten()) forgotten.add(id);
			else if (take.test(id.asInt())) weapons.add(id);
		}
		for (var id : loadout.get("forgotten_weapons")) {
			if (taken.getOrDefault(id.asInt(), 0) < available.getOrDefault(id.asInt(), 0)) forgotten.add(id);
			else missing.add(id.asString());
		}
		loadout.set("weapons", weapons);
		loadout.set("forgotten_weapons", forgotten);
		var chips = Json.createArray();
		for (var id : loadout.get("chips")) if (take.test(id.asInt())) chips.add(id);
		loadout.set("chips", chips);
		var components = Json.createArray();
		int slots = MAX_COMPONENTS;
		for (var c : loadout.get("components")) {
			int template = c.get("item").asInt();
			if (TRIPLE_SLOT_COMPONENTS.contains(template)) slots -= 2;
			if (components.size() >= slots) {
				missing.add(String.valueOf(template));
				continue;
			}
			if (take.test(template)) components.add(c);
		}
		loadout.set("components", components);
		if (!missing.isEmpty()) {
			ai.getLogs().addLog(AILog.WARNING, "setLoadout : objets non disponibles pour ce poireau, ignorés : " + String.join(", ", missing));
		}
		var resolved = rules.resolve(loadout, ai.getEntity().getLevel());
		return resolved == null ? null : new ObjectLoadout(resolved, source.isObject() && source.has("stats"));
	}

	/**
	 * Objets dont ce poireau dispose pour un équipement construit en code : les exemplaires de son
	 * éleveur que ses autres entités du combat ne portent pas, et son capital. null si l'inventaire
	 * n'est pas connu dans ce combat.
	 */
	public static Object getInventory(EntityAI ai) throws LeekRunException {
		var inventory = readInventory(ai);
		return inventory == null ? null : LeekValueManager.parseJSON(inventory, ai);
	}

	/** getInventory en JSON (rendu tel quel aux IA JS / Python). */
	public static JsonNode readInventory(EntityAI ai) throws LeekRunException {
		var entity = ai.getEntity();
		var available = ai.getFight().getState().availableCopies(entity);
		if (available == null) return null;
		ai.ops(10 + available.size());
		var weapons = Json.createArray();
		var chips = Json.createArray();
		var components = Json.createArray();
		for (var entry : new java.util.TreeMap<>(available).entrySet()) {
			int template = entry.getKey();
			ArrayNode list = Weapons.getWeapon(template) != null ? weapons
				: Chips.getChip(template) != null ? chips
				: Components.getComponent(template) != null ? components : null;
			if (list == null || entry.getValue() <= 0) continue;
			var item = Json.createObject();
			item.put("item", template);
			item.put("quantity", entry.getValue());
			if (list == weapons && Weapons.getWeapon(template).isForgotten()) item.put("forgotten", true);
			list.add(item);
		}
		var out = Json.createObject();
		out.set("weapons", weapons);
		out.set("chips", chips);
		out.set("components", components);
		var rules = Generator.getLoadoutRules();
		out.put("capital", rules == null ? 0 : rules.totalCapital(entity.getLevel()));
		return out;
	}

	private static ObjectNode normalize(JsonNode source, List<String> unknown) {
		var out = Json.createObject();
		// Même lecture que l'import d'un ensemble sur le site (client, model/loadout.ts).
		var name = source.path("name").isString() ? source.get("name").asString().trim() : "";
		if (name.codePointCount(0, name.length()) > NAME_MAX_LENGTH) name = name.substring(0, name.offsetByCodePoints(0, NAME_MAX_LENGTH));
		out.put("name", name);
		out.set("weapons", items(source.get("weapons"), LoadoutClass::weapon, unknown));
		out.set("forgotten_weapons", items(source.get("forgotten_weapons"), LoadoutClass::weapon, unknown));
		out.set("chips", items(source.get("chips"), LoadoutClass::chip, unknown));

		var components = Json.createArray();
		var usedSlots = new HashSet<Integer>();
		var usedTemplates = new HashSet<Integer>();
		if (source.path("components").isArray()) {
			for (var c : source.get("components")) {
				if (!c.isObject()) continue;
				var raw = c.has("item") ? c.get("item") : c.has("template") ? c.get("template") : c.get("name");
				var template = component(raw);
				if (template == null) { unknown.add(label(raw)); continue; }
				// Un modèle ne s'équipe qu'une fois.
				if (!usedTemplates.add(template)) continue;
				var entry = Json.createObject();
				var slot = c.has("slot") ? c.get("slot") : c.get("index");
				int s = slot != null && slot.isNumber() && slot.canConvertToInt() ? slot.asInt() : -1;
				if (s < 0 || s > 7 || usedSlots.contains(s)) {
					s = 0;
					while (usedSlots.contains(s)) s++;
				}
				if (s > 7) continue;
				usedSlots.add(s);
				entry.put("slot", s);
				entry.put("item", template);
				var stats = Json.createObject();
				if (c.path("stats").isObject()) {
					for (var stat : c.get("stats").properties()) {
						var v = stat.getValue();
						double value = toDouble(v);
						if (Double.isFinite(value) && Math.abs(value) < 1e9 && Math.round(value) != 0) stats.put(stat.getKey(), (int) Math.round(value));
					}
				}
				entry.set("stats", stats);
				components.add(entry);
			}
		}
		out.set("components", components);

		var stats = Json.createObject();
		var sourceStats = source.path("stats");
		for (var stat : STATS) {
			var value = sourceStats.path(stat);
			double capital = toDouble(value);
			stats.put(stat, Double.isFinite(capital) ? (int) Math.max(0, Math.min(1e9, Math.floor(capital))) : 0);
		}
		out.set("stats", stats);
		return out;
	}

	private static ArrayNode items(JsonNode list, Function<JsonNode, Integer> resolver, List<String> unknown) {
		var out = Json.createArray();
		if (list == null || !list.isArray()) return out;
		var seen = new HashSet<Integer>();
		for (var raw : list) {
			var id = resolver.apply(raw);
			if (id == null) unknown.add(label(raw));
			else if (seen.add(id)) out.add(id);
		}
		return out;
	}

	private static String label(JsonNode raw) {
		return raw == null ? "null" : raw.isString() ? raw.asString() : raw.toString();
	}

	/** Nombre, ou chaîne qui en écrit un (comme Number() côté client) ; NaN sinon. */
	private static double toDouble(JsonNode value) {
		if (value == null) return Double.NaN;
		if (value.isNumber()) return value.asDouble();
		if (!value.isString()) return Double.NaN;
		try {
			return Double.parseDouble(value.asString().trim());
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	/**
	 * Id d'un item désigné par son id (nombre ou chaîne de chiffres) ou par son nom, avec ou sans
	 * préfixe (`weapon_m_laser` ou `m_laser`). Null si inconnu.
	 */
	private static Integer resolve(JsonNode raw, String prefix, IntPredicate exists, Function<String, Integer> byName) {
		if (raw == null) return null;
		if (raw.isIntegralNumber()) return raw.canConvertToInt() && exists.test(raw.asInt()) ? raw.asInt() : null;
		if (!raw.isString()) return null;
		var name = raw.asString().trim();
		if (DIGITS.matcher(name).matches()) return exists.test(Integer.parseInt(name)) ? Integer.parseInt(name) : null;
		if (prefix != null && name.startsWith(prefix)) name = name.substring(prefix.length());
		return byName.apply(name);
	}

	private static Integer weapon(JsonNode raw) {
		return resolve(raw, "weapon_", id -> Weapons.getWeapon(id) != null, name -> {
			var w = Weapons.getWeapon(name);
			return w == null ? null : w.getId();
		});
	}

	private static Integer chip(JsonNode raw) {
		return resolve(raw, "chip_", id -> Chips.getChip(id) != null, name -> {
			var c = Chips.getChip(name);
			return c == null ? null : c.getId();
		});
	}

	private static Integer component(JsonNode raw) {
		return resolve(raw, null, id -> Components.getComponent(id) != null, name -> {
			var c = Components.getComponent(name);
			return c == null ? null : c.getTemplate();
		});
	}
}
