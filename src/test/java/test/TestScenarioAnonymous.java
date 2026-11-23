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
 * entités d'un autre éleveur, allié compris ; les siennes restent lisibles. Sans le drapeau, tout
 * se lit comme avant.
 */
public class TestScenarioAnonymous {

	private static final int FARMER = 7, ALLY = 8, OPPONENT = 9;
	private static final String AI_PATH = "scenario-anonymous-identities.leek";
	private static final String AI = "function who(e) {\n"
		+ "	return jsonEncode([getName(e), getFarmerName(e), getFarmerCountry(e), getFarmerID(e), getAIName(e), getAIID(e)])\n"
		+ "}\n"
		+ "var allies = []\n"
		+ "for (var a in getAllies()) if (a != getEntity()) push(allies, who(a))\n"
		+ "setRegister('enemy', who(getNearestEnemy()))\n"
		+ "setRegister('allies', jsonEncode(allies))\n"
		+ "setRegister('self', jsonEncode([getName(), getFarmerName(), getFarmerID()]))\n";

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
		previousFileSystem = LeekScript.getFileSystem();
		LeekScript.setFileSystem(new TestPolyglotMultiFile.MemFileSystem(FARMER, Map.of(AI_PATH, AI)));
		var scenario = new Scenario();
		scenario.seed = 1;
		scenario.maxTurns = 1;
		scenario.anonymous = anonymous;
		for (int f : new int[] { FARMER, ALLY, OPPONENT }) scenario.farmers.put(f, farmer(f));
		scenario.addEntity(0, leek(1, "joueur", FARMER));
		scenario.addEntity(0, leek(2, "second", FARMER));
		scenario.addEntity(0, leek(3, "allie", ALLY));
		scenario.addEntity(1, leek(4, "adversaire", OPPONENT));
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

	@Test
	public void otherFarmersAreHidden() {
		var r = run(true);
		Assert.assertEquals("[null,null,null,null,null,null]", r.get("enemy"));
		var allies = r.get("allies");
		Assert.assertTrue(allies, allies.contains("[null,null,null,null,null,null]"));
		Assert.assertTrue("son propre poireau reste lisible : " + allies, allies.contains("second") && allies.contains(String.valueOf(FARMER)));
		Assert.assertFalse(allies, allies.contains("allie") || allies.contains("F" + ALLY));
		Assert.assertEquals("[\"joueur\",\"F" + FARMER + "\"," + FARMER + "]", r.get("self"));
	}

	@Test
	public void everyoneIsVisibleOtherwise() {
		var r = run(false);
		Assert.assertTrue(r.get("enemy"), r.get("enemy").startsWith("[\"adversaire\",\"F" + OPPONENT + "\",\"fr\"," + OPPONENT + ","));
		Assert.assertTrue(r.get("allies"), r.get("allies").contains("allie") && r.get("allies").contains("second"));
	}
}
