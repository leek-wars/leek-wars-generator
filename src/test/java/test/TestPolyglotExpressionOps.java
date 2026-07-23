package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * COMPTAGE PAR EXPRESSION (JS). Depuis l'image isolate v25.1.3-combined-2, l'instrument compte
 * pour JS les statements + expressions (chaque operateur/lecture/appel d'une ligne complexe,
 * comme le « 1 op par operateur » LeekScript), scale par opsFactor(js)=0.6 ; Python reste a la
 * granularite statement. Ces tests verrouillent les invariants (granularite, valeur entiere,
 * parite de facturation des builtins) sans figer les comptes exacts, recalibrables.
 */
public class TestPolyglotExpressionOps extends FightTestBase {

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Expr1");
		fight.getState().addEntity(0, leek1);
		Leek leek2 = defaultLeek(2, "Expr2");
		fight.getState().addEntity(1, leek2);
	}

	private long run(PolyglotSandbox sb, String language, String turnBody) throws Exception {
		PolyglotEntityAI ai = new PolyglotEntityAI(language,
			"python".equals(language)
				? "def turn():\n" + turnBody
				: "function turn() {\n" + turnBody + "\n}", sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ((Number) ai.runIA()).longValue();
	}

	/**
	 * Une ligne complexe (beaucoup d'operateurs) doit couter nettement plus cher qu'une ligne
	 * simple a nombre d'iterations egal. Sous l'ancienne granularite statement, les deux boucles
	 * coutaient PAREIL (1 statement/iteration) : ce test echoue si l'image retombe en statements.
	 */
	@Test
	public void jsComplexLineCostsEachOperation() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			long simple = run(sb, "js",
				"var s = 0; for (var i = 0; i < 20000; i++) { s += i; }\nreturn System.operations;");
			long complex = run(sb, "js",
				"var s = 0; for (var i = 0; i < 20000; i++) { s += ((i * 3 + 1) ^ (i - 2)) / (i + 1) + (i % 7) * (s & 15); }\nreturn System.operations;");
			assertTrue("ligne complexe (" + complex + ") doit couter > 1.5x la ligne simple (" + simple + ")",
				complex > simple * 1.5);
		}
	}

	/**
	 * System.operations doit rester ENTIER cote guest malgre le facteur non entier (0.6) :
	 * l'override guest tronque (Math.floor / int()) comme le cast (long) hote.
	 */
	@Test
	public void jsOperationsStaysIntegral() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = new PolyglotEntityAI("js",
				"function turn() { var s = 0; for (var i = 0; i < 12345; i++) { s += i * i; }\n"
				+ "var o = System.operations; return o - Math.floor(o); }", sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			assertEquals("System.operations ne doit pas avoir de partie fractionnaire",
				0.0, ((Number) ai.runIA()).doubleValue(), 0.0);
		}
	}

	/**
	 * PARITE BUILTINS : un builtin natif enveloppe (sort sans comparateur) doit couter ~1 op par
	 * element (comme LeekScript et comme une boucle explicite equivalente), PAS 1 x opsFactor.
	 * Regression du fix builtinOpsFactor : a 0.6, sort(20k) coutait ~12k au lieu de ~20k.
	 */
	@Test
	public void jsWrappedBuiltinBilledAtParity() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			long delta = run(sb, "js",
				"var a = []; for (var i = 0; i < 20000; i++) { a.push((i * 2654435761) % 100000); }\n"
				+ "var before = System.operations;\n"
				+ "a.sort();\n"
				+ "return System.operations - before;");
			assertTrue("sort(20000) doit facturer ~20k ops (1/element), obtenu " + delta,
				delta >= 18000 && delta <= 30000);
		}
	}

	/** Python reste a la granularite STATEMENT : ligne complexe ~= ligne simple a iterations egales. */
	@Test
	public void pythonKeepsStatementGranularity() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			long simple = run(sb, "python",
				"    s = 0\n    for i in range(20000):\n        s += i\n    return System.operations");
			long complex = run(sb, "python",
				"    s = 0\n    for i in range(20000):\n        s += ((i * 3 + 1) ^ (i - 2)) // (i + 1) + (i % 7) * (s & 15)\n    return System.operations");
			assertTrue("Python : ligne complexe (" + complex + ") doit rester ~= ligne simple (" + simple + ")",
				complex < simple * 1.3);
		}
	}
}
