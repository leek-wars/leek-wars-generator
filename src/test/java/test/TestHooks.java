package test;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.FightLoadout;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;

public class TestHooks {

	private Generator generator;
	private Fight fight;
	private Leek leek1;
	private Leek leek2;

	@Before
	public void setUp() throws Exception {
		generator = new Generator();
		fight = new Fight(generator);
		final HashMap<Integer, String> store = new HashMap<>();
		fight.getState().setRegisterManager(new RegisterManager() {
			@Override public String getRegisters(int leek) { return store.get(leek); }
			@Override public void saveRegisters(int leek, String registers, boolean is_new) { store.put(leek, registers); }
		});
		fight.setStatisticsManager(new LocalTrophyManager());
		leek1 = new Leek(1, "Test", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "Bob", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
		fight.getState().setRestatPotionsAvailable(0, 999);
		fight.initFight();
	}

	private EntityAI compile(String code, Leek leek) throws Exception {
		var options = new Options(true);
		EntityAI ai = (EntityAI) LeekScript.compileSnippet(code, "com.leekwars.generator.fight.entity.EntityAI", options);
		ai.setEntity(leek);
		ai.setLogs(new LeekLog(new FarmerLog(fight, 0), leek));
		ai.setFight(fight);
		ai.setMaxOperations(1_000_000);
		return ai;
	}

	@Test
	public void hasHookDetection() throws Exception {
		EntityAI ai = compile("function beforeFight() {} function afterFight() {}", leek1);
		Assert.assertTrue(ai.hasHook("beforeFight"));
		Assert.assertTrue(ai.hasHook("afterFight"));
		Assert.assertFalse(ai.hasHook("notDefined"));
	}

	@Test
	public void hasHookAbsent() throws Exception {
		EntityAI ai = compile("var x = 1;", leek1);
		Assert.assertFalse(ai.hasHook("beforeFight"));
		Assert.assertFalse(ai.hasHook("afterFight"));
	}

	@Test
	public void runBeforeFightHookSetsRegister() throws Exception {
		EntityAI ai = compile("function beforeFight() { setRegister('hook_ran', 'yes'); }", leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals("yes", leek1.getRegister("hook_ran"));
	}

	@Test
	public void runAfterFightHookSetsRegister() throws Exception {
		EntityAI ai = compile("function afterFight() { setRegister('after_ran', 'yes'); }", leek1);
		ai.runHook("afterFight", EntityAI.HookPhase.AFTER_FIGHT);
		Assert.assertEquals("yes", leek1.getRegister("after_ran"));
	}

	@Test
	public void hookPhaseResetAfterRun() throws Exception {
		EntityAI ai = compile("function beforeFight() {}", leek1);
		Assert.assertEquals(EntityAI.HookPhase.NONE, ai.getHookPhase());
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals(EntityAI.HookPhase.NONE, ai.getHookPhase());
	}

	@Test
	public void runHookNoOpWhenAbsent() throws Exception {
		EntityAI ai = compile("var x = 1;", leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals(EntityAI.HookPhase.NONE, ai.getHookPhase());
	}

	@Test
	public void setLoadoutOutsideHookReturnsFalse() throws Exception {
		EntityAI ai = compile("return setLoadout('whatever');", leek1);
		var result = ai.runIA();
		Assert.assertEquals(false, result);
	}

	@Test
	public void setLoadoutWithUnknownNameReturnsFalse() throws Exception {
		EntityAI ai = compile("function beforeFight() { setRegister('result', setLoadout('inexistant') ? 'true' : 'false'); }", leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals("false", leek1.getRegister("result"));
	}

	@Test
	public void getWinnerBeforeEndIsMinusOne() throws Exception {
		EntityAI ai = compile("return getWinner();", leek1);
		var result = ai.runIA();
		Assert.assertEquals(-1L, result);
	}

	private static FightLoadout loadout(String name, Integer[] weapons, Integer[] chips, int life, int strength, int agility) {
		return loadout(name, weapons, new Integer[] {}, chips, life, strength, agility);
	}

	private static FightLoadout loadout(String name, Integer[] weapons, Integer[] forgottenWeapons, Integer[] chips, int life, int strength, int agility) {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_LIFE, life);
		stats.put(Entity.STAT_STRENGTH, strength);
		stats.put(Entity.STAT_AGILITY, agility);
		return new FightLoadout(name, java.util.Arrays.asList(weapons), java.util.Arrays.asList(forgottenWeapons), java.util.Arrays.asList(chips), stats);
	}

	@Test
	public void setLoadoutAppliesStatsAndLife() throws Exception {
		leek1.addLoadout(loadout("pvp", new Integer[] {}, new Integer[] {}, 800, 250, 50));
		EntityAI ai = compile("function beforeFight() { setRegister('ok', setLoadout('pvp') ? 'yes' : 'no'); }", leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertEquals("yes", leek1.getRegister("ok"));
		Assert.assertEquals(250, leek1.getStat(Entity.STAT_STRENGTH));
		Assert.assertEquals(50, leek1.getStat(Entity.STAT_AGILITY));
		Assert.assertEquals(800, leek1.getTotalLife());
		Assert.assertEquals(800, leek1.getLife());
	}

	@Test
	public void setLoadoutLastCallWins() throws Exception {
		leek1.addLoadout(loadout("strong", new Integer[] {}, new Integer[] {}, 500, 300, 0));
		leek1.addLoadout(loadout("fast", new Integer[] {}, new Integer[] {}, 500, 0, 300));
		EntityAI ai = compile("function beforeFight() { setLoadout('strong'); setLoadout('fast'); }", leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertEquals(0, leek1.getStat(Entity.STAT_STRENGTH));
		Assert.assertEquals(300, leek1.getStat(Entity.STAT_AGILITY));
	}

	@Test
	public void beforeFightMasksOpponentStats() throws Exception {
		// leek2 has strength=100 (from setUp). Outside hook, getForce(leek2.id) returns 100.
		// Inside beforeFight, it should return null.
		String code = "function beforeFight() { setRegister('masked_force', getForce(" + leek2.getFId() + ") == null ? 'null' : 'visible'); }";
		EntityAI ai = compile(code, leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals("null", leek1.getRegister("masked_force"));
	}

	@Test
	public void beforeFightDoesNotMaskSelfStats() throws Exception {
		String code = "function beforeFight() { setRegister('self_force', getForce(" + leek1.getFId() + ") == null ? 'null' : 'visible'); }";
		EntityAI ai = compile(code, leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals("visible", leek1.getRegister("self_force"));
	}

	@Test
	public void beforeFightMasksOpponentWeaponsAndChips() throws Exception {
		String code = "function beforeFight() {"
				+ "  setRegister('weapons', getWeapons(" + leek2.getFId() + ") == null ? 'null' : 'visible');"
				+ "  setRegister('chips', getChips(" + leek2.getFId() + ") == null ? 'null' : 'visible');"
				+ "}";
		EntityAI ai = compile(code, leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals("null", leek1.getRegister("weapons"));
		Assert.assertEquals("null", leek1.getRegister("chips"));
	}

	@Test
	public void beforeFightLeavesPublicLobbyInfoVisible() throws Exception {
		// beforeFight masks only loadout-dependent data: equipment, stats, effects.
		// Public lobby info (cell/name/team/level/IDs) stays visible because it's
		// not loadout-dependent and is already exposed via getOpponent*() helpers.
		String code = "function beforeFight() {"
				+ "  setRegister('cell', getCell(" + leek2.getFId() + ") == null ? 'null' : 'visible');"
				+ "  setRegister('name', getName(" + leek2.getFId() + ") == null ? 'null' : 'visible');"
				+ "  setRegister('teamID', getTeamID(" + leek2.getFId() + ") == null ? 'null' : 'visible');"
				+ "  setRegister('level', getLevel(" + leek2.getFId() + ") == null ? 'null' : 'visible');"
				+ "}";
		EntityAI ai = compile(code, leek1);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		Assert.assertEquals("visible", leek1.getRegister("cell"));
		Assert.assertEquals("visible", leek1.getRegister("name"));
		Assert.assertEquals("visible", leek1.getRegister("teamID"));
		Assert.assertEquals("visible", leek1.getRegister("level"));
	}

	@Test
	public void normalPlayDoesNotMaskOpponentStats() throws Exception {
		// runIA (no hook) — normal play, opponent stats fully visible
		String code = "return getForce(" + leek2.getFId() + ");";
		EntityAI ai = compile(code, leek1);
		var result = ai.runIA();
		Assert.assertEquals(100L, result);
	}

	@Test
	public void afterFightDoesNotMaskOpponentStats() throws Exception {
		String code = "function afterFight() { setRegister('opp_force', getForce(" + leek2.getFId() + ") == null ? 'null' : 'visible'); }";
		EntityAI ai = compile(code, leek1);
		ai.runHook("afterFight", EntityAI.HookPhase.AFTER_FIGHT);
		Assert.assertEquals("visible", leek1.getRegister("opp_force"));
	}

	@Test
	public void setLoadoutFailsOutsideHook() throws Exception {
		// Initial leek strength = 100 (from setUp constructor positional arg)
		leek1.addLoadout(loadout("any", new Integer[] {}, new Integer[] {}, 999, 999, 999));
		EntityAI ai = compile("return setLoadout('any');", leek1);
		var result = ai.runIA();
		Assert.assertEquals(false, result);
		// Stats untouched
		Assert.assertEquals(100, leek1.getStat(Entity.STAT_STRENGTH));
		Assert.assertEquals(500, leek1.getTotalLife());
	}

	@Test
	public void entityInfoParsesLoadoutsFromJson() throws Exception {
		// Mirrors the JSON shape produced by Worker's Entity.toEntityInfo() →
		// EntityInfo.toJson(), focusing on the loadouts field.
		String json = "{\"id\":42,\"name\":\"test\",\"level\":10,\"life\":500,\"tp\":6,\"mp\":7,\"strength\":100,"
			+ "\"loadouts\":[{\"name\":\"Boss\",\"weapons\":[37,38],\"forgotten_weapons\":[117,118],\"chips\":[3,33],"
			+ "\"stats\":{\"3\":250,\"0\":800}}]}";
		var parsed = new com.leekwars.generator.scenario.EntityInfo(
			(tools.jackson.databind.node.ObjectNode) com.leekwars.generator.util.Json.parse(json));

		Assert.assertEquals(1, parsed.loadouts.size());
		var ld = parsed.loadouts.get(0);
		Assert.assertEquals("Boss", ld.name);
		Assert.assertEquals(java.util.Arrays.asList(37, 38), ld.weapons);
		Assert.assertEquals(java.util.Arrays.asList(117, 118), ld.forgottenWeapons);
		Assert.assertEquals(java.util.Arrays.asList(3, 33), ld.chips);
		Assert.assertEquals(Integer.valueOf(250), ld.stats.get(Entity.STAT_STRENGTH));
		Assert.assertEquals(Integer.valueOf(800), ld.stats.get(Entity.STAT_LIFE));
	}

	// data/weapons.json doesn't always parse cleanly under tests (missing max_uses
	// in the bundled snapshot), so register synthetic weapons manually for these tests.
	private static final int FORGOTTEN_W1 = 9001;
	private static final int FORGOTTEN_W2 = 9002;
	private static final int FORGOTTEN_W3 = 9003;
	private static final int REGULAR_W = 9004;

	private static void registerSyntheticWeapons() {
		registerWeapon(FORGOTTEN_W1, "test_forgotten_1", true);
		registerWeapon(FORGOTTEN_W2, "test_forgotten_2", true);
		registerWeapon(FORGOTTEN_W3, "test_forgotten_3", true);
		registerWeapon(REGULAR_W, "test_regular", false);
	}

	private static void registerWeapon(int id, String name, boolean forgotten) {
		if (com.leekwars.generator.weapons.Weapons.getWeapon(id) == null) {
			com.leekwars.generator.weapons.Weapons.addWeapon(new com.leekwars.generator.weapons.Weapon(
				id, 5, 1, 6, com.leekwars.generator.util.Json.createArray(),
				(byte) 1, (byte) 1, true, id, name,
				com.leekwars.generator.util.Json.createArray(), 0, forgotten));
		}
	}

	private Leek makeLeekWithFarmer(int id, int farmerId) {
		return new Leek(id, "L" + id, farmerId, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
	}

	@Test
	public void setLoadoutFirstFreeForgottenAttribution() throws Exception {
		registerSyntheticWeapons();

		// Two leeks, same farmer. Both have the same single-candidate loadout —
		// only the first to apply gets the forgotten weapon, the second falls back
		// to no forgotten weapon (and emits a "no available alternative" warning).
		var allyA = makeLeekWithFarmer(101, 42);
		var allyB = makeLeekWithFarmer(102, 42);
		fight.getState().addEntity(0, allyA);
		fight.getState().addEntity(0, allyB);
		fight.initFight();

		Integer[] forgotten = { FORGOTTEN_W1 };
		allyA.addLoadout(loadout("boss", new Integer[] {}, forgotten, new Integer[] {}, 500, 100, 0));
		allyB.addLoadout(loadout("boss", new Integer[] {}, forgotten, new Integer[] {}, 500, 100, 0));

		EntityAI aiA = compile("function beforeFight() { setLoadout('boss'); }", allyA);
		aiA.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		EntityAI aiB = compile("function beforeFight() { setLoadout('boss'); }", allyB);
		aiB.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertTrue(allyA.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
		Assert.assertFalse(allyB.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
	}

	@Test
	public void setLoadoutAllowsSameForgottenAcrossDifferentFarmers() throws Exception {
		registerSyntheticWeapons();

		var farmerA = makeLeekWithFarmer(201, 1);
		var farmerB = makeLeekWithFarmer(202, 2);
		fight.getState().addEntity(0, farmerA);
		fight.getState().addEntity(1, farmerB);
		fight.initFight();

		Integer[] forgotten = { FORGOTTEN_W1 };
		farmerA.addLoadout(loadout("kit", new Integer[] {}, forgotten, new Integer[] {}, 500, 100, 0));
		farmerB.addLoadout(loadout("kit", new Integer[] {}, forgotten, new Integer[] {}, 500, 100, 0));

		EntityAI aiA = compile("function beforeFight() { setLoadout('kit'); }", farmerA);
		aiA.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		EntityAI aiB = compile("function beforeFight() { setLoadout('kit'); }", farmerB);
		aiB.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertTrue(farmerA.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
		Assert.assertTrue(farmerB.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
	}

	@Test
	public void setLoadoutDistributesAlternativesAcrossTeammates() throws Exception {
		registerSyntheticWeapons();

		// Two leeks, same farmer, same loadout listing two alternatives.
		// Each should get a distinct forgotten weapon — leek A picks W1 (first free),
		// leek B sees W1 reserved and falls back to W2.
		var allyA = makeLeekWithFarmer(301, 50);
		var allyB = makeLeekWithFarmer(302, 50);
		fight.getState().addEntity(0, allyA);
		fight.getState().addEntity(0, allyB);
		fight.initFight();

		Integer[] alts = { FORGOTTEN_W1, FORGOTTEN_W2 };
		allyA.addLoadout(loadout("boss", new Integer[] {}, alts, new Integer[] {}, 500, 100, 0));
		allyB.addLoadout(loadout("boss", new Integer[] {}, alts, new Integer[] {}, 500, 100, 0));

		compile("function beforeFight() { setLoadout('boss'); }", allyA)
			.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
		compile("function beforeFight() { setLoadout('boss'); }", allyB)
			.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertTrue(allyA.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
		Assert.assertFalse(allyA.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W2));
		Assert.assertFalse(allyB.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
		Assert.assertTrue(allyB.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W2));
	}

	@Test
	public void setLoadoutStickyKeepsCurrentForgottenWhenListed() throws Exception {
		registerSyntheticWeapons();

		// Leek already wears W2 (e.g. from default equipment). Loadout lists [W1, W2]
		// as alternatives. Sticky: keep W2, don't switch to W1 even though it's first.
		var leek = makeLeekWithFarmer(401, 60);
		fight.getState().addEntity(0, leek);
		fight.initFight();
		leek.addWeapon(com.leekwars.generator.weapons.Weapons.getWeapon(FORGOTTEN_W2));

		Integer[] alts = { FORGOTTEN_W1, FORGOTTEN_W2 };
		leek.addLoadout(loadout("boss", new Integer[] {}, alts, new Integer[] {}, 500, 100, 0));

		EntityAI ai = compile("function beforeFight() { setLoadout('boss'); }", leek);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertTrue("sticky: leek should still wear W2",
			leek.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W2));
		Assert.assertFalse("sticky: should not have grabbed W1",
			leek.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
	}

	@Test
	public void setLoadoutEmptyForgottenListClearsCurrentForgotten() throws Exception {
		registerSyntheticWeapons();

		// Loadout with empty forgotten_weapons list = "no forgotten for this build".
		var leek = makeLeekWithFarmer(501, 70);
		fight.getState().addEntity(0, leek);
		fight.initFight();
		leek.addWeapon(com.leekwars.generator.weapons.Weapons.getWeapon(FORGOTTEN_W1));

		leek.addLoadout(loadout("clean", new Integer[] {}, new Integer[] {}, new Integer[] {}, 500, 100, 0));

		EntityAI ai = compile("function beforeFight() { setLoadout('clean'); }", leek);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertFalse(leek.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
	}

	@Test
	public void setLoadoutFixedWeaponsIgnoreForgottenList() throws Exception {
		registerSyntheticWeapons();

		// A regular weapon listed in the fixed weapons slot is always equipped.
		var leek = makeLeekWithFarmer(601, 80);
		fight.getState().addEntity(0, leek);
		fight.initFight();

		Integer[] fixed = { REGULAR_W };
		Integer[] forgotten = { FORGOTTEN_W1 };
		leek.addLoadout(loadout("mix", fixed, forgotten, new Integer[] {}, 500, 100, 0));

		EntityAI ai = compile("function beforeFight() { setLoadout('mix'); }", leek);
		ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);

		Assert.assertTrue(leek.getWeapons().stream().anyMatch(w -> w.getId() == REGULAR_W));
		Assert.assertTrue(leek.getWeapons().stream().anyMatch(w -> w.getId() == FORGOTTEN_W1));
	}
}
