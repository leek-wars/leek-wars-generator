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
 * Operations NATIVES Python (image combined-10, PythonContext.lwCharge) : les elements parcourus par
 * les boucles Java des builtins (map, sum, list, ensembles, dict...) comptent dans System.operations,
 * sans compter deux fois une boucle for, que l'instrument compte deja.
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
		PolyglotEntityAI ai = new PolyglotEntityAI("python", src, sb);
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
	public void setAndDictBulkOperationsAreCounted() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			String setup = "s = set(range(5000))\nt = set(range(2500, 7500))\nd = {i: i for i in range(5000)}";
			assertTrue("intersection comptee", ops(sb, setup, "    for k in range(10):\n        s & t") >= 25_000);
			assertTrue("dict.update compte", ops(sb, setup, "    for k in range(10):\n        e = {}\n        e.update(d)") >= 50_000);
			assertTrue("dict.copy compte", ops(sb, setup, "    for k in range(10):\n        d.copy()") >= 50_000);
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

	/** Natif sans fin : le plafond du tour le coupe, meme attrape, et le tour finit en depassement. */
	@Test(timeout = 60_000)
	public void nativeLimitEndsTurnEvenIfCaught() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox(2_000_000, "python")) {
			for (String body : new String[] {
				"    sum(map(abs, itertools.repeat(1)))",
				"    try:\n        sum(map(abs, itertools.repeat(1)))\n    except BaseException:\n        pass\n    return 1",
			}) {
				try {
					ai(sb, "import itertools\ndef turn():\n" + body + "\n").runIA();
					Assert.fail("le plafond natif aurait du finir le tour : " + body);
				} catch (LeekRunException e) {
					assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
				}
			}
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
}
