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

	private static void expectTooMuchOperations(PolyglotEntityAI ai, String message) throws Exception {
		try {
			ai.runIA();
			Assert.fail(message);
		} catch (LeekRunException e) {
			Assert.assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
		}
	}

	/** Tour suivant d'une IA neutralisee : retour immediat (null), sans attendre le watchdog. */
	private static void assertDisabledReturnsImmediately(PolyglotEntityAI ai) throws Exception {
		long start = System.nanoTime();
		Object r = ai.runIA();
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;
		Assert.assertNull("une IA neutralisee ne doit rien renvoyer", r);
		Assert.assertTrue("une IA neutralisee doit rendre la main aussitot (etait " + elapsedMs + " ms)", elapsedMs < 100);
	}

	@Test(timeout = 30_000)
	public void wallClockWatchdogStopsRunawayTurn() throws Exception {
		initFightOnly();
		// Statement limit enorme -> n'intervient pas : seul le watchdog wall-clock peut arreter la boucle.
		try (PolyglotSandbox sb = new PolyglotSandbox(Long.MAX_VALUE, "js")) {
			PolyglotEntityAI ai = ai(sb, "js", "while (true) {}", 400);
			long start = System.nanoTime();
			expectTooMuchOperations(ai, "une boucle infinie aurait du etre coupee par le watchdog wall-clock");
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
				expectTooMuchOperations(ai, "depassement attendu au tour " + i);
			}
			assertDisabledReturnsImmediately(ai);
		}
	}

	/** Tours qui attendent sans calculer : le 2e epuise le budget d'attente. */
	private void assertIdleAiDisabled(String src) throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			PolyglotEntityAI ai = ai(sb, "python", src, 5_000);
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue()); // 0,5 s comptees sur 1 s
			expectTooMuchOperations(ai, "le budget d'attente du combat aurait du etre epuise");
			assertDisabledReturnsImmediately(ai);
		}
	}

	/** Tour Python rapide (hors construction du contexte, au 1er tour) qui renvoie {@code expected}. */
	private void assertFastPythonTurn(String src, long expected, String what) throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			PolyglotEntityAI ai = ai(sb, "python", src, 5_000);
			ai.runIA();
			long start = System.nanoTime();
			Assert.assertEquals(what, expected, ((Number) ai.runIA()).longValue());
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			Assert.assertTrue(what + " : tour trop long (" + elapsedMs + " ms)", elapsedMs < 1_000);
		}
	}

	@Test(timeout = 30_000)
	public void pythonSleepIsForbidden() throws Exception {
		assertFastPythonTurn("import time\nfrom time import sleep\ndef turn():\n    n = 0\n    for f in (time.sleep, sleep):\n        try:\n            f(3)\n        except RuntimeError:\n            n += 1\n    return n\n",
			2, "time.sleep doit lever une erreur sans attendre");
	}

	@Test(timeout = 30_000)
	public void pythonGcCollectIsFree() throws Exception {
		assertFastPythonTurn("import gc\ndef turn():\n    return sum(gc.collect() for i in range(1000))\n",
			0, "gc.collect ne doit plus forcer de GC");
	}

	@Test(timeout = 30_000)
	public void pythonEventWaitDisablesAi() throws Exception {
		assertIdleAiDisabled("import threading\ndef turn():\n    threading.Event().wait(0.6)\n    return 1\n");
	}

	@Test(timeout = 30_000)
	public void pythonLockWaitDisablesAi() throws Exception {
		// Les verrous C de GraalPy sont immuables : seule la mesure hote peut couper cette attente.
		assertIdleAiDisabled("import _thread\ndef turn():\n    l = _thread.allocate_lock()\n    l.acquire()\n    l.acquire(True, 0.6)\n    return 1\n");
	}

	@Test(timeout = 60_000)
	public void pythonComputeIsNotCountedAsIdle() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			PolyglotEntityAI ai = ai(sb, "python", "def turn():\n    s = 0\n    for i in range(2000000):\n        s += i\n    return 1\n", 5_000);
			for (int i = 0; i < 8; i++) {
				Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			}
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
