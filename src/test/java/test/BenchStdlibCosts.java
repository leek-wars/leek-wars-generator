package test;

import static org.junit.Assume.assumeTrue;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.runner.LeekFunctions;

/**
 * BANC : cout REEL de chaque fonction de la bibliotheque standard LeekScript, en OPERATIONS.
 *
 * <p>Les couts du registre ({@link LeekFunctions}, ex. cos = 30, pow = 140, hypot = 187) sont
 * historiques et ne reflètent plus le temps d'execution : sur la JVM actuelle un cos natif vaut
 * quelques additions. Ils sont pourtant factures aux IA LeekScript a chaque appel, et depuis
 * l'alignement inter-langages (2026-09) aux IA JS/Python aussi. Ce banc mesure, sur le VRAI
 * moteur (code LeekScript compile et execute par EntityAI), le surcout d'un appel par rapport a
 * la meme boucle sans l'appel, et l'exprime en unites « operation » : l'unite est le temps moyen
 * d'une operation de la boucle arithmetique de reference ({@code s += i}, 4 ops par iteration
 * d'apres le compteur), celle qui sert deja de reference a TestOpsCalibration.
 *
 * <p>Purement diagnostique, long (~1 min), skippe sans {@code STDLIB_BENCH=1}. Imprime un tableau
 * « fonction | registre | mesure ns/appel | mesure ops » et ecrit build/stdlib-costs.json. Les
 * valeurs retenues dans le registre sont arrondies a la main a partir de ce tableau (min 1).
 *
 * <pre>STDLIB_BENCH=1 gradle --offline :test --tests "test.BenchStdlibCosts"</pre>
 */
public class BenchStdlibCosts extends FightTestBase {

	private static final int N = 1_000_000;
	private static final int ROUNDS = 5;
	private static final int REPEATS = 5;

	private Leek leek1;

	@Override
	protected void createLeeks() {
		// 2000 coeurs : budget d'ops ~2 G (applyEntityBudgets le remet a cores x 1M a chaque tour),
		// sinon 1M appels de pow (140 ops au registre) depassent les 8M d'un poireau de test.
		leek1 = new Leek(1, "Bench1", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 2000, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, defaultLeek(2, "Bench2"));
	}

	/** IA compilee pour un corps LeekScript : temps minimal (ns) d'une execution apres echauffement, et ops comptees. */
	private static final class Run {
		final EntityAI ai;
		long ops;
		Run(EntityAI ai) { this.ai = ai; }
		long nanos() throws Exception {
			long best = Long.MAX_VALUE;
			for (int k = 0; k < ROUNDS; k++) {
				ai.resetCounter(); // le compteur d'ops est cumulatif hors combat : 28 runs de pow depassaient le budget
				long t = System.nanoTime();
				ai.runIA();
				best = Math.min(best, System.nanoTime() - t);
			}
			return best;
		}
	}

	private Run compile(String body) throws Exception {
		AIFile file = new AIFile("bench_ls_" + System.nanoTime(), body + "\nreturn s;",
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek1.getId(), false);
		leek1.setAIFile(file);
		leek1.setLogs(new LeekLog(farmerLog, leek1));
		leek1.setFight(fight);
		EntityAI ai = EntityAI.build(generator, file, leek1);
		Run run = new Run(ai);
		try {
			ai.runIA();
		} catch (leekscript.runner.LeekRunException e) {
			throw new RuntimeException("1er run : " + e.getError() + " ops=" + ai.getOperations() + " max=" + ai.maxOperations, e);
		}
		run.ops = ai.getOperations(); // ops d'UNE execution (compteur remis a zero par runIA)
		for (int k = 0; k < 3; k++) { ai.resetCounter(); ai.runIA(); } // echauffement (compilation JIT du corps)
		return run;
	}

	private static double median(double[] v) {
		double[] c = v.clone();
		java.util.Arrays.sort(c);
		return c.length % 2 == 1 ? c[c.length / 2] : (c[c.length / 2 - 1] + c[c.length / 2]) / 2;
	}

	private static String loop(String accumulate) {
		return "var s = 0;\nfor (var i = 0; i < " + N + "; i++) {\n\ts += " + accumulate + ";\n}";
	}

