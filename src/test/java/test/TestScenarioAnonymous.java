package test;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.FarmerInfo;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.test.LocalTrophyManager;
import com.leekwars.generator.util.Json;

import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.FileSystem;

/**
 * Scenario#anonymous (tournoi d'IA) : une IA ne lit ni le nom, ni l'éleveur, ni le pays, ni l'IA des
 * entités d'un autre éleveur, allié compris ; les siennes restent lisibles. Nom du poireau, de son
 * éleveur et de son IA : un pseudonyme (Pseudonyms), jamais null ; pays et ids : null. Sans le
 * drapeau, tout se lit comme avant.
 */
public class TestScenarioAnonymous {

	private static final int FARMER = 7, ALLY = 8, OPPONENT = 9;
	private static final String AI_PATH = "scenario-anonymous-leek-names.leek";
	private static final String AI = "function who(e) {\n"
		+ "	return jsonEncode([getName(e), getFarmerName(e), getFarmerCountry(e), getFarmerID(e), getAIName(e), getAIID(e)])\n"
		+ "}\n"
		+ "var allies = []\n"
		+ "for (var a in getAllies()) if (a != getEntity()) push(allies, who(a))\n"
		+ "for (var e in getEnemies()) if (getType(e) == ENTITY_LEEK) setRegister('enemy', who(e))\n"
		+ "setRegister('allies', jsonEncode(allies))\n"
		+ "setRegister('self', jsonEncode([getName(), getFarmerName(), getFarmerID()]))\n"
		+ "for (var e in getEnemies()) if (getType(e) == ENTITY_TURRET) setRegister('turret', getName(e))\n";

	private final Map<Integer, String> registers = new HashMap<>();
	private FileSystem previousFileSystem;

	@After
	public void reset() {
		if (previousFileSystem != null) LeekScript.setFileSystem(previousFileSystem);
	}

	private static EntityInfo leek(int id, String name, int farmer) {
		var e = new EntityInfo();
		e.id = id;
		e.name = name;
		e.type = Entity.TYPE_LEEK;
		e.farmer = farmer;
		e.level = 100;
		e.life = 1000;
		e.tp = 10;
		e.mp = 3;
		e.frequency = 100 - id;
		e.cores = 10;
		e.ram = 10;
		e.ai_path = AI_PATH;
		e.aiOwner = FARMER;
		e.ai_version = LeekScript.LATEST_VERSION;
		return e;
	}

	private static FarmerInfo farmer(int id) {
		var f = new FarmerInfo();
		f.id = id;
		f.name = "F" + id;
		f.country = "fr";
		return f;
	}

	/** Registres écrits par le poireau 1 (éleveur FARMER). */
	private Map<String, String> run(boolean anonymous) {
		return run(anonymous, 1);
	}

	private Map<String, String> run(boolean anonymous, int seed) {
		previousFileSystem = LeekScript.getFileSystem();
		LeekScript.setFileSystem(new TestPolyglotMultiFile.MemFileSystem(FARMER, Map.of(AI_PATH, AI)));
		var scenario = new Scenario();
		scenario.seed = seed;
		scenario.maxTurns = 1;
		scenario.anonymous = anonymous;
		for (int f : new int[] { FARMER, ALLY, OPPONENT }) scenario.farmers.put(f, farmer(f));
		scenario.addEntity(0, leek(1, "joueur", FARMER));
		scenario.addEntity(0, leek(2, "second", FARMER));
		scenario.addEntity(0, leek(3, "allie", ALLY));
		scenario.addEntity(1, leek(4, "adversaire", OPPONENT));
		// Tourelle adverse, sans éleveur : son nom n'est celui de personne
		var turret = leek(5, "turret", 0);
		turret.type = Entity.TYPE_TURRET;
		turret.ai_path = null;
		scenario.addEntity(1, turret);
		var manager = new RegisterManager() {
			@Override public String getRegisters(int leek) { return null; }
			@Override public void saveRegisters(int leek, String json, boolean isNew) { registers.put(leek, json); }
		};
		Outcome outcome = new Generator().runScenario(scenario, null, manager, new LocalTrophyManager());
		Assert.assertNull("le combat ne doit pas lever d'exception", outcome.exception);
		Assert.assertNotNull("le poireau 1 a joué", registers.get(1));
		var out = new HashMap<String, String>();
		Json.parseObject(registers.get(1)).properties().forEach(e -> out.put(e.getKey(), e.getValue().asString()));
		return out;
	}

	/** [nom, éleveur, pays, id d'éleveur, IA, id d'IA] d'une entité, tels que l'IA les lit. */
	private static java.util.List<String> who(String json) {
		var out = new java.util.ArrayList<String>();
		for (var v : Json.parseArray(json)) out.add(v.isNull() ? null : v.asString());
		return out;
	}

	@Test
	public void otherFarmersAreHidden() {
		var r = run(true);
		var enemy = who(r.get("enemy"));
		Assert.assertNotNull("un pseudonyme, pas null : " + enemy, enemy.get(0));
		Assert.assertNotEquals("adversaire", enemy.get(0));
		Assert.assertNotNull(enemy.get(1));
		Assert.assertNotEquals("F" + OPPONENT, enemy.get(1));
		Assert.assertNull("pays caché", enemy.get(2));
		Assert.assertNull("id d'éleveur caché", enemy.get(3));
		Assert.assertNotNull("nom d'IA : un pseudonyme", enemy.get(4));
		Assert.assertNull("id d'IA caché", enemy.get(5));
		var allies = r.get("allies");
		Assert.assertTrue("son propre poireau reste lisible : " + allies, allies.contains("second") && allies.contains(String.valueOf(FARMER)));
		Assert.assertFalse(allies, allies.contains("allie") || allies.contains("F" + ALLY));
		Assert.assertEquals("[\"joueur\",\"F" + FARMER + "\"," + FARMER + "]", r.get("self"));
		Assert.assertEquals("le nom d'une tourelle reste lisible", "turret", r.get("turret"));
		// L'allié d'un autre éleveur a son propre pseudonyme, distinct de celui de l'adversaire
		var autre = Json.parseArray(allies);
		java.util.List<String> allie = null;
		for (var a : autre) {
			var w = who(a.asString());
			if (w.get(3) == null) allie = w;
		}
		Assert.assertNotNull(allies, allie);
		Assert.assertNotEquals("deux poireaux, deux noms", enemy.get(0), allie.get(0));
		Assert.assertNotEquals("deux éleveurs, deux noms", enemy.get(1), allie.get(1));
	}

	/** Même graine, mêmes pseudonymes (rejeu identique) ; une autre graine les change. */
	@Test
	public void pseudonymsFollowTheSeed() {
		var a = who(run(true, 1).get("enemy"));
		var b = who(run(true, 1).get("enemy"));
		Assert.assertEquals(a, b);
		boolean change = false;
		for (int seed = 2; seed < 6 && !change; seed++) change = !who(run(true, seed).get("enemy")).get(0).equals(a.get(0));
		Assert.assertTrue("le pseudonyme dépend de la graine", change);
	}

	@Test
	public void everyoneIsVisibleOtherwise() {
		var r = run(false);
		Assert.assertTrue(r.get("enemy"), r.get("enemy").startsWith("[\"adversaire\",\"F" + OPPONENT + "\",\"fr\"," + OPPONENT + ","));
		Assert.assertTrue(r.get("allies"), r.get("allies").contains("allie") && r.get("allies").contains("second"));
	}
}
