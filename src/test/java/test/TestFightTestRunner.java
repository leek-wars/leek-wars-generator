package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.test.FightTestRunner;
import com.leekwars.generator.test.TestScenario;

import leekscript.common.Error;
import leekscript.compiler.LeekScript;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.FileSystem;
import leekscript.compiler.resolver.NativeFileSystem;
import leekscript.testing.TestCase;
import leekscript.testing.TestResult;
import leekscript.testing.TestRun;

/** Le lanceur de tests en combat : le bac à sable, et le plateau d'un fichier de scénario. */
public class TestFightTestRunner {

	private Path root;
	private FileSystem previous;

	@Before
	public void setUp() throws Exception {
		root = Files.createTempDirectory("fight-test-runner");
		previous = LeekScript.getFileSystem();
		LeekScript.setFileSystem(new NativeFileSystem(root.toString(), 1));
	}

	@After
	public void tearDown() {
		LeekScript.setFileSystem(previous);
	}

	private void write(String path, String content) throws Exception {
		var file = root.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	private Map<String, TestResult> run(String module) {
		var results = FightTestRunner.run(new Generator(), List.of(new FightTestRunner.Selection(module, null)),
			path -> LeekScript.getFileSystem().getRoot().resolve(path), new Object(), () -> true, Long.MAX_VALUE);
		return results.stream().collect(Collectors.toMap(TestResult::name, r -> r));
	}

	@Test
	public void testDependencyOfAPackage() throws Exception {
		// `depends test` : le module de test de scoring importe fixtures, qui dépend de scoring
		write("scoring/package.leek", "// @version:5\npackage scoring\ndepends test fixtures from \"../fixtures\"\nexport function score(integer n) -> integer { return n + 1 }");
		write("fixtures/package.leek", "// @version:5\npackage fixtures\ndepends scoring from \"../scoring\"\nimport { score } from \"@scoring\"\nexport function expected() -> integer { return score(2) }");
		write("scoring/test/s.test.leek", "// @version:5\nimport { score } from \"../package.leek\"\nimport { expected } from \"@fixtures\"\n@test function scores() { assertEquals(expected(), score(2)) }");
		var results = run("scoring/test/s.test.leek");
		assertEquals(results.toString(), TestResult.Status.PASSED, results.get("scores").status());
	}

	/** fixtures, dépendance de test de scoring ; deux modules du dossier test/ de scoring l'importent. */
	private void writeTestDependency() throws Exception {
		write("scoring/package.leek", "// @version:5\npackage scoring\ndepends test fixtures from \"../fixtures\"\nexport function score(integer n) -> integer { return n + 1 }");
		write("fixtures/package.leek", "// @version:5\npackage fixtures\nexport function two() -> integer { return 2 }");
		write("scoring/test/s.test.leek", "// @version:5\nimport { two } from \"@fixtures\"\n@test function t() { assertEquals(2, two()) }");
		write("scoring/test/adversaire.leek", "// @version:5\nimport { two } from \"@fixtures\"\ndebug(two())");
	}

	@Test
	public void fightAIIsNotCompiledInTestMode() throws Exception {
		writeTestDependency();
		var file = LeekScript.getFileSystem().getRoot().resolve("scoring/test/adversaire.leek");
		file.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
		try {
			file.compile(EntityAI.options(new Generator(), file, false));
		} catch (LeekCompilerException e) {
			// erreurs d'analyse : relevées sur le fichier
		}
		assertTrue(file.getErrors().stream().anyMatch(e -> e.error == Error.TEST_DEPENDENCY_OUTSIDE_TESTS));
	}

	@Test
	public void onlyTheTesterIsCompiledInTestMode() throws Exception {
		// Fight.startFight : seule l'entité testée compile en mode test, pas l'adversaire du même dossier
		writeTestDependency();
		var root = LeekScript.getFileSystem().getRoot();
		var module = root.resolve("scoring/test/s.test.leek");
		var generator = new Generator();
		module.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
		var table = TestCase.table(module.compile(EntityAI.options(generator, module, true)));
		var fight = new Fight(generator);
		var scenario = TestScenario.standard();
		FightTestRunner.prepare(fight, scenario, module, new TestRun("scoring/test/s.test.leek", table, table.get(0), new ArrayList<>()));
		var opponent = scenario.teams().get(1).get(0);
		opponent.setAIFile(root.resolve("scoring/test/adversaire.leek"));
		opponent.setAIEquipped(true);
		try {
			fight.startFight(false);
		} finally {
			fight.closePolyglotSandbox();
		}
		assertTrue(((EntityAI) scenario.tester().getAI()).isValid());
		assertFalse(((EntityAI) opponent.getAI()).isValid());
	}

	@Test
	public void scenarioFileBuildsTheBoard() throws Exception {
		write("sc/plateau.json", """
			{"type": "leek-wars-scenario", "version": 1, "seed": 7,
			 "map": {"obstacles": [200, 201], "team1": [300], "team2": [100, 110]},
			 "team1": [{"name": "Moi", "level": 120, "life": 1234, "strength": 300, "tp": 12, "weapons": ["pistol"], "chips": ["shield"]}],
			 "team2": [{"name": "A", "life": 50}, {"name": "B", "life": 60}]}""");
		write("sc/compte.json", "{\"type\": \"leek-wars-scenario\", \"team1\": [{\"leek\": \"Gorglucks\"}]}");
		write("sc/equipe.json", "{\"type\": \"leek-wars-scenario\", \"fight\": \"team\", \"team1\": [{\"name\": \"A\"}]}");
		write("sc/borne.json", "{\"type\": \"leek-wars-scenario\", \"team1\": [{\"name\": \"A\", \"life\": 0}]}");
		write("sc/seul.json", "{\"type\": \"leek-wars-scenario\", \"team1\": [{\"name\": \"A\", \"chips\": [\"shield\", \"helmet\", \"wall\", \"cure\", \"bandage\", \"protein\", \"motivation\"]}]}");
		write("sc/test/s.test.leek", """
			// @version:5
			@test(scenario: "../plateau.json")
			function board() {
				assertEquals(1234, getLife())
				assertEquals(300, getStrength())
				assertEquals(300, getCell())
				assertEquals(1, Array.count(getWeapons()))
				assertEquals([100, 110], Array.sorted(Array.map(getAliveEnemies(), (integer e) => getCell(e))))
				assert(isObstacle(200))
			}
			@test
			function sandbox() {
				assertEquals(5000, getLife())
				assertEquals(306, getCell())
			}
			@test(scenario: "../compte.json")
			function accountLeek() { assert(true) }
			@test(scenario: "absent.json")
			function missing() { assert(true) }
			@test(scenario: "../equipe.json")
			function teamFight() { assert(true) }
			@test(scenario: "../borne.json")
			function outOfBounds() { assert(true) }
			@test(scenario: "../seul.json")
			function alone() {
				assertEquals(1, Array.count(getAliveEnemies()))
				assertEquals(7, getRAM())
			}
			""");
		var results = run("sc/test/s.test.leek");
		assertEquals(TestResult.Status.PASSED, results.get("board").status());
		assertEquals(TestResult.Status.PASSED, results.get("sandbox").status());
		assertEquals(TestResult.Status.ERROR, results.get("accountLeek").status());
		assertTrue(results.get("accountLeek").error(), results.get("accountLeek").error().startsWith("scenario_leek_not_written"));
		assertTrue(results.get("missing").error(), results.get("missing").error().startsWith("scenario_not_found"));
		assertTrue(results.get("teamFight").error(), results.get("teamFight").error().startsWith("scenario_fight_unsupported"));
		// Bornes d'un poireau écrit : une valeur hors bornes est refusée, comme pour un combat de test
		assertTrue(results.get("outOfBounds").error(), results.get("outOfBounds").error().startsWith("scenario_leek_invalid life"));
		// Sans adversaire écrit, le mannequin ; sans RAM écrite, assez pour ses puces
		assertEquals(TestResult.Status.PASSED, results.get("alone").status());
	}
}