	/**
	 * [nom registre, expression AVEC l'appel, expression de REFERENCE]. La reference est un appel
	 * TRIVIAL de meme arite et memes arguments (abs pour 1 argument, min pour 2) : l'operateur `+`
	 * dynamique de LeekScript coute plus qu'un appel a deux arguments, une reference sans appel
	 * donnait des couts negatifs pour pow, hypot et atan2. Le delta isole donc le travail PROPRE
	 * de la fonction ; abs et min valent 0 par construction et prennent le plancher (1).
	 */
	private static final String[][] CASES = {
		// --- Number ---
		{"abs", "abs(i - 500000)", "(i - 500000)"},
		{"min", "min(i, 500000)", "abs(i + 500000)"},
		{"max", "max(i, 500000)", "abs(i + 500000)"},
		{"ceil", "ceil(i * 0.5)", "abs(i * 0.5)"},
		{"floor", "floor(i * 0.5)", "abs(i * 0.5)"},
		{"round", "round(i * 0.5)", "abs(i * 0.5)"},
		{"signum", "signum(i - 500000)", "abs(i - 500000)"},
		{"cos", "cos(i * 0.001)", "abs(i * 0.001)"},
		{"sin", "sin(i * 0.001)", "abs(i * 0.001)"},
		{"tan", "tan(i * 0.001)", "abs(i * 0.001)"},
		{"acos", "acos((i % 1000) * 0.001)", "abs((i % 1000) * 0.001)"},
		{"asin", "asin((i % 1000) * 0.001)", "abs((i % 1000) * 0.001)"},
		{"atan", "atan(i * 0.001)", "abs(i * 0.001)"},
		{"atan2", "atan2(i * 0.001, 1.5)", "min(i * 0.001, 1.5)"},
		{"sqrt", "sqrt(i)", "abs(i)"},
		{"cbrt", "cbrt(i)", "abs(i)"},
		{"exp", "exp((i % 100) * 0.1)", "abs((i % 100) * 0.1)"},
		{"log", "log(i + 1)", "abs(i + 1)"},
		{"log2", "log2(i + 1)", "abs(i + 1)"},
		{"log10", "log10(i + 1)", "abs(i + 1)"},
		{"pow", "pow(i * 0.001, 1.5)", "min(i * 0.001, 1.5)"},
		{"pow_b", "pow(1.0001, i % 100)", "min(1.0001, i % 100)"},
		{"hypot", "hypot(i * 0.001, 2.5)", "min(i * 0.001, 2.5)"},
		{"toRadians", "toRadians(i)", "abs(i)"},
		{"toDegrees", "toDegrees(i)", "abs(i)"},
		{"rand", "rand()", "abs(0.5)"},
		{"randInt", "randInt(0, 100)", "min(0, 100)"},
		{"randReal", "randReal(0.0, 1.0)", "min(0.0, 1.0)"},
		{"isNaN", "(isNaN(i) ? 1 : 0)", "(abs(i) < 0 ? 1 : 0)"},
		{"isFinite", "(isFinite(i) ? 1 : 0)", "(abs(i) < 0 ? 1 : 0)"},
		{"isInfinite", "(isInfinite(i) ? 1 : 0)", "(abs(i) < 0 ? 1 : 0)"},
		{"binString", "(binString(i) == \"1\" ? 1 : 0)", "(abs(i) == 1 ? 1 : 0)"},
		{"hexString", "(hexString(i) == \"1\" ? 1 : 0)", "(abs(i) == 1 ? 1 : 0)"},
		{"bitCount", "bitCount(i)", "abs(i)"},
		{"bitLength", "bitLength(i)", "abs(i)"},
		{"testBit", "(testBit(i, 3) ? 1 : 0)", "(min(i, 3) < 0 ? 1 : 0)"},
		{"setBit", "setBit(i, 3)", "min(i, 3)"},
		{"leadingZeros", "leadingZeros(i)", "abs(i)"},
		{"trailingZeros", "trailingZeros(i)", "abs(i)"},
		{"bitReverse", "bitReverse(i)", "abs(i)"},
		{"byteReverse", "byteReverse(i)", "abs(i)"},
		{"rotateLeft", "rotateLeft(i, 3)", "min(i, 3)"},
		{"rotateRight", "rotateRight(i, 3)", "min(i, 3)"},
		{"realBits", "realBits(i * 0.5)", "abs(i * 0.5)"},
		{"bitsToReal", "bitsToReal(i)", "abs(i)"},
		{"isPermutation", "(isPermutation(i, i + 1) ? 1 : 0)", "(min(i, i + 1) < 0 ? 1 : 0)"},
		// --- String / Value (reference : meme expression sans l'appel, le cout des chaines domine) ---
		{"length", "length(\"hello world\")", "abs(11)"},
		{"charAt", "(charAt(\"hello\", i % 5) == \"h\" ? 1 : 0)", "(min(i % 5, 4) == 0 ? 1 : 0)"},
		{"codePointAt", "codePointAt(\"hello\", i % 5)", "min(i % 5, 4)"},
		{"trim", "(trim(\" hello \") == \"h\" ? 1 : 0)", "(abs(i) == 1 ? 1 : 0)"},
		{"string", "(string(i) == \"1\" ? 1 : 0)", "(abs(i) == 1 ? 1 : 0)"},
		{"number", "number(\"123\")", "abs(123)"},
		{"typeOf", "typeOf(i)", "abs(i)"},
	};

