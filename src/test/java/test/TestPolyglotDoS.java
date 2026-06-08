package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.common.Error;
import leekscript.runner.LeekRunException;

/**
 * Anti-DoS : le backstop wall-clock coupe un tour qui depasse son echeance (typiquement du travail
 * natif que le statement limit ne compte pas), et neutralise une IA qui depasse de maniere repetee.
 * On NEUTRALISE le statement limit (limite enorme) pour isoler le watchdog wall-clock : sinon une
 * boucle pure serait coupee par le statement limit avant l'echeance.
 */
public class TestPolyglotDoS extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "DoS1");
		leek2 = defaultLeek(2, "DoS2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private PolyglotEntityAI ai(PolyglotSandbox sb, String lang, String src, long limitMs) {
		PolyglotEntityAI ai = new PolyglotEntityAI(lang, src, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		ai.setTurnWallClockLimitMs(limitMs);
		return ai;
	}

	@Test(timeout = 30_000)
	public void wallClockWatchdogStopsRunawayTurn() throws Exception {
		initFightOnly();
		// Statement limit enorme -> n'intervient pas : seul le watchdog wall-clock peut arreter la boucle.
		try (PolyglotSandbox sb = new PolyglotSandbox(Long.MAX_VALUE, "js")) {
			PolyglotEntityAI ai = ai(sb, "js", "while (true) {}", 400);
			long start = System.nanoTime();
			try {
				ai.runIA();
				Assert.fail("une boucle infinie aurait du etre coupee par le watchdog wall-clock");
			} catch (LeekRunException e) {
				Assert.assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
			}
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			Assert.assertTrue("le tour aurait du etre coupe rapidement (etait " + elapsedMs + " ms)", elapsedMs < 10_000);
		}
	}

	@Test(timeout = 30_000)
	public void aiDisabledAfterRepeatedTimeouts() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox(Long.MAX_VALUE, "js")) {
			PolyglotEntityAI ai = ai(sb, "js", "while (true) {}", 300);
			// 3 depassements -> l'IA est neutralisee (borne le cout total au lieu de payer la limite chaque tour).
			for (int i = 0; i < 3; i++) {
				try {
					ai.runIA();
					Assert.fail("depassement attendu au tour " + i);
				} catch (LeekRunException e) {
					Assert.assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
				}
			}
			// Tour suivant : IA neutralisee -> retour immediat (null), sans attendre le watchdog.
			long start = System.nanoTime();
			Object r = ai.runIA();
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			Assert.assertNull("une IA neutralisee ne doit rien renvoyer", r);
			Assert.assertTrue("une IA neutralisee doit rendre la main aussitot (etait " + elapsedMs + " ms)", elapsedMs < 100);
		}
	}

	@Test(timeout = 30_000)
	public void normalTurnIsNotAffectedByWatchdog() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Tour rapide : le watchdog ne se declenche pas, l'IA n'est jamais neutralisee.
			PolyglotEntityAI ai = ai(sb, "js", "function turn(){ return 42; }", 5_000);
			for (int i = 0; i < 5; i++) {
				Assert.assertEquals(42L, ((Number) ai.runIA()).longValue());
			}
		}
	}
}
