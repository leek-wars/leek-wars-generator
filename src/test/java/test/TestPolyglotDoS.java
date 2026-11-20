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

	@Test(timeout = 60_000)
	public void uninterruptibleNativeDisablesAiAfterOneTurn() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Le parseur ne passe par aucun safepoint : un source de quelques Mo l'occupe plus d'1 s
			// au-dela de l'echeance, que l'interruption ne coupe pas.
			String src = "var a = 0;\n".repeat(120_000) + "function turn(){ return 1; }";
			PolyglotEntityAI ai = ai(sb, "js", src, 200);
			expectTooMuchOperations(ai, "le tour aurait du depasser son echeance");
			assertDisabledReturnsImmediately(ai);
		}
	}

	/** Natif non paye par les ops, sous le watchdog : neutralise en quelques tours, sans attendre 64. */
	private void assertUnpaidNativeWorkDisablesAi(String lang, String src) throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox(lang)) {
			PolyglotEntityAI ai = ai(sb, lang, src, 5_000);
			for (int turn = 0; turn < 6; turn++) {
				try {
					ai.runIA();
				} catch (LeekRunException e) {
					Assert.assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
					assertDisabledReturnsImmediately(ai);
					return;
				}
			}
			Assert.fail("l'IA aurait du etre neutralisee pour calcul natif non paye");
		}
	}

	@Test(timeout = 120_000)
	public void jsUnpaidNativeWorkDisablesAi() throws Exception {
		// 3n ** 40000n : 63 400 bits, sous le plafond ; chaque toString coute ~1 ms pour 1 op.
		assertUnpaidNativeWorkDisablesAi("js", "var x = 3n ** 40000n;\nfunction turn(){ var n = 0; for (var i = 0; i < 1500; i++) n += x.toString().length; return n; }");
	}

	@Test(timeout = 120_000)
	public void pythonUnpaidNativeWorkDisablesAi() throws Exception {
		assertUnpaidNativeWorkDisablesAi("python", "b = bytes(1 << 26)\ndef turn():\n    n = 0\n    for i in range(40):\n        n += b.find(b'\\x01')\n    return n\n");
	}

	/** Calcul guest lourd mais paye par ses ops : jamais neutralise. */
	@Test(timeout = 120_000)
	public void jsHeavyGuestComputeIsNotDisabled() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = ai(sb, "js", "function turn(){ var a = []; for (var i = 0; i < 300000; i++) a.push((i * 7919) % 300007); a.sort(function(x, y){ return x - y; }); var s = 0; for (var j = 0; j < 3000000; j++) s += j % 7; return 1; }", 5_000);
			for (int i = 0; i < 8; i++) {
				Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			}
		}
	}

	/** Un tour qui rend {@code expected} en moins de 3 s : les refus de l'image sont immediats. */
	private void assertQuickResult(String lang, String src, long expected) throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox(lang)) {
			PolyglotEntityAI ai = ai(sb, lang, src, 5_000);
			long start = System.nanoTime();
			Assert.assertEquals(expected, ((Number) ai.runIA()).longValue());
			Assert.assertTrue("refus immediats", (System.nanoTime() - start) / 1_000_000 < 3_000);
		}
	}

	/** Entiers plafonnes dans l'image isolate (patches graaljs / graalpython) : refus immediat, l'IA continue. */
	@Test(timeout = 60_000)
	public void jsBigIntSizeIsCapped() throws Exception {
		String src = "function turn(){ var refused = 0;\n"
			+ " [function(){ return 3n ** 60000000n; }, function(){ return 1n << 100000000n; }, function(){ return BigInt('9'.repeat(5000000)); },"
			+ "  function(){ var x = 2n ** 40000n; return x * x; }, function(){ return BigInt.asUintN(1000000, -1n); }]"
			+ " .forEach(function(f){ try { f(); } catch (e) { if (e instanceof RangeError) refused++; } });\n"
			+ " var ok = (2n ** 65535n).toString(2).length === 65536 && BigInt.asUintN(64, -1n) === 18446744073709551615n;\n"
			+ " return refused * 10 + (ok ? 1 : 0); }";
		assertQuickResult("js", src, 51L);
	}

	@Test(timeout = 60_000)
	public void pythonIntSizeIsCapped() throws Exception {
		String src = "import math, random\n"
			+ "def turn():\n"
			+ "    refused = 0\n"
			+ "    for f in (lambda: 3 ** 60000000, lambda: 1 << 100000000, lambda: (2 ** 40000) * (2 ** 40000),\n"
			+ "              lambda: math.factorial(10 ** 6), lambda: math.comb(10 ** 6, 300000), lambda: math.perm(10 ** 6, 300000),\n"
			+ "              lambda: pow(7, 3 ** 20000 + 1, 3 ** 20000 + 1), lambda: int('f' * 4000000, 16), lambda: random.getrandbits(10 ** 9)):\n"
			+ "        try:\n"
			+ "            f()\n"
			+ "        except OverflowError:\n"
			+ "            refused += 1\n"
			+ "    ok = (2 ** 65535).bit_length() == 65536 and math.factorial(5000) > 0 and pow(7, 2 ** 64 + 13, 10 ** 9 + 7) == 948541898\n"
			+ "    return refused * 10 + (1 if ok else 0)\n";
		assertQuickResult("python", src, 91L);
	}

	/** Boucle native sur une taille virtuelle (sans memoire) : l'interruption du watchdog y atterrit. */
	private void assertNativeLoopInterrupted(String lang, String src) throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox(lang)) {
			PolyglotEntityAI ai = ai(sb, lang, src, 300);
			long start = System.nanoTime();
			expectTooMuchOperations(ai, "la boucle native aurait du etre interrompue");
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			Assert.assertTrue("interrompue vite (etait " + elapsedMs + " ms)", elapsedMs < 1_300);
		}
	}

	@Test(timeout = 60_000)
	public void jsArrayLikeLoopIsInterrupted() throws Exception {
		assertNativeLoopInterrupted("js", "function turn(){ return Array.prototype.indexOf.call({length: 2**53 - 1}, 1); }");
		assertNativeLoopInterrupted("js", "function turn(){ return Array.prototype.shift.call({length: 2**53 - 1}); }");
		assertNativeLoopInterrupted("js", "function turn(){ return Array.prototype.toSorted.call({length: 2**28}).length; }");
	}

	@Test(timeout = 60_000)
	public void pythonIteratorLoopIsInterrupted() throws Exception {
		assertNativeLoopInterrupted("python", "def turn():\n    return max(iter(range(10 ** 10)))\n");
	}

	/** Source trop longue (plafond de l'image, 1 Mo) refusee par chaque chemin d'evaluation ; les autres marchent comme avant. */
	@Test(timeout = 60_000)
	public void jsEvaluatedSourceIsCapped() throws Exception {
		String src = "function turn(){\n"
			+ " var big = '1+'.repeat(600000) + '1';\n"
			+ " var G = Object.getPrototypeOf(function*(){}).constructor, A = Object.getPrototypeOf(async function(){}).constructor,"
			+ "  AG = Object.getPrototypeOf(async function*(){}).constructor;\n"
			+ " var paths = [function(){ eval(big); }, function(){ (0, eval)(big); }, function(){ new Function(big); }, function(){ Function(big); },"
			+ "  function(){ (function(){}).constructor(big); }, function(){ G(big); }, function(){ A(big); }, function(){ AG(big); },"
			+ "  function(){ load({script: big, name: 'x.js'}); }, function(){ new Function({toString: function(){ return big; }}); }];\n"
			+ " var refused = 0;\n"
			+ " paths.forEach(function(f){ try { f(); } catch (e) { if (e instanceof RangeError) refused++; } });\n"
			+ " var loc = 21;\n"
			+ " var ok = eval('loc * 2') === 42 && new Function('a', 'return a*2')(21) === 42 && load({script: '6*7', name: 'y.js'}) === 42"
			+ "  && eval('1+'.repeat(100000) + '1') === 100001 && G('yield 1')().next().value === 1;\n"
			+ " return refused * 10 + (ok ? 1 : 0);\n"
			+ "}";
		assertQuickResult("js", src, 101L);
	}

	/** Python : compile() sert aussi a importer les modules, sans plafond de source ; le parseur s'interrompt. */
	@Test(timeout = 60_000)
	public void pythonBigModulesStillImport() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			String src = "def turn():\n"
				+ "    import _pydecimal\n" // 227 Ko
				+ "    exec('x = 1\\n' * 30000)\n"
				+ "    return 1\n";
			Assert.assertEquals(1L, ((Number) ai(sb, "python", src, 5_000).runIA()).longValue());
		}
	}

	/** Budget d'ops epuise par une facturation, exception attrapee par le guest : le tour finit quand meme en depassement. */
	private void assertCaughtExhaustionEndsTurn(String lang, String src) throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox(lang)) {
			PolyglotEntityAI ai = ai(sb, lang, src, 5_000);
			expectTooMuchOperations(ai, "le depassement attrape par le guest aurait du finir le tour en erreur");
			Assert.assertTrue("ops du tour bornees au budget (etait " + ai.operations() + ")", ai.operations() <= ai.getMaxOperations());
		}
	}

	@Test(timeout = 30_000)
	public void jsCaughtOpsExhaustionEndsTurn() throws Exception {
		assertCaughtExhaustionEndsTurn("js", "function turn(){ for (var i = 0; i < 4; i++) { try { Array.prototype.fill.call({length: 4294967295}, 0); } catch (e) {} } return 1; }");
	}

	@Test(timeout = 30_000)
	public void pythonCaughtOpsExhaustionEndsTurn() throws Exception {
		assertCaughtExhaustionEndsTurn("python", "def turn():\n    for i in range(4):\n        try:\n            sum(range(2 * 10 ** 9))\n        except BaseException:\n            pass\n    return 1\n");
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