	@Test
	public void benchStdlibCosts() throws Exception {
		assumeTrue("STDLIB_BENCH=1 pour lancer le banc", "1".equals(System.getenv("STDLIB_BENCH")));
		initFightOnly();

		// Unite : ns par operation, moyenne de 3 boucles de reference (entier, reel, comparaison) dont
		// les ops sont LUES au compteur LeekScript, pas supposees.
		String[] refs = { "i", "(i * 0.001)", "(i % 7 == 3 ? 1 : 0)" };
		double nsPerOp = 0;
		for (String r : refs) {
			Run run = compile(loop(r));
			double perOp = run.nanos() / (double) run.ops;
			System.out.printf("reference %-24s ops=%d  %.2f ns/op%n", r, run.ops, perOp);
			nsPerOp += perOp / refs.length;
		}
		System.out.printf("%n===== BANC STDLIB : unite %.2f ns/op, %d repetitions entrelacees, mediane =====%n", nsPerOp, REPEATS);
		System.out.printf("%-16s %9s %14s %10s%n", "fonction", "registre", "ns/appel", "ops");

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("nsPerOp", nsPerOp);
		Map<String, Object> costs = new LinkedHashMap<>();
		List<String> failed = new ArrayList<>();
		String only = System.getenv("STDLIB_BENCH_ONLY"); // ex. "pow,hypot" pour ne relancer que celles-la
		for (String[] c : CASES) {
			if (only != null && !("," + only + ",").contains("," + c[0] + ",")) continue;
			try {
				Run with = compile(loop(c[1]));
				Run without = compile(loop(c[2]));
				if (only != null) System.out.printf("[ops] %s : avec=%d sans=%d%n", c[0], with.ops, without.ops);
				// Entrelacement AVEC/SANS : une derive de la machine touche les deux mesures pareil.
				double[] samples = new double[REPEATS];
				for (int r = 0; r < REPEATS; r++) {
					samples[r] = (with.nanos() - without.nanos()) / (double) N;
				}
				double nsPerCall = Math.max(0.0, median(samples));
				double ops = nsPerCall / nsPerOp;
				LeekFunctions fn = LeekFunctions.getValue(c[0].replaceAll("_[a-z]$", ""), false);
				int registry = fn == null ? -1 : fn.getOperations();
				System.out.printf("%-16s %9d %14.1f %10.2f%n", c[0], registry, nsPerCall, ops);
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("registry", registry);
				row.put("nsPerCall", nsPerCall);
				row.put("ops", ops);
				costs.put(c[0], row);
			} catch (Exception e) {
				String detail = e instanceof leekscript.runner.LeekRunException
					? ((leekscript.runner.LeekRunException) e).getError() + " " + java.util.Arrays.toString(((leekscript.runner.LeekRunException) e).getParameters())
					: e.toString();
				failed.add(c[0] + " : " + detail);
			}
		}
		out.put("functions", costs);
		for (String f : failed) System.out.println("ECHEC " + f);
		try (FileWriter w = new FileWriter("build/stdlib-costs.json")) {
			w.write(toJson(out));
		}
		System.out.println("-> build/stdlib-costs.json");
	}

	@SuppressWarnings("unchecked")
	private static String toJson(Object o) {
		if (o instanceof Map) {
			StringBuilder sb = new StringBuilder("{");
			boolean first = true;
			for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
				if (!first) sb.append(",");
				first = false;
				sb.append('"').append(e.getKey()).append("\":").append(toJson(e.getValue()));
			}
			return sb.append("}").toString();
		}
		return String.valueOf(o);
	}
}
