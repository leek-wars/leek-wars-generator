package test;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * ADVERSARIAL : un joueur peut-il faire BEAUCOUP de travail dans UN SEUL statement (compte ~1) pour
 * contourner le compteur d'ops ? On mesure getOperations() (via le vrai chemin, facteur inclus) pour
 * des one-liners "bulk" (comprehension, generateur, builtins natifs) vs la boucle explicite
 * equivalente. Si le one-liner compte O(1) alors que la boucle compte O(N), c'est une faille de
 * fairness (et un vecteur DoS borne seulement par le watchdog wall-clock 5s + cap RAM).
 */
public class TestOpsAdversarial extends FightTestBase {

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Adv1");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, defaultLeek(2, "Adv2"));
	}

	private long ops(PolyglotSandbox sb, String lang, String entry) {
		try {
			PolyglotEntityAI ai = new PolyglotEntityAI(lang, entry, sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			return ((Number) ai.runIA()).longValue();
		} catch (Throwable e) {
			return -1;
		}
	}
	private long ls(String body) {
		try {
			leekscript.compiler.AIFile file = new leekscript.compiler.AIFile("adv_ls_" + System.nanoTime(),
				body + "\nreturn getOperations();", System.currentTimeMillis(),
				leekscript.compiler.LeekScript.LATEST_VERSION, leek1.getId(), false);
			leek1.setAIFile(file);
			leek1.setLogs(new LeekLog(farmerLog, leek1));
			leek1.setFight(fight);
			com.leekwars.generator.fight.entity.EntityAI ai =
				com.leekwars.generator.fight.entity.EntityAI.build(generator, file, leek1);
			return ((Number) ai.runIA()).longValue();
		} catch (Throwable e) {
			return -1;
		}
	}
	private long py(PolyglotSandbox sb, String body) {
		return ops(sb, "python", "def turn():\n" + body + "\n    return getOperations()\n");
	}
	private long js(PolyglotSandbox sb, String body) {
		return ops(sb, "js", "function turn() {\n" + body + "\nreturn getOperations();\n}");
	}

	@Test
	public void adversarialSingleStatementWork() throws Exception {
		initFightOnly();
		System.out.println("\n===== ADVERSARIAL : travail cache dans 1 statement (N = 1 000 000) =====");
		System.out.println("ops elevees = bien compte (bon) ; ops ~qq milliers = TRAVAIL NON COMPTE (faille)\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			int N = 1_000_000;
			System.out.println("--- LEEKSCRIPT (reference : facture-t-il ses builtins ?) ---");
			System.out.printf("%-42s ops=%d%n", "boucle explicite for",
				ls("var s = 0; for (var i = 0; i < " + N + "; i++) { s += i * i; }"));
			System.out.printf("%-42s ops=%d%n", "builtin : sum(bigArray)",
				ls("var a = []; for (var i = 0; i < " + N + "; i++) { push(a, i); } var s = sum(a);"));
			System.out.printf("%-42s ops=%d%n", "builtin : arraySort(bigArray)",
				ls("var a = []; for (var i = 0; i < " + N + "; i++) { push(a, " + N + " - i); } arraySort(a);"));
			System.out.printf("%-42s ops=%d%n", "builtin : fill(bigArray, 0, N)",
				ls("var a = fill([], 0, " + N + "); var n = count(a);"));
			System.out.println("\n--- PYTHON ---");
			System.out.printf("%-42s ops=%d%n", "boucle explicite for (reference)",
				py(sb, "    s = 0\n    for i in range(" + N + "):\n        s += i * i"));
			System.out.printf("%-42s ops=%d%n", "generateur : sum(i*i for i in range N)",
				py(sb, "    s = sum(i * i for i in range(" + N + "))"));
			System.out.printf("%-42s ops=%d%n", "comprehension : [i*i for i in range N]",
				py(sb, "    a = [i * i for i in range(" + N + ")]"));
			System.out.printf("%-42s ops=%d%n", "builtin : sum(range N)",
				py(sb, "    s = sum(range(" + N + "))"));
			System.out.printf("%-42s ops=%d%n", "builtin : list(range N)",
				py(sb, "    a = list(range(" + N + "))"));
			System.out.printf("%-42s ops=%d%n", "builtin : sorted(range N, reverse)",
				py(sb, "    a = sorted(range(" + N + "), reverse=True)"));
			System.out.printf("%-42s ops=%d%n", "string : 'x' * 10_000_000",
				py(sb, "    s = 'x' * 10000000\n    n = len(s)"));
			System.out.printf("%-42s ops=%d%n", "bignum : 10 ** 5_000_000 (natif)",
				py(sb, "    x = 10 ** 5000000\n    n = 1"));
			System.out.printf("%-42s ops=%d (-1 = stoppe par le cap RAM)%n", "comprehension RETENUE 10M [i for i in range]",
				py(sb, "    a = [i for i in range(10000000)]\n    n = len(a)"));

			System.out.println("\n--- JS ---");
			System.out.printf("%-42s ops=%d%n", "boucle explicite for (reference)",
				js(sb, "var s = 0; for (var i = 0; i < " + N + "; i++) { s += i * i; }"));
			System.out.printf("%-42s ops=%d%n", "Array.from + reduce (callback/elem)",
				js(sb, "var a = Array.from({length: " + N + "}, (_, i) => i); var s = a.reduce((x, y) => x + y, 0);"));
			System.out.printf("%-42s ops=%d%n", "new Array(N).fill(0) (natif)",
				js(sb, "var a = new Array(" + N + ").fill(0); var n = a.length;"));
			System.out.printf("%-42s ops=%d%n", "'x'.repeat(10_000_000) (natif)",
				js(sb, "var s = 'x'.repeat(10000000); var n = s.length;"));
			System.out.printf("%-42s ops=%d%n", "Array sort (N) natif",
				js(sb, "var a = Array.from({length: " + N + "}, (_, i) => " + N + " - i); a.sort((x,y)=>x-y); var n = a.length;"));
		}
		System.out.println("\n===================================================================\n");
	}
}
