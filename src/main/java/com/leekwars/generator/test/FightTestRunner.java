package com.leekwars.generator.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.leekwars.generator.Generator;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.weapons.Weapons;

import leekscript.compiler.AIFile;
import leekscript.compiler.Options;
import leekscript.testing.TestCase;
import leekscript.testing.TestResult;
import leekscript.testing.TestRun;
import leekscript.testing.TestRunner;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lance les tests d'un module de test (`*.test.leek`) dans des combats. Chaque test a son combat
 * bac à sable et son IA neufs : les fonctions du combat y répondent pour de vrai, et rien ne passe
 * d'un test au suivant.
 *
 * Le bac à sable : sur une carte vide, un testeur qui porte toutes les armes et toutes les puces
 * (case {@link #TESTER_CELL}), et un mannequin sans IA ni équipement, toutes caractéristiques à
 * zéro (case {@link #DUMMY_CELL}, à 5 cases du testeur, en ligne). Le test tourne au tour 1 du
 * testeur, à la place de son code ; son plafond d'opérations est celui du test.
 */
public class FightTestRunner {

	/** Case du testeur : le centre de la carte. */
	public static final int TESTER_CELL = 306;
	/** Case du mannequin : 5 cases plus loin, sur la même ligne. */
	public static final int DUMMY_CELL = 216;

	public static final int TESTER_ID = 1;
	public static final int DUMMY_ID = 2;

	/** Plafond d'opérations d'un test, même s'il demande plus (`@test(ops: …)`) : borne sa durée. */
	public static final long MAX_OPERATIONS = 500_000_000;

	/** Plafond d'opérations du test : le sien (`@test(ops: …)`) ou celui par défaut, borné. */
	public static long maxOperations(TestCase test) {
		return Math.min(MAX_OPERATIONS, test.maxOperations(TestRunner.DEFAULT_MAX_OPERATIONS));
	}

	/** Lignes de journal gardées par test, et longueur d'une ligne : un `debug` en boucle ne remplit pas le rapport. */
	private static final int MAX_LOG_LINES = 500;
	private static final int MAX_LOG_LINE_LENGTH = 2000;

	/** Journal de l'entité recopié dans une liste bornée, pour le joindre au résultat du test. */
	private static class CapturingLog extends LeekLog {
		private final List<String> sink;

		CapturingLog(FarmerLog farmerLog, com.leekwars.generator.state.Entity entity, List<String> sink) {
			super(farmerLog, entity);
			this.sink = sink;
		}

		private void capture(String line) {
			if (sink.size() < MAX_LOG_LINES) sink.add(line.length() > MAX_LOG_LINE_LENGTH ? line.substring(0, MAX_LOG_LINE_LENGTH) + "…" : line);
		}

		@Override
		public void addLog(int type, String message) {
			capture(message);
			super.addLog(type, message);
		}

		@Override
		public void addLog(int type, String message, int color) {
			capture(message);
			super.addLog(type, message, color);
		}

		// Les erreurs système aussi : c'est là qu'une IA qui ne compile pas dit pourquoi
		@Override
		public void addSystemLog(int type, String trace, int key, String[] parameters) {
			capture("#" + key + (parameters == null ? "" : " " + String.join(" ", parameters)) + (trace == null || trace.isEmpty() ? "" : "\n" + trace.strip()));
			super.addSystemLog(type, trace, key, parameters);
		}
	}

	/**
	 * Lance tous les tests du module et rend leurs résultats, dans l'ordre du fichier. `name` : le
	 * nom du module dans les résultats (son chemin tel que l'appelant l'a donné).
	 */
	public static List<TestResult> runModule(Generator generator, AIFile module, String name) throws Exception {
		return runTable(generator, module, name, compile(generator, module));
	}

	/**
	 * Compile le module de test et rend sa table (vide s'il n'est pas un module de test). Seule
	 * étape qui lit les autres fichiers de l'éleveur : l'appelant qui partage le système de
	 * fichiers la prend sous son verrou, et lance les tests ({@link #runTable}) en dehors.
	 */
	public static List<TestCase> compile(Generator generator, AIFile module) throws Exception {
		synchronized (module) {
			module.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
			return TestCase.table(module.compile(new Options(module.getVersion(), module.isStrict(), generator.use_leekscript_cache, true, null, true)));
		}
	}

	/** Lance les tests d'une table rendue par {@link #compile}, dans l'ordre du fichier. */
	public static List<TestResult> runTable(Generator generator, AIFile module, String name, List<TestCase> table) {
		var results = new ArrayList<TestResult>();
		for (var test : table) {
			if (test.isTest()) results.add(runTest(generator, module, new TestRun(name, table, test, new ArrayList<>())));
		}
		return results;
	}

	private static TestResult runTest(Generator generator, AIFile module, TestRun run) {
		var test = run.test();
		if (test.skip() != null) return run.skipped();
		var fight = new Fight(generator);
		Leek tester = null;
		try {
			final var registers = new HashMap<Integer, String>();
			fight.getState().setRegisterManager(new RegisterManager() {
				@Override public String getRegisters(int leek) { return registers.get(leek); }
				@Override public void saveRegisters(int leek, String value, boolean is_new) { registers.put(leek, value); }
			});
			fight.setStatisticsManager(new LocalTrophyManager());
			fight.setMaxTurns(1);
			var farmerLog = new FarmerLog(fight, 0);

			// Un cœur = un million d'opérations par tour : assez de cœurs pour le plafond du test, que
			// l'IA applique ensuite à l'unité près
			long maxOperations = maxOperations(test);
			int cores = (int) Math.max(1, Math.min(Integer.MAX_VALUE / 1_000_000, (maxOperations + 999_999) / 1_000_000));

			tester = new Leek(TESTER_ID, "Tester", 1, 301, 5000, 30, 10, 500, 500, 100, 500, 500, 500, 500, cores, 100, 0, false, 0, 1, "", 0, "", "", "", 0);
			for (var weapon : Weapons.getTemplates().values()) tester.addWeapon(weapon);
			for (var chip : Chips.getTemplates().values()) tester.addChip(chip);
			fight.getState().addEntity(0, tester);
			tester.setLogs(new CapturingLog(farmerLog, tester, run.logs()));
			tester.setFight(fight);
			tester.setBirthTurn(1);
			tester.setAIFile(module);

			var dummy = new Leek(DUMMY_ID, "Dummy", 2, 301, 5000, 10, 3, 0, 0, 0, 0, 0, 0, 0, 1, 100, 0, false, 0, 2, "", 0, "", "", "", 0);
			fight.getState().addEntity(1, dummy);
			dummy.setLogs(new LeekLog(farmerLog, dummy));
			dummy.setFight(fight);
			dummy.setBirthTurn(1);
			dummy.setAIEquipped(false);

			ObjectNode map = Json.createObject();
			map.set("obstacles", Json.createObject());
			map.putArray("team1").add(TESTER_CELL);
			map.putArray("team2").add(DUMMY_CELL);
			fight.getState().setCustomMap(map);
			fight.getState().seed(1);

			fight.setTest(tester, run);
			fight.startFight(false);

			var result = result(tester);
			// Le tour du testeur n'est pas venu : son IA n'a pas compilé, ou le combat s'est arrêté avant
			return result != null ? result : run.error("test_not_run");
		} catch (Throwable e) {
			// Le combat a planté après le tour du testeur : le résultat du test est déjà là
			var result = result(tester);
			return result != null ? result : run.error("sandbox_error " + e);
		} finally {
			fight.closePolyglotSandbox();
		}
	}

	private static TestResult result(Leek tester) {
		return tester != null && tester.getAI() instanceof EntityAI ai ? ai.getTestResult() : null;
	}

	/** Résultats et décompte, en JSON : ce que lit l'appelant. */
	public static ObjectNode report(List<TestResult> results) {
		var json = Json.createObject();
		var array = json.putArray("results");
		var counts = new java.util.EnumMap<TestResult.Status, Integer>(TestResult.Status.class);
		long operations = 0;
		boolean ok = true;
		for (var result : results) {
			array.add(result.toJson());
			counts.merge(result.status(), 1, Integer::sum);
			operations += result.operations();
			ok &= result.status().ok;
		}
		var summary = json.putObject("summary");
		summary.put("tests", results.size());
		for (var status : TestResult.Status.values()) summary.put(status.name().toLowerCase(), counts.getOrDefault(status, 0));
		summary.put("operations", operations);
		// Un lancement qui n'a lancé aucun test n'a rien prouvé
		summary.put("ok", ok && !results.isEmpty());
		return json;
	}
}
