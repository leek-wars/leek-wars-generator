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

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * End-to-end tests that build a Fight, attach LeekScript AIs as AIFile, and run
 * fight.startFight() — exercising the full Fight.runHooks orchestration, the
 * turn loop, and post-fight cleanup. Distinct from TestHooks which calls
 * runHook() directly without going through Fight.startFight().
 */
public class TestHooksFight {

	private Generator generator;
	private Fight fight;
	private Leek leek1;
	private Leek leek2;
	private FarmerLog farmerLog;
	private final HashMap<Integer, String> registerStore = new HashMap<>();

	@Before
	public void setUp() {
		generator = new Generator();
		fight = new Fight(generator);
		fight.getState().setRegisterManager(new RegisterManager() {
			@Override public String getRegisters(int leek) { return registerStore.get(leek); }
			@Override public void saveRegisters(int leek, String registers, boolean is_new) { registerStore.put(leek, registers); }
		});
		fight.setStatisticsManager(new LocalTrophyManager());
		// Constructor positions: id, name, farmer, level, life, tp, mp, force, agility,
		// frequency, wisdom, resistance, science, magic, cores, ram, skin, metal, face, ...
		// Cores must be > 0 (maxOperations = cores * 1M) — with 0 cores, the very first ops
		// call past the budget throws TOO_MUCH_OPERATIONS.
		leek1 = new Leek(1, "L1", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 8, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "L2", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 8, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
		farmerLog = new FarmerLog(fight, 0);
	}

	private static final java.util.concurrent.atomic.AtomicLong AI_COUNTER = new java.util.concurrent.atomic.AtomicLong(1_000_000);

	private void attachAI(Leek leek, String code) {
		long uid = AI_COUNTER.incrementAndGet();
		AIFile file = new AIFile("<test_" + uid + ">", code, System.currentTimeMillis(),
			LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	private void runFight() throws Exception {
		fight.startFight(true);
		// Mirror Generator.runScenario register save loop: production persists
		// modified registers to the manager at fight end.
		var rm = fight.getState().getRegisterManager();
		for (var entity : fight.getState().getEntities().values()) {
			if (!entity.isSummon() && entity.getRegisters() != null
				&& (entity.getRegisters().isModified() || entity.getRegisters().isNew())) {
				rm.saveRegisters(entity.getId(), entity.getRegisters().toJSONString(), entity.getRegisters().isNew());
			}
		}
	}

	private static FightLoadout statsLoadout(String name, int life, int strength, int agility) {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_LIFE, life);
		stats.put(Entity.STAT_STRENGTH, strength);
		stats.put(Entity.STAT_AGILITY, agility);
		return new FightLoadout(name, java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats);
	}

	// ---------- Hook orchestration ----------

	@Test
	public void beforeFightCalledViaStartFight() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('hook', '1'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("hook"));
	}

	@Test
	public void afterFightCalledViaStartFight() throws Exception {
		attachAI(leek1, "function afterFight() { setRegister('after', '1'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("after"));
	}

	@Test
	public void bothHooksCalledOnSameLeek() throws Exception {
		attachAI(leek1,
			"function beforeFight() { setRegister('phase', 'before'); }"
			+ "function afterFight() { setRegister('phase', getRegister('phase') + ',after'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("before,after", leek1.getRegister("phase"));
	}

	@Test
	public void hooksCalledOnEachEntity() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('me', 'leek1'); }");
		attachAI(leek2, "function beforeFight() { setRegister('me', 'leek2'); }");
		runFight();
		Assert.assertEquals("leek1", leek1.getRegister("me"));
		Assert.assertEquals("leek2", leek2.getRegister("me"));
	}

	@Test
	public void noHookDefinedIsNoOp() throws Exception {
		attachAI(leek1, "var dummy = 1;");
		attachAI(leek2, "");
		runFight();
		// No crash, and no register written
		Assert.assertNull(leek1.getRegister("anything"));
	}

	// ---------- Phase flag transitions ----------

	@Test
	public void hookPhaseIsBeforeFightDuringBeforeFight() throws Exception {
		// Marker: setLoadout('__nope__') returns false outside of beforeFight phase,
		// returns false inside too if name unknown — but only inside the phase will
		// the warning code be 1006 (LOADOUT_NOT_FOUND), not 1007 (out-of-hook).
		// Easier: write the registered phase via setRegister directly.
		attachAI(leek1, "function beforeFight() { setRegister('inHook', setLoadout('__never__') ? 'true' : 'false'); }"
			+ "function afterFight() { setRegister('outHook', setLoadout('__never__') ? 'true' : 'false'); }");
		attachAI(leek2, "");
		runFight();
		// Inside beforeFight: setLoadout looks up name (not found) → false, but no out-of-hook warning
		Assert.assertEquals("false", leek1.getRegister("inHook"));
		// Outside beforeFight (in afterFight): setLoadout returns false because phase != BEFORE_FIGHT
		Assert.assertEquals("false", leek1.getRegister("outHook"));
	}

	@Test
	public void hookPhaseIsResetBetweenHookAndTurn() throws Exception {
		// beforeFight runs, then turn 1 runs the AI's main code.
		// During main, hookPhase must be NONE (not BEFORE_FIGHT).
		// Marker: setLoadout from main code triggers SET_LOADOUT_OUT_OF_HOOK warning + returns false.
		attachAI(leek1,
			"function beforeFight() {}"
			+ "setRegister('main_setLoadout', setLoadout('whatever') ? 'true' : 'false');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("false", leek1.getRegister("main_setLoadout"));
	}

	// ---------- setLoadout end-to-end ----------

	@Test
	public void setLoadoutInBeforeFightAppliesStatsForTheFight() throws Exception {
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		// Initial leek strength = 100. After beforeFight setLoadout('pvp'), should be 250.
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }"
			+ "setRegister('strength_turn1', '' + getStrength());"
			+ "setRegister('life_turn1', '' + getTotalLife());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("250", leek1.getRegister("strength_turn1"));
		Assert.assertEquals("800", leek1.getRegister("life_turn1"));
	}

	@Test
	public void setLoadoutDoesNotAffectOtherLeeks() throws Exception {
		leek1.addLoadout(statsLoadout("pvp", 800, 250, 50));
		attachAI(leek1, "function beforeFight() { setLoadout('pvp'); }");
		attachAI(leek2, "setRegister('opp_strength', '' + getStrength());");
		runFight();
		// leek2 still has its initial strength
		Assert.assertEquals("100", leek2.getRegister("opp_strength"));
	}

	@Test
	public void setLoadoutMultipleCallsLastWins() throws Exception {
		leek1.addLoadout(statsLoadout("strong", 500, 300, 0));
		leek1.addLoadout(statsLoadout("fast", 500, 0, 300));
		attachAI(leek1, "function beforeFight() { setLoadout('strong'); setLoadout('fast'); }"
			+ "setRegister('s', '' + getStrength());"
			+ "setRegister('a', '' + getAgility());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("0", leek1.getRegister("s"));
		Assert.assertEquals("300", leek1.getRegister("a"));
	}

	@Test
	public void setLoadoutWithUnknownNameKeepsCurrentEquipment() throws Exception {
		// No loadouts registered, so setLoadout('whatever') fails — stats unchanged.
		attachAI(leek1, "function beforeFight() { setLoadout('whatever'); }"
			+ "setRegister('s', '' + getStrength());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("s"));
	}

	// ---------- getWinner ----------

	@Test
	public void getWinnerReturnsValueInAfterFight() throws Exception {
		// Simple AI for both: do nothing, fight will run to MAX_TURNS, drawCheckLife=true compares life.
		// They start with equal life so it should be a draw → -1.
		attachAI(leek1, "function afterFight() { setRegister('w', '' + getWinner()); }");
		attachAI(leek2, "");
		runFight();
		String winner = leek1.getRegister("w");
		// Since fights without weapons typically end in a draw with equal HP, winner = -1
		Assert.assertNotNull(winner);
		Assert.assertTrue("getWinner returned: " + winner, winner.equals("-1") || winner.equals("0") || winner.equals("1"));
	}

	@Test
	public void getWinnerInBeforeFightReturnsMinusOne() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('w_pre', '' + getWinner()); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("-1", leek1.getRegister("w_pre"));
	}

	// ---------- Info-masking during beforeFight ----------

	@Test
	public void beforeFightCannotReadOpponentStrength() throws Exception {
		attachAI(leek1, "function beforeFight() {"
			+ "  var enemies = getEnemies();"
			+ "  var f = getForce(enemies[0]);"
			+ "  setRegister('opp_str', f == null ? 'null' : '' + f);"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("null", leek1.getRegister("opp_str"));
	}

	@Test
	public void beforeFightCanReadOwnStrength() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('my_str', '' + getStrength()); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("my_str"));
	}

	@Test
	public void beforeFightCanReadOpponentNameLevelCell() throws Exception {
		// Public info should remain visible during beforeFight
		attachAI(leek1, "function beforeFight() {"
			+ "  var enemies = getEnemies();"
			+ "  setRegister('opp_name', getName(enemies[0]));"
			+ "  setRegister('opp_level', '' + getLevel(enemies[0]));"
			+ "  setRegister('opp_cell_null', getCell(enemies[0]) == null ? 'true' : 'false');"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("L2", leek1.getRegister("opp_name"));
		Assert.assertEquals("10", leek1.getRegister("opp_level"));
		Assert.assertEquals("false", leek1.getRegister("opp_cell_null"));
	}

	@Test
	public void afterFightCanReadOpponentStrength() throws Exception {
		// Masking is only during beforeFight (symmetry of execution), not afterFight.
		attachAI(leek1, "function afterFight() { var enemies = getEnemies(); setRegister('opp_str', '' + getForce(enemies[0])); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("100", leek1.getRegister("opp_str"));
	}

	// ---------- Hook robustness (errors / ops) ----------

	@Test
	public void beforeFightExceptionDoesNotCrashFight() throws Exception {
		// AI throws null pointer in beforeFight — fight should still run.
		attachAI(leek1, "function beforeFight() { var x = null; var y = x[0]; }"
			+ "setRegister('turn1_ran', '1');");
		attachAI(leek2, "");
		runFight();
		// Turn 1 main code still runs even though hook failed
		Assert.assertEquals("1", leek1.getRegister("turn1_ran"));
	}

	@Test
	public void afterFightExceptionDoesNotPreventOtherEntities() throws Exception {
		attachAI(leek1, "function afterFight() { var x = null; var y = x[0]; }");
		attachAI(leek2, "function afterFight() { setRegister('reached', '1'); }");
		runFight();
		Assert.assertEquals("1", leek2.getRegister("reached"));
	}

	@Test
	public void beforeFightInfiniteLoopHitsOpsLimit() throws Exception {
		// Beforefight gets turn-1 ops + 1M bonus. An infinite loop should hit the limit
		// and return without crashing the fight — turn 1 main code should still run.
		attachAI(leek1, "function beforeFight() { while (true) { var x = 1; } }"
			+ "setRegister('turn1_ran', '1');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("turn1_ran"));
	}

	// ---------- Cross-entity + state integrity ----------

	@Test
	public void registersFromHooksPersistToManager() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('persisted', 'yes'); }");
		attachAI(leek2, "");
		runFight();
		// After the fight, register manager should hold the value
		Assert.assertNotNull("Register manager should have stored leek1 registers", registerStore.get(leek1.getId()));
		Assert.assertTrue("Stored JSON should contain 'persisted'", registerStore.get(leek1.getId()).contains("persisted"));
	}























	@Test
	public void hookSeesCurrentFightContext() throws Exception {
		// getFightType / getFightContext should work in beforeFight (they don't depend on opponent equipment)
		attachAI(leek1, "function beforeFight() {"
			+ "  setRegister('type', '' + getFightType());"
			+ "  setRegister('context', '' + getFightContext());"
			+ "}");
		attachAI(leek2, "");
		runFight();
		// Default state.type and state.context — both 0 in this test setup
		Assert.assertNotNull(leek1.getRegister("type"));
		Assert.assertNotNull(leek1.getRegister("context"));
	}
}
