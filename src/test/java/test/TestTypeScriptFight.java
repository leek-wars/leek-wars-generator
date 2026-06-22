package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * Support des IA en TypeScript : un fichier {@code .ts} est transpile en JS au build
 * ({@code ts.transpileModule}) puis execute par le pipeline polyglot identique au JS.
 * On verifie que du TS TYPE (annotations, enum, generics, type de retour) joue un VRAI
 * combat multi-tours en appelant l'API de combat, et qu'une erreur de syntaxe TS rend l'IA inerte.
 */
public class TestTypeScriptFight extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "TsLeek");
		leek2 = defaultLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** Attache une IA via un AIFile dont le chemin se termine par .ts (-> dispatch polyglot + transpilation). */
	private void attachTsAI(Leek leek, String code) {
		AIFile file = new AIFile("ts_test_" + System.nanoTime() + ".ts", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	private static int extractTurns(String registersJson) {
		var m = java.util.regex.Pattern.compile("\"turns\"\\s*:\\s*\"?(\\d+)\"?").matcher(registersJson);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	@Test
	public void typedTsAiRunsInFullMultiTurnFight() throws Exception {
		// TS type : annotations sur la static, type de retour void, enum, narrowing.
		String ts = String.join("\n",
			"enum Phase { Start, Mid }",
			"class C { static n: number = 0; }",
			"function turn(): void {",
			"  C.n = C.n + 1;",
			"  const p: Phase = C.n < 3 ? Phase.Start : Phase.Mid;",
			"  setRegister('turns', '' + C.n);",
			"  setRegister('phase', '' + p);",
			"}");
		attachTsAI(leek1, ts);
		attachAI(leek2, ""); // adversaire LeekScript inerte
		runFight();

		Assert.assertTrue("leek1 doit utiliser une IA polyglot (TS->JS)", leek1.getAI() instanceof PolyglotEntityAI);
		String registers = registerStore.get(leek1.getId());
		Assert.assertNotNull("les registres de leek1 doivent etre persistes", registers);
		int turns = extractTurns(registers);
		Assert.assertTrue("turn() doit s'etre execute sur plusieurs tours (static persistante), recu: " + turns, turns > 1);
	}

	@Test
	public void twoTypedTsAisMoveTowardEachOther() throws Exception {
		// IA TS typee appelant l'API de combat (getNearestEnemy, getCell, moveToward).
		String mover = String.join("\n",
			"function turn(): void {",
			"  const e: number = getNearestEnemy();",
			"  const before: number = getCellDistance(getCell(), getCell(e));",
			"  if (getRegister('startDist') == null) setRegister('startDist', '' + before);",
			"  moveToward(e);",
			"  setRegister('endDist', '' + getCellDistance(getCell(), getCell(e)));",
			"}");
		attachTsAI(leek1, mover);
		attachTsAI(leek2, mover);
		runFight();

		int start1 = Integer.parseInt(leek1.getRegister("startDist"));
		int end1 = Integer.parseInt(leek1.getRegister("endDist"));
		int start2 = Integer.parseInt(leek2.getRegister("startDist"));
		int end2 = Integer.parseInt(leek2.getRegister("endDist"));

		System.out.println("[demo] === Combat avec 2 IA TypeScript ===");
		System.out.println("[demo] leek1 (TS) : distance ennemi " + start1 + " -> " + end1);
		System.out.println("[demo] leek2 (TS) : distance ennemi " + start2 + " -> " + end2);

		Assert.assertTrue("leek1 (TS) doit s'etre rapproche : " + start1 + " -> " + end1, end1 < start1);
		Assert.assertTrue("leek2 (TS) doit s'etre rapproche : " + start2 + " -> " + end2, end2 < start2);
		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertTrue(leek2.getAI() instanceof PolyglotEntityAI);
	}

	@Test
	public void invalidTsSyntaxMakesAiInert() throws Exception {
		// Source TS avec une vraie erreur de syntaxe -> rejete au build (diagnostics tsc), IA inerte.
		attachTsAI(leek1, "function turn(): void { return ( }");
		attachAI(leek2, "");
		runFight();
		Assert.assertFalse("une IA TS au source invalide ne doit pas devenir polyglot",
			leek1.getAI() instanceof PolyglotEntityAI);
	}
}
