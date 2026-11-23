package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.leekwars.generator.state.ComponentInstance;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.FightLoadout;
import com.leekwars.generator.state.LoadoutRules;
import com.leekwars.generator.test.LocalTrophyManager;
import com.leekwars.generator.util.Json;

import tools.jackson.databind.JsonNode;

import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.FileSystem;

/**
 * Scenario#loadouts = false (combat aux poireaux imposés, tournoi d'IA) : un setLoadout(objet) en
 * beforeFight est refusé, getInventory() rend null et l'inventaire réel de l'éleveur n'est jamais
 * demandé aux règles. Le même combat avec loadouts = true change l'équipement : le test attrape
 * bien la faille. Le combat passe par Generator.runScenario, comme celui du worker.
 */
public class TestScenarioLoadouts {

	private static final int FARMER = 7;
	private static final int OPPONENT = 8;
	private static final int PISTOL = 37, M_LASER = 47, FLASH = 6;
	private static final String AI_PATH = "scenario-loadouts.leek";
	private static final String AI = "function beforeFight() {\n"
		+ "	var ok = setLoadout({weapons: [WEAPON_M_LASER], chips: [CHIP_FLASH], stats: {strength: 50}})\n"
		+ "	setRegister('ok', '' + ok)\n"
		+ "	setRegister('inventory', jsonEncode(getInventory()))\n"
		+ "	setRegister('weapons', jsonEncode(getWeapons()))\n"
		+ "	setRegister('strength', '' + getStrength())\n"
		+ "}\n";

	private int itemRequests = 0;
	private final Map<Integer, String> registers = new HashMap<>();
	private FileSystem previousFileSystem;

	@After
	public void reset() {
		Generator.setLoadoutRules(null);
		if (previousFileSystem != null) LeekScript.setFileSystem(previousFileSystem);
	}

	private static List<Integer> ids(JsonNode list) {
		var out = new ArrayList<Integer>();
		for (var id : list) out.add(id.asInt());
		return out;
	}

	/** Règles factices (cf. TestLoadoutObject) : l'éleveur possède un laser et un flash. */
	private void rules() {
		Generator.setLoadoutRules(new LoadoutRules() {
			@Override public FightLoadout resolve(JsonNode loadout, int level) {
				Map<Integer, Integer> stats = new HashMap<>();
				stats.put(Entity.STAT_STRENGTH, loadout.get("stats").get("strength").asInt());
				return new FightLoadout(loadout.get("name").asString(), ids(loadout.get("weapons")), ids(loadout.get("forgotten_weapons")),
					ids(loadout.get("chips")), stats, stats, false, new ArrayList<>());
			}
			@Override public int totalCapital(int level) { return level * 5; }
			@Override public Map<Integer, Map<Integer, Integer>> farmerItems(java.util.Set<Integer> farmers) {
				itemRequests++;
				return farmers.contains(FARMER) ? Map.of(FARMER, Map.of(M_LASER, 1, FLASH, 1)) : Map.of();
			}
			@Override public Map<Integer, List<ComponentInstance>> freeComponents(java.util.Set<Integer> farmers) {
				return Map.of();
			}
		});
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
		e.frequency = 100;
		e.cores = 10;
		e.ram = 10;
		e.weapons.add(PISTOL);
		return e;
	}

	private static FarmerInfo farmer(int id) {
		var f = new FarmerInfo();
		f.id = id;
		f.name = "F" + id;
		f.country = "fr";
		f.restatPotions = 10;
		return f;
	}

	private Map<String, String> run(boolean loadouts) {
		rules();
		previousFileSystem = LeekScript.getFileSystem();
		LeekScript.setFileSystem(new TestPolyglotMultiFile.MemFileSystem(FARMER, Map.of(AI_PATH, AI)));
		var scenario = new Scenario();
		scenario.seed = 1;
		scenario.maxTurns = 1;
		scenario.loadouts = loadouts;
		scenario.farmers.put(FARMER, farmer(FARMER));
		scenario.farmers.put(OPPONENT, farmer(OPPONENT));
		var player = leek(1, "joueur", FARMER);
		player.ai_path = AI_PATH;
		player.aiOwner = FARMER;
		player.ai_version = LeekScript.LATEST_VERSION;
		scenario.addEntity(0, player);
		scenario.addEntity(1, leek(2, "adversaire", OPPONENT));
		var manager = new RegisterManager() {
			@Override public String getRegisters(int leek) { return null; }
			@Override public void saveRegisters(int leek, String json, boolean isNew) { registers.put(leek, json); }
		};
		Outcome outcome = new Generator().runScenario(scenario, null, manager, new LocalTrophyManager());
		Assert.assertNull("le combat ne doit pas lever d'exception", outcome.exception);
		// Mémoire d'ensemble : poireaux imposés, rien de posé ni de potion facturée ; sinon la
		// répartition jouée coûte une potion de restat, que le worker débiterait.
		var memory = outcome.loadouts.get(1);
		Assert.assertEquals(loadouts, memory.capital != null);
		Assert.assertEquals(loadouts, memory.restatCharged);
		var out = new HashMap<String, String>();
		Json.parseObject(registers.get(1)).properties().forEach(e -> out.put(e.getKey(), e.getValue().asString()));
		return out;
	}

	@Test
	public void fixedLeeksRefuseLoadoutAndInventory() {
		var r = run(false);
		Assert.assertEquals("false", r.get("ok"));
		Assert.assertEquals("null", r.get("inventory"));
		Assert.assertEquals("[" + PISTOL + "]", r.get("weapons"));
		Assert.assertEquals("0", r.get("strength"));
		Assert.assertEquals("l'inventaire réel n'est jamais demandé", 0, itemRequests);
	}

	@Test
	public void freeLeeksChangeTheirLoadout() {
		var r = run(true);
		Assert.assertEquals("true", r.get("ok"));
		Assert.assertNotEquals("null", r.get("inventory"));
		Assert.assertEquals("[" + M_LASER + "]", r.get("weapons"));
		Assert.assertEquals("50", r.get("strength"));
	}
}
