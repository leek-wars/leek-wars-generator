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
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_LIFE, life);
		stats.put(Entity.STAT_STRENGTH, strength);
		stats.put(Entity.STAT_AGILITY, agility);
		return new FightLoadout(name, java.util.Arrays.asList(weapons), java.util.Arrays.asList(chips), stats);
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
}
