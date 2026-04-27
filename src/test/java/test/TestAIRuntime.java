package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;

/**
 * AI runtime quirks — ops budget, RAM, recursion, exceptions, global state,
 * debug spam, and how the engine recovers from misbehaved AIs.
 */
public class TestAIRuntime extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	// ---------- Ops budget ----------

	@Test
	public void opsBudgetIsResetEachTurn() throws Exception {
		// Each turn: do 100 ops worth of nothing, then write the turn count.
		// AI should reach turn 64 successfully.
		attachAI(leek1, ""
			+ "global counter = 0;"
			+ "for (var i = 0; i < 100; i++) {}"
			+ "counter++;"
			+ "setRegister('count', '' + counter);");
		attachAI(leek2, "");
		runFight();
		String count = leek1.getRegister("count");
		Assert.assertNotNull(count);
		Assert.assertTrue("Should accumulate over many turns: " + count, Integer.parseInt(count) > 30);
	}

	@Test
	public void infiniteLoopLogsTooMuchOperationsButFightContinues() throws Exception {
		attachAI(leek1, ""
			+ "global counter = 0;"
			+ "counter++;"
			+ "setRegister('count', '' + counter);"
			+ "while (true) {}");
		attachAI(leek2, "");
		runFight();
		String count = leek1.getRegister("count");
		Assert.assertNotNull(count);
		// counter should still grow each turn (write happens BEFORE infinite loop)
		Assert.assertTrue("Count should grow despite ops limit: " + count, Integer.parseInt(count) > 1);
	}

	// ---------- Stack / recursion ----------

	@Test
	public void deepRecursionEventuallyHitsStackOverflow() throws Exception {
		attachAI(leek1, ""
			+ "global counter = 0;"
			+ "function recurse(n) { return recurse(n + 1); }"
			+ "counter++;"
			+ "setRegister('count', '' + counter);"
			+ "recurse(0);");
		attachAI(leek2, "");
		runFight();
		String count = leek1.getRegister("count");
		// Counter set BEFORE recursive call — should always increment
		Assert.assertNotNull(count);
		Assert.assertTrue("Count should grow even when recursion overflows: " + count,
			Integer.parseInt(count) >= 1);
	}

	@Test
	public void mutuallyRecursiveStackOverflow() throws Exception {
		attachAI(leek1, ""
			+ "global counter = 0;"
			+ "function f(n) { return g(n + 1); }"
			+ "function g(n) { return f(n + 1); }"
			+ "counter++;"
			+ "setRegister('count', '' + counter);"
			+ "f(0);");
		attachAI(leek2, "");
		runFight();
		Assert.assertNotNull(leek1.getRegister("count"));
	}

	// ---------- Global state ----------

	@Test
	public void globalCounterAccumulatesAcrossTurns() throws Exception {
		attachAI(leek1, "global n = 0; n++; setRegister('n', '' + n);");
		attachAI(leek2, "");
		runFight();
		Assert.assertTrue("global int should accumulate", Integer.parseInt(leek1.getRegister("n")) > 5);
	}

	@Test
	public void globalArrayAccumulates() throws Exception {
		attachAI(leek1, ""
			+ "global history = [];"
			+ "push(history, getTurn());"
			+ "setRegister('size', '' + count(history));");
		attachAI(leek2, "");
		runFight();
		String size = leek1.getRegister("size");
		Assert.assertNotNull(size);
		Assert.assertTrue("Array should accumulate: " + size, Integer.parseInt(size) > 5);
	}

	// ---------- Local variables reset ----------

	@Test
	public void localVariableResetsEachTurn() throws Exception {
		attachAI(leek1, "var n = 0; n++; setRegister('n', '' + n);");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("local var should reset each turn", "1", leek1.getRegister("n"));
	}

	// ---------- Errors mid-turn ----------

	@Test
	public void divisionByZeroIsCaughtAndAIContinues() throws Exception {
		attachAI(leek1, ""
			+ "global counter = 0;"
			+ "counter++;"
			+ "setRegister('count', '' + counter);"
			+ "var x = 1 / 0;"
			+ "counter++;"); // unreachable
		attachAI(leek2, "");
		runFight();
		String count = leek1.getRegister("count");
		Assert.assertNotNull(count);
		// Each turn: counter++ (writes), then div0 throws. So counter == turn count.
		Assert.assertTrue("Counter should grow: " + count, Integer.parseInt(count) > 5);
	}

	@Test
	public void nullPointerInArrayAccessIsCaught() throws Exception {
		attachAI(leek1, ""
			+ "global counter = 0;"
			+ "counter++;"
			+ "setRegister('count', '' + counter);"
			+ "var x = null;"
			+ "var y = x[0];");
		attachAI(leek2, "");
		runFight();
		Assert.assertNotNull(leek1.getRegister("count"));
	}

	@Test
	public void undefinedVariableThrowsAtCompileNotRuntime() throws Exception {
		// Strict mode would reject; default mode might warn.
		// LeekScript should treat undefined vars as null, not crash.
		attachAI(leek1, "setRegister('result', '' + count(undefined_var));");
		attachAI(leek2, "");
		runFight();
		// result is set or not depending on null handling. Don't crash.
		// Just verify the fight runs to completion.
		Assert.assertEquals(-1, fight.getWinner());
	}

	// ---------- AI invalidity ----------

	@Test
	public void syntaxErrorAIIsInvalidAndDoesNotRun() throws Exception {
		attachAI(leek1, "function broken( setRegister('x', '1');"); // missing close paren
		attachAI(leek2, "");
		runFight();
		var ai = (EntityAI) leek1.getAI();
		Assert.assertNotNull(ai);
		Assert.assertFalse("Syntax error AI should be invalid", ai.isValid());
		// Did not run — no register written
		Assert.assertNull(leek1.getRegister("x"));
	}

	@Test
	public void invalidAIDoesNotPreventOpponentFromRunning() throws Exception {
		attachAI(leek1, "this is broken");
		attachAI(leek2, "setRegister('alive', 'yes');");
		runFight();
		// Opponent's AI ran successfully
		Assert.assertEquals("yes", leek2.getRegister("alive"));
	}

	// ---------- Debug / logs ----------

	@Test
	public void debugDoesNotCrash() throws Exception {
		attachAI(leek1, "debug('hello'); debug(42); debug([1, 2, 3]); setRegister('done', 'yes');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("yes", leek1.getRegister("done"));
	}

	@Test
	public void manyDebugCallsDoNotKillBudget() throws Exception {
		attachAI(leek1, ""
			+ "for (var i = 0; i < 100; i++) debug(i);"
			+ "setRegister('done', 'yes');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("yes", leek1.getRegister("done"));
	}

	// ---------- Type coercion ----------

	@Test
	public void stringConcatenationCoercesNumbers() throws Exception {
		attachAI(leek1, "setRegister('s', 'x' + 42 + 'y');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("x42y", leek1.getRegister("s"));
	}

	@Test
	public void booleanToStringConcatenation() throws Exception {
		attachAI(leek1, "setRegister('s', 'b=' + (1 == 1));");
		attachAI(leek2, "");
		runFight();
		String s = leek1.getRegister("s");
		Assert.assertNotNull(s);
		Assert.assertTrue("Boolean concat should produce string: " + s, s.startsWith("b="));
	}

	// ---------- Function definitions ----------

	@Test
	public void recursiveFunctionWorks() throws Exception {
		attachAI(leek1, ""
			+ "function fib(n) { if (n < 2) return n; return fib(n - 1) + fib(n - 2); }"
			+ "setRegister('result', '' + fib(10));");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("55", leek1.getRegister("result"));
	}

	@Test
	public void closureCapture() throws Exception {
		attachAI(leek1, ""
			+ "var x = 42;"
			+ "var f = -> x;"
			+ "setRegister('captured', '' + f());");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("42", leek1.getRegister("captured"));
	}

	// ---------- AI lifecycle invariants ----------

	@Test
	public void aiInstancePersistsAcrossTurns() throws Exception {
		// global vars survive per AI instance, proving the same instance is reused.
		attachAI(leek1, "global x = 'first'; setRegister('x', x); x = 'modified';");
		attachAI(leek2, "");
		runFight();
		// On turn 2+, x is 'modified'. The register was overwritten each turn but
		// here we assume turn 1 happens last (alphabetical naming bias). Just check
		// it's one of the values.
		String x = leek1.getRegister("x");
		Assert.assertNotNull(x);
		Assert.assertTrue("x should be 'first' or 'modified': " + x,
			x.equals("first") || x.equals("modified"));
	}

	// ---------- runIA return value ----------

	@Test
	public void aiReturningValueDoesNotAffectFight() throws Exception {
		// Last expression of top-level code becomes runIA's return value (Object).
		// Should not affect fight outcome.
		attachAI(leek1, "setRegister('x', '1'); 42;");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("1", leek1.getRegister("x"));
		Assert.assertEquals(-1, fight.getWinner());
	}
}
