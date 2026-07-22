package test;

import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * CALIBRATION inter-langages (LeekScript / JS / Python) du compteur d'operations et du cap RAM.
 *
 * Question : le budget d'ops (cores*1M) et le cap RAM (min(50,RAM)*8Mo) sont IDENTIQUES pour les
 * 3 langages, mais un "statement" compte-t-il le meme travail partout ? Si un algo identique
 * consomme 2x moins d'ops en Python qu'en LeekScript, le joueur Python fait 2x plus de calcul par
 * tour = avantage en arene. On execute des snippets EQUIVALENTS a nombre d'iterations FIXE dans les
 * 3 langages, via le VRAI moteur, et on compare getOperations().
 *
 * Purement diagnostique (imprime un tableau), n'echoue pas : sert a decider la calibration.
 */
public class TestOpsCalibration extends FightTestBase {

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Cal1");
		fight.getState().addEntity(0, leek1);
		Leek leek2 = defaultLeek(2, "Cal2");
		fight.getState().addEntity(1, leek2);
	}

	// --- runners : chaque snippet fait un travail FIXE puis renvoie getOperations() ---

	// -1 = le langage a atteint son budget d'ops (TOO_MUCH) ou a leve : cellule "epuise"
	private long lsOps(String body) {
		try {
			AIFile file = new AIFile("cal_ls_" + System.nanoTime(), body + "\nreturn getOperations();",
				System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek1.getId(), false);
			leek1.setAIFile(file);
			leek1.setLogs(new LeekLog(farmerLog, leek1));
			leek1.setFight(fight);
			EntityAI ai = EntityAI.build(generator, file, leek1);
			return ((Number) ai.runIA()).longValue();
		} catch (Exception e) {
			return -1;
		}
	}

	private long jsOps(PolyglotSandbox sb, String body) {
		try {
			PolyglotEntityAI ai = new PolyglotEntityAI("js", "function turn() {\n" + body + "\nreturn System.operations;\n}", sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			return ((Number) ai.runIA()).longValue();
		} catch (Exception e) {
			return -1;
		}
	}

	private long pyOps(PolyglotSandbox sb, String bodyIndented) {
		try {
			PolyglotEntityAI ai = new PolyglotEntityAI("python",
				"def turn():\n" + bodyIndented + "\n    return System.operations\n", sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			return ((Number) ai.runIA()).longValue();
		} catch (Exception e) {
			return -1;
		}
	}

	private void row(String label, long ls, long js, long py) {
		// ratios normalises sur LeekScript (reference = ce que les joueurs connaissent)
		String jsR = (ls <= 0 || js < 0) ? "  -  " : String.format("x%.2f", (double) js / ls);
		String pyR = (ls <= 0 || py < 0) ? "  -  " : String.format("x%.2f", (double) py / ls);
		System.out.printf("%-26s LS=%-9s JS=%-9s(%s) PY=%-9s(%s)%n",
			label, ls < 0 ? "EPUISE" : ls, js < 0 ? "EPUISE" : js, jsR, py < 0 ? "EPUISE" : py, pyR);
	}

	@Test
	public void calibrateOperations() throws Exception {
		initFightOnly();
		System.out.println("\n===== CALIBRATION OPERATIONS (ratio normalise sur LeekScript) =====");
		System.out.println("(x>1 = le langage consomme PLUS d'ops que LS pour le meme travail = budget plus SERRE ;");
		System.out.println(" x<1 = consomme MOINS = le joueur peut faire PLUS de calcul par tour)\n");

		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {

			// 1. Boucle arithmetique 100k iterations
			row("boucle arith 100k",
				lsOps("var s = 0; for (var i = 0; i < 100000; i++) { s += i; }"),
				jsOps(sb, "var s = 0; for (var i = 0; i < 100000; i++) { s += i; }"),
				pyOps(sb, "    s = 0\n    for i in range(100000):\n        s += i"));

			// 2. Boucle imbriquee 300x300 = 90k iterations internes
			row("boucle imbriquee 300x300",
				lsOps("var s = 0; for (var i = 0; i < 300; i++) { for (var j = 0; j < 300; j++) { s += i * j; } }"),
				jsOps(sb, "var s = 0; for (var i = 0; i < 300; i++) { for (var j = 0; j < 300; j++) { s += i * j; } }"),
				pyOps(sb, "    s = 0\n    for i in range(300):\n        for j in range(300):\n            s += i * j"));

			// 3. Appels de fonction 50k
			row("appels fonction 50k",
				lsOps("function f(x) { return x * 2; }\nvar s = 0; for (var i = 0; i < 50000; i++) { s += f(i); }"),
				jsOps(sb, "function f(x) { return x * 2; }\nvar s = 0; for (var i = 0; i < 50000; i++) { s += f(i); }"),
				pyOps(sb, "    def f(x):\n        return x * 2\n    s = 0\n    for i in range(50000):\n        s += f(i)"));

			// 4. Construction de tableau 50k (push)
			row("tableau push 50k",
				lsOps("var a = []; for (var i = 0; i < 50000; i++) { push(a, i); }"),
				jsOps(sb, "var a = []; for (var i = 0; i < 50000; i++) { a.push(i); }"),
				pyOps(sb, "    a = []\n    for i in range(50000):\n        a.append(i)"));

			// 5. Acces / somme de tableau 50k
			row("somme tableau 50k",
				lsOps("var a = []; for (var i = 0; i < 50000; i++) { push(a, i); } var s = 0; for (var i = 0; i < 50000; i++) { s += a[i]; }"),
				jsOps(sb, "var a = []; for (var i = 0; i < 50000; i++) { a.push(i); } var s = 0; for (var i = 0; i < 50000; i++) { s += a[i]; }"),
				pyOps(sb, "    a = []\n    for i in range(50000):\n        a.append(i)\n    s = 0\n    for i in range(50000):\n        s += a[i]"));

			// 6. Map/dict 20k insertions + lectures
			row("map 20k set+get",
				lsOps("var m = [:]; for (var i = 0; i < 20000; i++) { m[i] = i * 2; } var s = 0; for (var i = 0; i < 20000; i++) { s += m[i]; }"),
				jsOps(sb, "var m = new Map(); for (var i = 0; i < 20000; i++) { m.set(i, i * 2); } var s = 0; for (var i = 0; i < 20000; i++) { s += m.get(i); }"),
				pyOps(sb, "    m = {}\n    for i in range(20000):\n        m[i] = i * 2\n    s = 0\n    for i in range(20000):\n        s += m[i]"));

			// 7. String concat 2k (LS facture au prorata de la longueur -> O(n^2))
			row("string concat 2k",
				lsOps("var s = ''; for (var i = 0; i < 2000; i++) { s += 'x'; }"),
				jsOps(sb, "var s = ''; for (var i = 0; i < 2000; i++) { s += 'x'; }"),
				pyOps(sb, "    s = ''\n    for i in range(2000):\n        s += 'x'"));

			// 8. Recursion type minimax : fibonacci(28) (le vrai profil des IA de recherche)
			row("recursion fib(28)",
				lsOps("function fib(n) { if (n < 2) return n; return fib(n-1) + fib(n-2); }\nvar r = fib(28);"),
				jsOps(sb, "function fib(n) { if (n < 2) return n; return fib(n-1) + fib(n-2); }\nvar r = fib(28);"),
				pyOps(sb, "    def fib(n):\n        if n < 2:\n            return n\n        return fib(n-1) + fib(n-2)\n    r = fib(28)"));

			// 9. Arithmetique flottante 50k (sqrt/mult)
			row("math flottant 50k",
				lsOps("var s = 0.0; for (var i = 1; i < 50000; i++) { s += sqrt(i) * 1.5; }"),
				jsOps(sb, "var s = 0.0; for (var i = 1; i < 50000; i++) { s += Math.sqrt(i) * 1.5; }"),
				pyOps(sb, "    import math\n    s = 0.0\n    for i in range(1, 50000):\n        s += math.sqrt(i) * 1.5"));
		}
		System.out.println("\n===================================================================\n");
		System.out.println("Lecture : PY x0.20 = un algo qui coute 100k ops en LeekScript n'en coute que 20k en");
		System.out.println("Python -> le joueur Python peut faire ~5x plus de calcul par tour a budget egal.\n");
	}
}
