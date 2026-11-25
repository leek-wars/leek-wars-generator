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

	/** System.operations consommees par {@code body} dans un tour JS. */
	private long jsOps(PolyglotSandbox sb, String setup, String body) throws Exception {
		leek1.setCores(200);
		return ((Number) ai(sb, "js", setup + "\nfunction turn(){ var o = System.operations; " + body + "; return System.operations - o; }").runIA()).longValue();
	}

	@Test
	public void jsNativeCopiesStayChargedWhenApplyIsRedefined() throws Exception {
		// Les enveloppes appelaient o.apply, resolu sur Function.prototype a chaque appel : un apply
		// redefini par le joueur sautait la facturation des copies (repeat, slice, Array.from)
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			String hijack = "var A = Function.prototype.apply; Function.prototype.apply = function(t, a){ return 0; };";
			String body = "var s = 'x'.repeat(1000000); var b = new Array(1000000).fill(0).slice(); var c = Array.from(b)";
			assertTrue("copies facturees", jsOps(sb, "", body) >= 3_000_000);
			assertTrue("copies facturees malgre apply", jsOps(sb, hijack, body) >= 3_000_000);
		}
	}

	@Test
	public void jsNativesThatCopyOrAllocateAreCharged() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			String setup = "var a = new Array(1000000).fill(1);";
			assertTrue("toSorted", jsOps(sb, setup, "a.toSorted()") >= 1_000_000);
			assertTrue("toReversed", jsOps(sb, setup, "a.toReversed()") >= 1_000_000);
			assertTrue("JSON.stringify", jsOps(sb, setup, "JSON.stringify(a)") >= 1_000_000);
			assertTrue("padStart", jsOps(sb, "", "'x'.padStart(1000000)") >= 1_000_000);
			assertTrue("split", jsOps(sb, "var s = 'x,'.repeat(500000);", "s.split(',')") >= 1_000_000);
			assertTrue("ArrayBuffer", jsOps(sb, "", "new ArrayBuffer(8000000)") >= 1_000_000);
			assertTrue("ArrayBuffer (taille en chaine)", jsOps(sb, "", "new ArrayBuffer('8000000')") >= 1_000_000);
			assertTrue("Float64Array", jsOps(sb, "", "new Float64Array(1000000)") >= 1_000_000);
			assertTrue("Float64Array.from", jsOps(sb, setup, "Float64Array.from(a)") >= 1_000_000);
			assertTrue("TypedArray.slice", jsOps(sb, "var f = new Float64Array(1000000);", "f.slice()") >= 1_000_000);
			// Le constructeur d'origine n'est pas rejoignable par une instance
			assertTrue("constructor", jsOps(sb, "", "new (new ArrayBuffer(8).constructor)(8000000)") >= 1_000_000);
			// Semantique inchangee : instanceof, erreurs du natif, vue sur un tampon (gratuite)
			Assert.assertEquals(1L, jsOps(sb, "", "return (new ArrayBuffer(8) instanceof ArrayBuffer) && (new Uint8Array(4) instanceof Uint8Array) ? 1 : 0"));
		}
	}

	@Test
	public void jsHugeBufferIsRefusedBeforeItIsAllocated() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = ai(sb, "js", "function turn(){ return new ArrayBuffer(2000000000).byteLength; }");
			leek1.setCores(1);
			try {
				ai.runIA();
				Assert.fail("un tampon de 2 Go depasse un budget d'un million d'operations");
			} catch (leekscript.runner.LeekRunException e) {
				Assert.assertEquals(leekscript.common.Error.TOO_MUCH_OPERATIONS, e.getError());
			}
		}
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

	/**
	 * Recherches, comparaisons et fonctions de chaine au tarif de LeekScript : 1 par element parcouru
	 * (inArray), 1 + 1 par 10 caracteres cherches (indexOf), 1 par caractere decoupe ou remplace, 1 par
	 * element joint ou converti.
	 */
	@Test
	public void searchesAndStringFunctionsAreCounted() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			String setup = "l = list(range(10000))\nl2 = list(range(10000))\ns = 'a' * 100000\nb = bytes(100000)\nss = [str(i) for i in range(10000)]";
			String loop = "    for k in range(10):\n        ";
			assertTrue("x in l", ops(sb, setup, loop + "9999 in l") >= 100_000);
			assertTrue("l.index", ops(sb, setup, loop + "l.index(9999)") >= 100_000);
			assertTrue("l.count", ops(sb, setup, loop + "l.count(9999)") >= 100_000);
			assertTrue("l == l2", ops(sb, setup, loop + "l == l2") >= 100_000);
			assertTrue("s.find", ops(sb, setup, loop + "s.find('b')") >= 100_000);
			assertTrue("'b' in s", ops(sb, setup, loop + "'b' in s") >= 100_000);
			assertTrue("b.find", ops(sb, setup, loop + "b.find(b'x')") >= 100_000);
			assertTrue("s.split", ops(sb, setup, loop + "s.split('b')") >= 1_000_000);
			assertTrue("s.replace", ops(sb, setup, loop + "s.replace('a', 'b')") >= 1_000_000);
			assertTrue("join", ops(sb, setup, loop + "','.join(ss)") >= 100_000);
			assertTrue("repr", ops(sb, setup, loop + "str(l)") >= 100_000);
			assertTrue("l.remove", ops(sb, setup, loop + "l.remove(9999)\n        l.append(9999)") >= 100_000);
			assertTrue("partition", ops(sb, setup, loop + "s.partition('b')") >= 100_000);
			assertTrue("deque", ops(sb, setup + "\nimport collections\nd = collections.deque(l)", loop + "9999 in d\n        d.count(1)") >= 200_000);
			assertTrue("array", ops(sb, setup + "\nimport array\na = array.array('i', l)", loop + "9999 in a\n        a.count(1)") >= 200_000);
			// Une plage inversee ne rend pas d'operations
			assertTrue("count, plage inversee", ops(sb, setup, loop + "s.count('x', len(s) - 1, 0)") >= 0);
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
			assertEquals(0L, ((Number) ai(sb, "import __graalpython__ as g\nint(hasattr(g, 'lw_set_operations')) + int(hasattr(g, 'time_millis'))").runIA()).longValue());
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
