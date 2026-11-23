package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.common.Error;
import leekscript.runner.LeekRunException;

/**
 * Un seul compteur d'operations par contexte (image combined-11) : les statements et expressions de
 * l'instrument, et en Python les elements parcourus par les boucles Java des builtins (map, sum,
 * ensembles, dict...), sans compter deux fois une boucle for. Sa limite est le budget du tour.
 */
public class TestPolyglotNativeOps extends FightTestBase {

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Native1");
		fight.getState().addEntity(0, leek1);
		Leek leek2 = defaultLeek(2, "Native2");
		fight.getState().addEntity(1, leek2);
	}

	private PolyglotEntityAI ai(PolyglotSandbox sb, String src) {
		return ai(sb, "python", src);
	}

	private PolyglotEntityAI ai(PolyglotSandbox sb, String lang, String src) {
		PolyglotEntityAI ai = new PolyglotEntityAI(lang, src, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	/** System.operations consommees par {@code body} dans un tour (difference avant/apres). */
	private long ops(PolyglotSandbox sb, String setup, String body) throws Exception {
		return ((Number) ai(sb, setup + "\ndef turn():\n    o = System.operations\n" + body + "\n    return System.operations - o\n").runIA()).longValue();
	}

	@Test
	public void nativeIterationIsCounted() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			String setup = "a = [float(i) for i in range(10000)]\nb = [2.0] * 10000";
			long mapSum = ops(sb, setup, "    for k in range(10):\n        sum(map(float.__mul__, a, b))");
			// 10 x (2 x 10 000 elements de map + 10 000 de sum) : bien plus que les quelques statements
			assertTrue("map/sum natifs comptes (" + mapSum + ")", mapSum >= 200_000);
		}
	}

	@Test
	public void setDictAndSortOperationsAreCounted() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			String setup = "s = set(range(5000))\nt = set(range(2500, 7500))\nd = {i: i for i in range(5000)}";
			assertTrue("intersection comptee", ops(sb, setup, "    for k in range(10):\n        s & t") >= 25_000);
			assertTrue("dict.update compte", ops(sb, setup, "    for k in range(10):\n        e = {}\n        e.update(d)") >= 50_000);
			assertTrue("list.sort compte", ops(sb, setup + "\nl = list(range(5000))", "    for k in range(10):\n        l.sort()") >= 50_000);
		}
	}

	/** Une boucle for est deja comptee par l'instrument : parcourir un dict ne coute pas plus qu'une liste. */
	@Test
	public void forLoopIsNotCountedTwice() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			String setup = "l = list(range(10000))\nd = dict.fromkeys(l)";
			long overList = ops(sb, setup, "    for x in l:\n        pass");
			long overDict = ops(sb, setup, "    for x in d:\n        pass");
			assertTrue("liste " + overList + ", dict " + overDict, Math.abs(overDict - overList) < 100);
		}
	}

	/** Ni le compteur (remise a zero) ni l'heure reelle de __graalpython__ ne sont a portee du joueur. */
	@Test
	public void internalsAreHidden() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			assertEquals(0L, ((Number) ai(sb, "import __graalpython__ as g\nint(hasattr(g, 'lw_native_ops')) + int(hasattr(g, 'time_millis')) + int('__lw_native' in globals())").runIA()).longValue());
		}
	}

	/** Tour qui doit finir en depassement du budget (un million d'operations), bien avant les 20M de statements. */
	private void expectBudgetEndsTurn(PolyglotSandbox sb, String lang, String src) throws Exception {
		PolyglotEntityAI ai = ai(sb, lang, src);
		leek1.setCores(1); // un million d'operations
		long start = System.nanoTime();
		try {
			ai.runIA();
			Assert.fail("le budget aurait du finir le tour : " + src);
		} catch (LeekRunException e) {
			assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
		}
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;
		assertTrue("coupe au budget (" + elapsedMs + " ms)", elapsedMs < 3_000);
		assertTrue("ops du tour bornees au budget (" + ai.operations() + ")", ai.operations() <= ai.getMaxOperations());
	}

	@Test(timeout = 60_000)
	public void statementsStopAtBudget() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			expectBudgetEndsTurn(sb, "python", "def turn():\n    while True:\n        pass\n");
			expectBudgetEndsTurn(sb, "python", "def turn():\n    try:\n        while True:\n            pass\n    except BaseException:\n        pass\n    return 1\n");
			expectBudgetEndsTurn(sb, "js", "function turn() { for (;;) {} }");
			expectBudgetEndsTurn(sb, "js", "function turn() { try { for (;;) {} } catch (e) {} return 1; }");
		}
	}

	/** Natif (~800 000) et statements (~600 000) passent chacun sous le budget d'un million : ensemble, ils le depassent. */
	@Test(timeout = 60_000)
	public void statementsAndNativeShareTheBudget() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			expectBudgetEndsTurn(sb, "python", "l = list(range(10000))\ndef turn():\n    for k in range(40):\n        sum(map(abs, l))\n    for i in range(150000):\n        x = i + 1\n    return 1\n");
		}
	}

	/**
	 * Le guest ne voit qu'un lecteur du compteur : retrouve par introspection des closures de System,
	 * il ne remet pas le compte a zero et ne leve pas la limite.
	 */
	@Test(timeout = 60_000)
	public void guestCannotResetOrLiftTheCounter() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			expectBudgetEndsTurn(sb, "python", ""
				+ "def walk(f, seen):\n"
				+ "    for cell in (getattr(f, '__closure__', None) or ()):\n"
				+ "        try:\n"
				+ "            v = cell.cell_contents\n"
				+ "        except ValueError:\n"
				+ "            continue\n"
				+ "        if callable(v) and id(v) not in seen:\n"
				+ "            seen.add(id(v))\n"
				+ "            for args in (('reset',), ('turn', 10 ** 15, 1.0, 0), ('host', -10 ** 15)):\n"
				+ "                try:\n"
				+ "                    v(*args)\n"
				+ "                except BaseException:\n"
				+ "                    pass\n"
				+ "            walk(v, seen)\n"
				+ "def turn():\n"
				+ "    seen = set()\n"
				+ "    for name in dir(type(System)):\n"
				+ "        a = getattr(type(System), name, None)\n"
				+ "        for fn in (getattr(a, 'fget', None), a):\n"
				+ "            if fn is not None:\n"
				+ "                walk(fn, seen)\n"
				+ "    while True:\n"
				+ "        pass\n");
		}
	}
}
