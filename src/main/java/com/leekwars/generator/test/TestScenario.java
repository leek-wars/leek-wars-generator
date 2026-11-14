package com.leekwars.generator.test;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.weapons.Weapons;

import leekscript.compiler.AIFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Le plateau d'un test : ses deux équipes, sa carte et sa graine. Le testeur, celui sur qui tourne le
 * test, est le premier poireau de l'équipe 1 ; les autres entités n'ont pas d'IA.
 *
 * Par défaut, le bac à sable : un testeur qui porte toutes les armes et toutes les puces, et un
 * mannequin sans caractéristiques à 5 cases, en ligne, sur une carte vide. Avec
 * `@test(scenario: "plateau.json")`, un fichier de scénario de test (`"type": "leek-wars-scenario"`,
 * le format des scénarios de l'éditeur) : ses poireaux écrits en entier, ses armes et puces par leur
 * nom, sa carte écrite en données ; sans adversaire écrit, le mannequin. Le chemin part du dossier
 * du module de test, comme un import, ou de la racine avec un « / ».
 *
 * @param teams les poireaux de chaque équipe, sans combat ni IA : le lanceur les y pose
 */
public record TestScenario(List<List<Leek>> teams, ObjectNode map, int seed) {

	public static final String TYPE = "leek-wars-scenario";

	/**
	 * Un poireau écrit dans le fichier : [défaut, min, max] de chaque champ, comme pour un combat de
	 * test lancé de ce fichier (TestScenarioFile côté API) : un test et un combat jouent le même plateau.
	 */
	private static final java.util.Map<String, int[]> FIELDS = java.util.Map.ofEntries(
		java.util.Map.entry("level", new int[] { 1, 1, 301 }), java.util.Map.entry("life", new int[] { 100, 1, 100000 }),
		java.util.Map.entry("strength", new int[] { 0, 0, 9999 }), java.util.Map.entry("wisdom", new int[] { 0, 0, 9999 }),
		java.util.Map.entry("agility", new int[] { 0, 0, 9999 }), java.util.Map.entry("resistance", new int[] { 0, 0, 9999 }),
		java.util.Map.entry("science", new int[] { 0, 0, 9999 }), java.util.Map.entry("magic", new int[] { 0, 0, 9999 }),
		java.util.Map.entry("frequency", new int[] { 100, 100, 9999 }), java.util.Map.entry("tp", new int[] { 10, 0, 1000 }),
		java.util.Map.entry("mp", new int[] { 3, 0, 100 }), java.util.Map.entry("cores", new int[] { 1, 1, 30 }),
		java.util.Map.entry("ram", new int[] { 6, 1, 40 }), java.util.Map.entry("skin", new int[] { 1, 1, 1000 }));

	/** Case du testeur du bac à sable : le centre de la carte. */
	private static final int TESTER_CELL = 306;
	/** Case du mannequin : 5 cases plus loin, sur la même ligne. */
	private static final int DUMMY_CELL = 216;

	/** Un scénario que le lanceur ne sait pas jouer : la raison (code) et ce qui la cause. */
	public static class ScenarioException extends Exception {
		public ScenarioException(String code, String detail) {
			super(detail == null ? code : code + " " + detail);
		}
	}

	public Leek tester() {
		return teams.get(0).get(0);
	}

	/** Le bac à sable par défaut. */
	public static TestScenario standard() {
		var tester = new Leek(1, "Tester", 1, 301, 5000, 30, 10, 500, 500, 100, 500, 500, 500, 500, 1, 100, 0, false, 0, 1, "", 0, "", "", "", 0);
		for (var weapon : Weapons.getTemplates().values()) tester.addWeapon(weapon);
		for (var chip : Chips.getTemplates().values()) tester.addChip(chip);
		var dummy = dummy(2);
		ObjectNode map = Json.createObject();
		map.set("obstacles", Json.createObject());
		map.putArray("team1").add(TESTER_CELL);
		map.putArray("team2").add(DUMMY_CELL);
		return new TestScenario(List.of(List.of(tester), List.of(dummy)), map, 1);
	}

	/** Le mannequin : sans IA, sans équipement, toutes caractéristiques à zéro. */
	private static Leek dummy(int id) {
		return new Leek(id, "Dummy", 2, 301, 5000, 10, 3, 0, 0, 0, 0, 0, 0, 0, 1, 100, 0, false, 0, 2, "", 0, "", "", "", 0);
	}

	/** Le plateau du fichier de scénario `path`, relu sur le disque. */
	public static TestScenario read(AIFile module, String path) throws ScenarioException {
		AIFile file;
		try {
			file = module.getFolder().resolve(path);
		} catch (Exception e) {
			file = null;
		}
		if (file == null) throw new ScenarioException("scenario_not_found", path);
		JsonNode json;
		try {
			json = Json.parse(file.getCode());
		} catch (Exception e) {
			throw new ScenarioException("scenario_invalid_json", path);
		}
		if (json == null || !TYPE.equals(json.path("type").asString(""))) throw new ScenarioException("scenario_not_a_scenario", path);
		// Ce que le plateau d'un test ne sait pas encore poser : le dire plutôt que l'ignorer
		var fightType = json.path("fight").asString("free");
		if (!fightType.equals("free")) throw new ScenarioException("scenario_fight_unsupported", fightType);
		if (json.path("turrets").isObject() && !json.get("turrets").isEmpty()) throw new ScenarioException("scenario_turrets_unsupported", path);

		var teams = new ArrayList<List<Leek>>();
		int id = 1;
		for (int t = 0; t < 2; t++) {
			var team = new ArrayList<Leek>();
			for (var entry : json.path("team" + (t + 1))) team.add(leek(entry, id++, t));
			teams.add(team);
		}
		if (teams.get(0).isEmpty()) throw new ScenarioException("scenario_no_tester", path);
		// Un combat se joue à deux équipes : sans adversaire écrit, le mannequin du bac à sable
		if (teams.get(1).isEmpty()) teams.get(1).add(dummy(id));
		return new TestScenario(teams, map(json.get("map")), json.path("seed").asInt(0) != 0 ? json.get("seed").asInt() : 1);
	}

	/**
	 * Un poireau écrit en entier dans le fichier. Un poireau désigné (vrai poireau, poireau de test,
	 * bot) vit dans la base de l'éleveur, que le lanceur de tests ne lit pas.
	 */
	private static Leek leek(JsonNode entry, int id, int team) throws ScenarioException {
		if (!entry.isObject()) throw new ScenarioException("scenario_leek_invalid", null);
		for (var key : List.of("leek", "test_leek", "bot")) {
			if (entry.has(key)) throw new ScenarioException("scenario_leek_not_written", entry.path(key).asString(""));
		}
		var leek = new Leek(id, entry.path("name").asString("Leek " + id), team + 1, field(entry, "level"),
			field(entry, "life"), field(entry, "tp"), field(entry, "mp"), field(entry, "strength"),
			field(entry, "agility"), field(entry, "frequency"), field(entry, "wisdom"), field(entry, "resistance"),
			field(entry, "science"), field(entry, "magic"), field(entry, "cores"), field(entry, "ram"),
			field(entry, "skin"), false, 0, team + 1, "", 0, "", "", "", 0);
		for (var name : entry.path("weapons")) {
			var weapon = Weapons.getWeapon(name.asString(""));
			if (weapon == null) throw new ScenarioException("scenario_unknown_weapon", name.asString(""));
			leek.addWeapon(weapon);
		}
		for (var name : entry.path("chips")) {
			var chip = Chips.getChip(name.asString(""));
			if (chip == null) throw new ScenarioException("scenario_unknown_chip", name.asString(""));
			leek.addChip(chip);
		}
		return leek;
	}

	private static int field(JsonNode entry, String key) {
		var bounds = FIELDS.get(key);
		return entry.path(key).isNumber() ? Math.max(bounds[1], Math.min(bounds[2], entry.get(key).asInt())) : bounds[0];
	}

	/**
	 * La carte écrite dans le fichier : obstacles (liste de cases, ou {case: obstacle}) et cases de
	 * départ de chaque équipe. Sans carte, une carte vide où les entités sont placées au hasard (graine
	 * du scénario). Une carte de test désignée par son nom vit dans la base.
	 */
	private static ObjectNode map(JsonNode data) throws ScenarioException {
		ObjectNode map = Json.createObject();
		ObjectNode obstacles = map.putObject("obstacles");
		if (data == null || data.isNull()) return map;
		if (!data.isObject()) throw new ScenarioException("scenario_map_not_written", data.asString(""));
		var written = data.path("obstacles");
		if (written.isArray()) {
			for (var cell : written) obstacles.put(String.valueOf(cell.asInt()), 1);
		} else if (written.isObject()) {
			for (var cell : written.properties()) obstacles.set(cell.getKey(), cell.getValue());
		}
		for (var team : List.of("team1", "team2")) {
			if (data.path(team).isArray()) map.set(team, data.get(team));
		}
		return map;
	}
}
