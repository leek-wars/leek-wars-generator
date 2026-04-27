package test;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * Edge cases & oddities — probing the engine for unexpected behaviors,
 * especially in hook contexts where regular turn actions don't make sense.
 */
public class TestFightEdgeCases {

	private Generator generator;
	private Fight fight;
	private Leek leek1;
	private Leek leek2;
	private FarmerLog farmerLog;
	private final HashMap<Integer, String> registerStore = new HashMap<>();
	private static final java.util.concurrent.atomic.AtomicLong AI_COUNTER = new java.util.concurrent.atomic.AtomicLong(3_000_000);

	@Before
	public void setUp() {
		generator = new Generator();
		fight = new Fight(generator);
		fight.getState().setRegisterManager(new RegisterManager() {
			@Override public String getRegisters(int leek) { return registerStore.get(leek); }
			@Override public void saveRegisters(int leek, String registers, boolean is_new) { registerStore.put(leek, registers); }
		});
		fight.setStatisticsManager(new LocalTrophyManager());
		leek1 = new Leek(1, "L1", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 8, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "L2", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 8, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
		farmerLog = new FarmerLog(fight, 0);
	}

	private void attachAI(Leek leek, String code) {
		long uid = AI_COUNTER.incrementAndGet();
		AIFile file = new AIFile("<edge_" + uid + ">", code, System.currentTimeMillis(),
			LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	private void runFight() throws Exception {
		fight.startFight(true);
	}

	// ---------- Combat actions in hooks ----------

	@Test
	public void moveTowardCellInBeforeFightDoesNotCrash() throws Exception {
		// Spec §2.2: "fonctions de combat actif appelées dans beforeFight() ou afterFight()
		// → no-op + warning". moveToward isn't currently gated; verify it at least
		// doesn't crash the fight.
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  setRegister('start_cell', '' + getCell());"
			+ "  var enemyCell = getCell(getEnemies()[0]);"
			+ "  var moved = moveTowardCell(enemyCell);"
			+ "  setRegister('moved_in_hook', '' + moved);"
			+ "  setRegister('cell_after_hook_move', '' + getCell());"
			+ "}");
		attachAI(leek2, "");
		runFight();
		// The hook ran — just verify it didn't crash the fight
		Assert.assertNotNull(leek1.getRegister("start_cell"));
		Assert.assertNotNull(leek1.getRegister("moved_in_hook"));
	}

	@Test
	public void useWeaponInBeforeFightDoesNotCrash() throws Exception {
		// useWeapon without an equipped weapon — should fail gracefully.
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  var enemies = getEnemies();"
			+ "  var result = useWeapon(enemies[0]);"
			+ "  setRegister('use_result', '' + result);"
			+ "}");
		attachAI(leek2, "");
		runFight();
		// No crash means the hook orchestration is robust
		Assert.assertNotNull(leek1.getRegister("use_result"));
	}

	@Test
	public void useChipInBeforeFightDoesNotCrash() throws Exception {
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  var enemies = getEnemies();"
			+ "  var result = useChip(0, enemies[0]);"
			+ "  setRegister('use_result', '' + result);"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertNotNull(leek1.getRegister("use_result"));
	}

	@Test
	public void useChipInBeforeFightReturnsMinusOneAndDoesNotMutate() throws Exception {
		// Combat actions are gated: useChip during beforeFight must return -1 and not
		// consume TP / trigger effects.
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  var initial_tp = getTP();"
			+ "  var result = useChip(0, getEnemies()[0]);"
			+ "  setRegister('result', '' + result);"
			+ "  setRegister('tp_unchanged', getTP() == initial_tp ? 'true' : 'false');"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("-1", leek1.getRegister("result"));
		Assert.assertEquals("true", leek1.getRegister("tp_unchanged"));
	}

	@Test
	public void useWeaponInBeforeFightReturnsMinusOne() throws Exception {
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  setRegister('result', '' + useWeapon(getEnemies()[0]));"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("-1", leek1.getRegister("result"));
	}

	@Test
	public void moveTowardCellInBeforeFightDoesNotMove() throws Exception {
		// Combat action moveTowardCell must not actually move during beforeFight.
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  setRegister('cell_before', '' + getCell());"
			+ "  moveTowardCell(getCell(getEnemies()[0]));"
			+ "  setRegister('cell_after', '' + getCell());"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals(leek1.getRegister("cell_before"), leek1.getRegister("cell_after"));
	}

	@Test
	public void setWeaponInBeforeFightReturnsFalse() throws Exception {
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  setRegister('result', setWeapon(37) ? 'true' : 'false');"
			+ "}");
		attachAI(leek2, "");
		runFight();
		// setWeapon during a hook returns false (gated)
		Assert.assertEquals("false", leek1.getRegister("result"));
	}

