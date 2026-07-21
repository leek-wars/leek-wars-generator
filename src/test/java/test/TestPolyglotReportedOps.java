package test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * REGRESSION #4586 : le nombre d'operations REPORTE au moteur (rapport de combat, ops cumulees de
 * l'entite) doit etre celui que l'IA voit sur {@code System.operations}, pas le seul compteur hote.
 *
 * Avant correctif, {@code operations()} renvoyait {@code mOperations} (travail HOTE uniquement :
 * fonctions de combat + facturation des builtins) : une IA JS/Python qui lisait 10M d'operations
 * n'en affichait que quelques milliers dans le rapport.
 */
public class TestPolyglotReportedOps extends FightTestBase {

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Ops1");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, defaultLeek(2, "Ops2"));
	}

	private PolyglotEntityAI build(PolyglotSandbox sb, String lang, String source) {
		PolyglotEntityAI ai = new PolyglotEntityAI(lang, source, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	/** Le total reporte suit ce que l'IA lit, et n'est pas le compteur hote (quasi nul ici). */
	private void checkReportedOps(PolyglotEntityAI ai) throws Exception {
		long seenByAI = ((Number) ai.runIA()).longValue();
		long reported = ai.operations();
		System.out.printf("vu par l'IA=%d  reporte=%d%n", seenByAI, reported);
		assertTrue("l'IA doit consommer un vrai budget d'ops (" + seenByAI + ")", seenByAI > 100_000);
		// Le snapshot est pris APRES la lecture du guest : jamais plus petit (au terme temps pres,
		// qui ne fait que monter). On borne donc par le bas.
		assertTrue("ops reportees (" + reported + ") trop loin de celles vues par l'IA (" + seenByAI + ")",
			reported >= seenByAI * 9 / 10);
	}

	@Test
	public void reportedOpsMatchGuestOpsJS() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			checkReportedOps(build(sb, "js",
				"function turn() { var s = 0; for (var i = 0; i < 300000; i++) { s += i * i; } return System.operations; }"));
		}
	}

	@Test
	public void reportedOpsMatchGuestOpsPython() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			checkReportedOps(build(sb, "python",
				"def turn():\n    s = 0\n    for i in range(300000):\n        s += i * i\n    return System.operations\n"));
		}
	}

	/** Un tour ou l'IA ne tourne pas ne doit pas reporter le total du tour precedent. */
	@Test
	public void reportedOpsResetBetweenTurns() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = build(sb, "js",
				"function turn() { var s = 0; for (var i = 0; i < 300000; i++) { s += i * i; } return System.operations; }");
			ai.runIA();
			assertTrue(ai.operations() > 100_000);
			ai.resetCounter();
			assertTrue("resetCounter() doit remettre a zero le total reporte", ai.operations() == 0);
		}
	}
}
