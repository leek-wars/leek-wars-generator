package com.leekwars.generator.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import com.leekwars.generator.Generator;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.component.Components;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.items.Items;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.state.ComponentInstance;
import com.leekwars.generator.state.Entity;
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
		var data = parse(ai, json);
		return data == null ? null : readLoadouts(ai, data);
	}

	private static JsonNode parse(EntityAI ai, String json) throws LeekRunException {
		ai.ops((long) json.length() * 10);
		try {
			return Json.parse(json);
		} catch (Exception e) {
			ai.getLogs().addLog(AILog.ERROR, "getLoadout : JSON invalide");
			return null;
		}
	}

	private static JsonNode readLoadouts(EntityAI ai, JsonNode data) {
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

	/**
	 * Le premier équipement d'un JSON, au format de normalize, sans IA à qui rapporter les erreurs :
	 * null si le JSON est invalide ou n'en décrit aucun. Sert au worker pour l'équipement d'un
	 * fichier posé sur un poireau de scénario de test.
	 */
	public static ObjectNode firstLoadout(String json) {
		JsonNode data;
		try {
			data = Json.parse(json);
		} catch (Exception e) {
			return null;
		}
		if (data != null && data.isObject() && data.path("loadouts").isArray()) {
			data = data.get("loadouts");
		}
		if (data != null && data.isArray()) {
			JsonNode first = null;
			for (var entry : data) {
				if (entry.isObject()) { first = entry; break; }
			}
			data = first;
		}
		if (data == null || !data.isObject()) return null;
		try {
			return normalize(data, new ArrayList<>());
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Équipement construit en code, et s'il porte une répartition de capital (clé stats). */
	public record ObjectLoadout(FightLoadout loadout, boolean withStats) {}

	// Emplacements de composants, comme au début du combat (Leek.create côté serveur) : les modèles
	// 406 et 407 en prennent trois.
	private static final int MAX_COMPONENTS = 8;
	private static final Set<Integer> TRIPLE_SLOT_COMPONENTS = Set.of(406, 407);

	/**
	 * Exemplaires libres pour un poireau (State#availableCopies), pris un à un par son équipement ;
	 * ce qui manque est noté pour l'avertissement.
	 */
	private static final class Copies {
		private final Map<Integer, Integer> available;
		private final Map<Integer, Integer> taken = new HashMap<>();
		private final List<String> missing = new ArrayList<>();
		private int slots = 0;

		Copies(Map<Integer, Integer> available) {
			this.available = available;
		}

		/** Un exemplaire encore libre ; pour une arme oubliée, une candidate qui n'en prend pas (le poireau n'en porte qu'une). */
		boolean candidate(int template) {
			if (taken.getOrDefault(template, 0) < available.getOrDefault(template, 0)) return true;
			missing.add(itemName(template));
			return false;
		}

		boolean take(int template) {
			if (!candidate(template)) return false;
			taken.merge(template, 1, Integer::sum);
			return true;
		}

		boolean takeComponent(int template) {
			int size = TRIPLE_SLOT_COMPONENTS.contains(template) ? 3 : 1;
			if (slots + size > MAX_COMPONENTS) {
				missing.add(itemName(template));
				return false;
			}
			if (!take(template)) return false;
			slots += size;
			return true;
		}

		boolean complete() {
			return missing.isEmpty();
		}

		void warn(EntityAI ai, String loadout) {
			if (!missing.isEmpty()) {
				ai.addSystemLog(AILog.WARNING, FarmerLog.LOADOUT_ITEMS_UNAVAILABLE, new String[] { loadout, String.join(", ", missing) });
			}
		}
	}

	private static String itemName(int template) {
		var type = Items.getType(template);
		String name = type == null ? null : switch (type) {
			case Items.TYPE_WEAPON -> Weapons.getWeapon(template).getName();
			case Items.TYPE_CHIP -> Chips.getChip(template).getName();
			case Items.TYPE_COMPONENT -> Components.getComponent(template).getName();
			default -> null;
		};
		return name == null ? String.valueOf(template) : name;
	}

	/**
	 * setLoadout(objet) : équipement construit en code (au format de getLoadout, ids ou noms),
	 * résolu par les règles du serveur au niveau du poireau. Un exemplaire par poireau : ce que
	 * l'éleveur n'a plus de libre (ses autres entités du combat le portent) est retiré, avec un
	 * avertissement. null si ce n'est pas un équipement, ou si les règles ou l'inventaire manquent.
	 */
	public static ObjectLoadout loadoutFromObject(EntityAI ai, JsonNode source, Map<Integer, Integer> worn) throws LeekRunException {
		var rules = Generator.getLoadoutRules();
		var available = rules == null ? null : ai.getFight().getState().availableCopies(ai.getEntity(), worn);
		if (available == null) {
			ai.getLogs().addLog(AILog.WARNING, "setLoadout : un équipement en objet n'est pas disponible dans ce combat");
			return null;
		}
		// Relu depuis son texte : le JSON du moteur peut porter ses sous-objets autrement qu'en nœuds objet.
		var data = parse(ai, source.toString());
		if (data == null) return null;
		var loadouts = readLoadouts(ai, data);
		if (loadouts == null) return null;
		if (!loadouts.isObject()) {
			ai.getLogs().addLog(AILog.ERROR, "setLoadout : un seul équipement à la fois");
			return null;
		}
		var loadout = (ObjectNode) loadouts;
		var copies = new Copies(available);
		// Une arme oubliée n'est qu'une candidate (une seule portée par poireau, unique pour l'éleveur) :
		// placée parmi les armes, elle rejoint les candidates, comme à l'enregistrement d'un ensemble.
		var weapons = Json.createArray();
		var forgotten = Json.createArray();
		for (var id : loadout.get("weapons")) {
			int weapon = id.asInt();
			if (Weapons.getWeapon(weapon).isForgotten()) {
				if (copies.candidate(weapon)) forgotten.add(id);
			} else if (copies.take(weapon)) {
				weapons.add(id);
			}
		}
		for (var id : loadout.get("forgotten_weapons")) {
			if (copies.candidate(id.asInt())) forgotten.add(id);
		}
		var chips = Json.createArray();
		for (var id : loadout.get("chips")) {
			if (copies.take(id.asInt())) chips.add(id);
		}
		var components = Json.createArray();
		for (var c : loadout.get("components")) {
			if (copies.takeComponent(c.get("item").asInt())) components.add(c);
		}
		loadout.set("weapons", weapons);
		loadout.set("forgotten_weapons", forgotten);
		loadout.set("chips", chips);
		loadout.set("components", components);
		copies.warn(ai, loadout.get("name").asString());
		var resolved = rules.resolve(loadout, ai.getEntity().getLevel());
		return resolved == null ? null : new ObjectLoadout(resolved, investsCapital(loadout.get("stats")));
	}

	/**
	 * Une répartition, c'est du capital investi quelque part, lu sur les stats normalisées (clés
	 * connues, valeurs nettoyées). getLoadout rend toujours une clé stats, à zéro quand le fichier
	 * n'en a pas : setLoadout(getLoadout(…)) doit alors garder le capital du poireau, comme un objet
	 * sans stats, au lieu de jouer un capital vide.
	 */
	private static boolean investsCapital(JsonNode stats) {
		for (var stat : stats.properties()) {
			if (stat.getValue().asInt() > 0) return true;
		}
		return false;
	}

	/**
	 * setLoadout(nom) : l'ensemble enregistré, moins ce que l'éleveur n'a plus de libre — un
	 * exemplaire par poireau, comme pour un équipement en objet. Les stats d'un ensemble comptent
	 * celles des composants : un composant retiré emporte les siennes. Les armes oubliées restent
	 * candidates (applyLoadout écarte celles déjà portées). Tel quel si l'inventaire est inconnu.
	 */
	public static FightLoadout keepAvailable(EntityAI ai, FightLoadout loadout, Map<Integer, Integer> worn) {
		var available = ai.getFight().getState().availableCopies(ai.getEntity(), worn);
		if (available == null) return loadout;
		var copies = new Copies(available);
		var weapons = keep(loadout.getWeapons(), copies::take);
		var chips = keep(loadout.getChips(), copies::take);
		var stats = loadout.getStats();
		var components = new ArrayList<FightLoadout.ComponentChoice>();
		for (var choice : loadout.getComponents()) {
			if (copies.takeComponent(choice.template)) {
				components.add(choice);
				continue;
			}
			if (stats == loadout.getStats()) stats = new HashMap<>(stats);
			var component = Components.getComponent(choice.template);
			if (component == null) continue;
			for (var stat : component.getStats().entrySet()) {
				var id = Entity.STAT_NAMES.get(stat.getKey());
				if (id != null) stats.merge(id, -stat.getValue(), Integer::sum);
			}
		}
		if (copies.complete()) return loadout;
		copies.warn(ai, loadout.getName());
		return new FightLoadout(loadout.getName(), weapons, loadout.getForgottenWeapons(), chips, stats,
			loadout.getCapitalStats(), loadout.isOverCapital(), components);
	}

	private static List<Integer> keep(List<Integer> ids, IntPredicate take) {
		if (ids == null) return Collections.emptyList();
		var out = new ArrayList<Integer>();
		for (int id : ids) {
			if (take.test(id)) out.add(id);
		}
		return out;
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
		var copies = ai.getFight().getState().availableCopies(entity);
		if (copies == null) return null;
		var available = new java.util.TreeMap<>(copies); // dans l'ordre des modèles
		// Pièces améliorées par modèle, dans l'ordre des ids : celles du pot et celles qu'il tient.
		var availablePieces = ai.getFight().getState().availablePieces(entity);
		var pieces = availablePieces.stream().sorted(java.util.Comparator.comparingInt(ComponentInstance::getId))
			.collect(java.util.stream.Collectors.groupingBy(ComponentInstance::getTemplate));
		ai.ops(10 + available.size() + availablePieces.size());
		var weapons = Json.createArray();
		var chips = Json.createArray();
		var components = Json.createArray();
		for (var entry : available.entrySet()) {
			int template = entry.getKey();
			var type = Items.getType(template);
			ArrayNode list = type == null ? null : switch (type) {
				case Items.TYPE_WEAPON -> weapons;
				case Items.TYPE_CHIP -> chips;
				case Items.TYPE_COMPONENT -> components;
				default -> null;
			};
			if (list == null || entry.getValue() <= 0) continue;
			var item = Json.createObject();
			item.put("item", template);
			item.put("quantity", entry.getValue());
			if (list == weapons && Weapons.getWeapon(template).isForgotten()) item.put("forgotten", true);
			if (list == components) {
				var component = Components.getComponent(template);
				if (component == null) continue;
				// Stats du modèle (base), puis ce qu'ajoute l'altération de chaque pièce améliorée
				// (alterations) : ces dernières, dans les stats d'un composant de setLoadout, y
				// désignent la pièce. Pas « stats » pour la base : renvoyé tel quel à setLoadout, un
				// composant de l'inventaire demanderait sinon une pièce améliorée.
				item.set("base", Entity.statsJSON(stat -> component.getStats().get(stat)));
				var alterations = Json.createArray();
				for (var piece : pieces.getOrDefault(template, List.of())) {
					alterations.add(piece.statsJSON());
				}
				item.set("alterations", alterations);
			}
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
		for (var stat : Entity.STAT_NAMES.keySet()) {
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