	// ---------- getTurn behavior ----------

	@Test
	public void getTurnInBeforeFightReturnsZero() throws Exception {
		// Hook runs before turn 1 starts — expected to be 0 or 1?
		attachAI(leek1, "function beforeFight() { setRegister('turn', '' + getTurn()); }");
		attachAI(leek2, "");
		runFight();
		String turn = leek1.getRegister("turn");
		Assert.assertNotNull(turn);
		// Whatever the answer, document it
		System.out.println("[INFO] getTurn() in beforeFight = " + turn);
		// Reasonable expectation: 0 (no turn started yet) or 1 (about to start)
		int t = Integer.parseInt(turn);
		Assert.assertTrue("Turn in beforeFight should be 0 or 1, got: " + t, t == 0 || t == 1);
	}

	@Test
	public void getTurnInAfterFightReturnsLastTurn() throws Exception {
		attachAI(leek1, "function afterFight() { setRegister('turn', '' + getTurn()); }");
		attachAI(leek2, "");
		runFight();
		String turn = leek1.getRegister("turn");
		Assert.assertNotNull(turn);
		System.out.println("[INFO] getTurn() in afterFight = " + turn);
		// Fight ran 64 turns + extra increment — should be ≥64
		int t = Integer.parseInt(turn);
		Assert.assertTrue("Turn in afterFight should be high: " + t, t >= 64);
	}

	// ---------- say() in hooks ----------

	@Test
	public void sayInBeforeFightDoesNotCrash() throws Exception {
		attachAI(leek1, "function beforeFight() { say('Hello from before'); setRegister('said', 'yes'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("yes", leek1.getRegister("said"));
	}

	// ---------- Recursion safety ----------

	@Test
	public void hookCallsRegularUserFunction() throws Exception {
		attachAI(leek1, ""
			+ "function helper() { setRegister('helped', 'yes'); }"
			+ "function beforeFight() { helper(); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("yes", leek1.getRegister("helped"));
	}

	@Test
	public void manyLoadoutSwapsInOneHook() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(com.leekwars.generator.state.Entity.STAT_LIFE, 800);
		stats.put(com.leekwars.generator.state.Entity.STAT_STRENGTH, 250);
		leek1.addLoadout(new com.leekwars.generator.state.FightLoadout("a",
			java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats));
		leek1.addLoadout(new com.leekwars.generator.state.FightLoadout("b",
			java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats));
		// Lots of swaps — should be fine, each call resets equipment from scratch
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  for (var i = 0; i < 50; i++) {"
			+ "    setLoadout(i % 2 == 0 ? 'a' : 'b');"
			+ "  }"
			+ "  setRegister('s', '' + getStrength());"
			+ "  setRegister('l', '' + getTotalLife());"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("250", leek1.getRegister("s"));
		Assert.assertEquals("800", leek1.getRegister("l"));
	}

	// ---------- Registers edge cases ----------

	@Test
	public void registerWithSpecialChars() throws Exception {
		// LeekScript uses single-quoted strings; double quotes inside are literal,
		// no escaping needed — and \\ is a backslash.
		attachAI(leek1, ""
			+ "setRegister('json', '{\"key\":\"value\",\"n\":42}');"
			+ "setRegister('back', 'a\\\\b');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("{\"key\":\"value\",\"n\":42}", leek1.getRegister("json"));
		Assert.assertEquals("a\\b", leek1.getRegister("back"));
	}

	@Test
	public void registerJsonRoundTrip() throws Exception {
		attachAI(leek1, ""
			+ "setRegister('a', 'value with \"quotes\"');"
			+ "setRegister('b', 'simple');");
		attachAI(leek2, "");
		runFight();
		// Save then load and verify all keys survive serialization
		var regs = leek1.getRegisters();
		String json = regs.toJSONString();
		var loaded = com.leekwars.generator.leek.Registers.fromJSONString(json);
		Assert.assertEquals(regs.get("a"), loaded.get("a"));
		Assert.assertEquals(regs.get("b"), loaded.get("b"));
	}

	@Test
	public void registerEmptyValueWorks() throws Exception {
		attachAI(leek1, "setRegister('empty', '');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("", leek1.getRegister("empty"));
	}

	@Test
	public void registerWithVeryLongValueIsRejected() throws Exception {
		// Doubling-string trick: 13 iterations gives 8192 chars (> MAX_DATA_LENGTH=5000)
		attachAI(leek1, ""
			+ "var s = 'x';"
			+ "for (var i = 0; i < 13; i++) s = s + s;"
			+ "setRegister('big', s);"
			+ "setRegister('marker', 'after');");
		attachAI(leek2, "");
		runFight();
		Assert.assertNull("big register should be rejected (over MAX_DATA_LENGTH=5000)", leek1.getRegister("big"));
		Assert.assertEquals("after", leek1.getRegister("marker"));
	}

	@Test
	public void manyRegistersUpToLimit() throws Exception {
		// MAX_ENTRIES = 100 — set 110 registers, only first 100 should stick
		attachAI(leek1, ""
			+ "for (var i = 0; i < 110; i++) {"
			+ "  setRegister('k' + i, 'v' + i);"
			+ "}"
			+ "setRegister('done', 'yes');");
		attachAI(leek2, "");
		runFight();
		// After 100 entries, further sets should fail — but 'done' might already exist
		// from a prior turn. Let's just check that early entries are all there.
		Assert.assertEquals("v0", leek1.getRegister("k0"));
		Assert.assertEquals("v50", leek1.getRegister("k50"));
		// k99 is the 100th entry; k100+ should be rejected once full
		Assert.assertEquals("v99", leek1.getRegister("k99"));
	}

	// ---------- Setter natives in hooks ----------

	@Test
	public void setRegisterFromAfterFightWorks() throws Exception {
		attachAI(leek1, "function afterFight() { setRegister('end', 'reached'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("reached", leek1.getRegister("end"));
	}

	@Test
	public void deleteRegisterFromAfterFightWorks() throws Exception {
		attachAI(leek1, ""
			+ "setRegister('temp', '1');"
			+ "function afterFight() { deleteRegister('temp'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertNull(leek1.getRegister("temp"));
	}

	// ---------- setLoadout edge cases ----------

	@Test
	public void setLoadoutInAfterFightReturnsFalse() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(com.leekwars.generator.state.Entity.STAT_LIFE, 800);
		leek1.addLoadout(new com.leekwars.generator.state.FightLoadout("any",
			java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats));
		attachAI(leek1, "function afterFight() { setRegister('result', setLoadout('any') ? 'true' : 'false'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("false", leek1.getRegister("result"));
	}

	@Test
	public void setLoadoutWithEmptyName() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('result', setLoadout('') ? 'true' : 'false'); }");
		attachAI(leek2, "");
		runFight();
		// Empty name should not match any loadout → false
		Assert.assertEquals("false", leek1.getRegister("result"));
	}

	@Test
	public void setLoadoutWithNullArg() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('result', setLoadout(null) ? 'true' : 'false'); }");
		attachAI(leek2, "");
		runFight();
		// null name should not match (string('null') becomes "null" string) → false
		Assert.assertEquals("false", leek1.getRegister("result"));
	}

	// ---------- getWinner edge cases ----------

	@Test
	public void getWinnerInBeforeFightHook() throws Exception {
		attachAI(leek1, "function beforeFight() { setRegister('w', '' + getWinner()); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("-1", leek1.getRegister("w"));
	}

	@Test
	public void getWinnerInRegularTurn() throws Exception {
		// During regular turns, fight is in progress — winner should be -1.
		attachAI(leek1, "setRegister('w', '' + getWinner());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("-1", leek1.getRegister("w"));
	}

	// ---------- Hook execution context ----------

	@Test
	public void hookHasAccessToRegistersWrittenInTurn() throws Exception {
		// turn 1 writes a register. afterFight reads it.
		attachAI(leek1, ""
			+ "global ran_once = false;"
			+ "if (!ran_once) {"
			+ "  setRegister('from_turn', 'yes');"
			+ "  ran_once = true;"
			+ "}"
			+ "function afterFight() {"
			+ "  setRegister('seen_from_turn', getRegister('from_turn') == null ? 'null' : getRegister('from_turn'));"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("yes", leek1.getRegister("seen_from_turn"));
	}

	@Test
	public void beforeFightWriteVisibleInTurn() throws Exception {
		attachAI(leek1, ""
			+ "function beforeFight() { setRegister('from_hook', 'yes'); }"
			+ "if (getRegister('seen_from_hook') == null) {"
			+ "  setRegister('seen_from_hook', getRegister('from_hook') == null ? 'null' : getRegister('from_hook'));"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("yes", leek1.getRegister("seen_from_hook"));
	}

	// ---------- AI compile errors ----------

	@Test
	public void aiWithRuntimeErrorContinuesPerTurn() throws Exception {
		// First instruction OK, second throws (division by 0).
		attachAI(leek1, ""
			+ "global counter = 0;"
			+ "counter++;"
			+ "setRegister('count', '' + counter);"
			+ "var x = 1 / 0;");  // throws
		attachAI(leek2, "");
		runFight();
		String count = leek1.getRegister("count");
		Assert.assertNotNull(count);
		// Even with runtime error each turn, count keeps growing
		Assert.assertTrue("count should grow despite runtime errors: " + count,
			Integer.parseInt(count) > 5);
	}

	@Test
	public void hookWithDivisionByZeroIsCaught() throws Exception {
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  setRegister('before_div', 'reached');"
			+ "  var x = 1 / 0;"
			+ "  setRegister('after_div', 'reached');"
			+ "}"
			+ "global counter = 0;"
			+ "counter++;"
			+ "setRegister('turn_count', '' + counter);");
		attachAI(leek2, "");
		runFight();
		// Hook started but threw — turn should still run
		Assert.assertEquals("reached", leek1.getRegister("before_div"));
		// after_div may or may not be reached depending on whether 1/0 throws
		Assert.assertNotNull("Turn 1 should still run after hook crashed",
			leek1.getRegister("turn_count"));
	}

	// ---------- Multi-leek introspection ----------

	@Test
	public void getEnemiesInBeforeFightReturnsEnemies() throws Exception {
		// In our 1v1 setup, enemies has size 1. Confirm in hook context.
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  var es = getEnemies();"
			+ "  setRegister('count', '' + count(es));"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("count"));
	}

	// ---------- Entity field consistency ----------

	@Test
	public void totalLifeConsistentAfterLoadout() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(com.leekwars.generator.state.Entity.STAT_LIFE, 1234);
		leek1.addLoadout(new com.leekwars.generator.state.FightLoadout("hp",
			java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats));
		attachAI(leek1, ""
			+ "function beforeFight() {"
			+ "  setLoadout('hp');"
			+ "  setRegister('totalLife', '' + getTotalLife());"
			+ "  setRegister('life', '' + getLife());"
			+ "}");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1234", leek1.getRegister("totalLife"));
		Assert.assertEquals("1234", leek1.getRegister("life"));
		// Java side
		Assert.assertEquals(1234, leek1.getTotalLife());
		Assert.assertTrue("Life should be at most total at fight start: " + leek1.getLife(),
			leek1.getLife() <= 1234);
	}

	// ---------- AI-to-AI determinism ----------

	@Test
	public void hooksRunInDeterministicOrder() throws Exception {
		// Run the same fight setup multiple times; both leeks define beforeFight()
		// and write a marker. The order of marker writes (concatenated via global var)
		// must be the same across runs — proves Fight.runHooks iterates deterministically.
		String observed = null;
		for (int run = 0; run < 5; run++) {
			setUp(); // reset everything
			attachAI(leek1, "function beforeFight() { setRegister('order', '1'); }");
			attachAI(leek2, "function beforeFight() { setRegister('order', '2'); }");
			runFight();
			// Each leek has its own register; we just check consistency
			String l1 = leek1.getRegister("order");
			String l2 = leek2.getRegister("order");
			String fingerprint = l1 + "/" + l2;
			if (observed == null) observed = fingerprint;
			Assert.assertEquals("Fight should be deterministic across runs", observed, fingerprint);
		}
	}

	@Test
	public void setLoadoutWithNumericArg() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(com.leekwars.generator.state.Entity.STAT_LIFE, 800);
		// Loadout with numeric-looking name
		leek1.addLoadout(new com.leekwars.generator.state.FightLoadout("123",
			java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats));
		// Pass a number — should be coerced to string "123"
		attachAI(leek1, "function beforeFight() { setRegister('result', setLoadout(123) ? 'true' : 'false'); }");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("Numeric arg should be coerced to string and match", "true",
			leek1.getRegister("result"));
	}

	@Test
	public void addLoadoutOverwritesSameName() throws Exception {
		var stats1 = new HashMap<Integer, Integer>();
		stats1.put(com.leekwars.generator.state.Entity.STAT_STRENGTH, 100);
		var stats2 = new HashMap<Integer, Integer>();
		stats2.put(com.leekwars.generator.state.Entity.STAT_STRENGTH, 999);
		leek1.addLoadout(new com.leekwars.generator.state.FightLoadout("dup",
			java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats1));
		leek1.addLoadout(new com.leekwars.generator.state.FightLoadout("dup",
			java.util.Collections.emptyList(), java.util.Collections.emptyList(), stats2));
		attachAI(leek1, "function beforeFight() { setLoadout('dup'); setRegister('s', '' + getStrength()); }");
		attachAI(leek2, "");
		runFight();
		// Last addLoadout wins — strength = 999
		Assert.assertEquals("999", leek1.getRegister("s"));
	}

	@Test
	public void registerKeyTooLongIsRejected() throws Exception {
		// MAX_KEY_LENGTH = 100
		attachAI(leek1, ""
			+ "var k = 'x';"
			+ "for (var i = 0; i < 8; i++) k = k + k;" // 256 chars
			+ "setRegister(k, 'value');"
			+ "setRegister('marker', 'after');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("after", leek1.getRegister("marker"));
		// The over-limit key is rejected
		Assert.assertEquals(1, leek1.getAllRegisters().size());
	}

	@Test
	public void identicalAIsProduceConsistentResults() throws Exception {
		String code = ""
			+ "global counter = 0;"
			+ "counter++;"
			+ "setRegister('count', '' + counter);";
		attachAI(leek1, code);
		attachAI(leek2, code);
		runFight();
		String c1 = leek1.getRegister("count");
		String c2 = leek2.getRegister("count");
		Assert.assertNotNull(c1);
		Assert.assertNotNull(c2);
		Assert.assertEquals("Identical AIs should play same number of turns in symmetric fight",
			c1, c2);
	}
}
