package test;

import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.FightFunctions;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.IACompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

/**
 * Benchmark des 3 phases d'analyse (lex / syntax / semantic) sur l'IA Quantum.
 *
 * Usage: gradle test --tests "test.TestBenchAnalyze"
 *   ou:  java -classpath ... test.TestBenchAnalyze [iterations] [warmup]
 *
 * Variable d'env LEEK_AI_ROOT optionnelle pour override le chemin.
 */
public class TestBenchAnalyze {

	private static final String DEFAULT_ROOT = "/home/pierre/dev/leek-wars/server/filesystem/1/ia";
	private static final String ENTRYPOINT = "Quantum";
	// BENCH_RESET_INCLUDES=0 mime la prod réelle (édition de l'entrypoint sans
	// toucher aux 124 inclus) → seul Quantum.leek se fait re-lex à chaque itération.
	// Par défaut on reset tout (cold path = worst case).
	private static boolean RESET_INCLUDES = !"0".equals(System.getenv().getOrDefault("BENCH_RESET_INCLUDES", "1"));

	public static void main(String[] args) throws Exception {
		int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 5;
		int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 30;
		String root = System.getenv().getOrDefault("LEEK_AI_ROOT", DEFAULT_ROOT);
		run(root, ENTRYPOINT, warmup, iterations);
	}

	@Test
	public void benchmark() throws Exception {
		String root = System.getenv().getOrDefault("LEEK_AI_ROOT", DEFAULT_ROOT);
		// Benchmark dev-only : il mesure l'analyse sur un dossier d'IA local (LEEK_AI_ROOT)
		// qui n'existe pas en CI. On le skip proprement (test ignoré, pas en échec) quand le
		// root est absent ; lancer avec LEEK_AI_ROOT pointant sur un dossier d'IA pour mesurer.
		org.junit.Assume.assumeTrue("LEEK_AI_ROOT absent (" + root + "), benchmark ignoré hors dev",
			new java.io.File(root).isDirectory());
		int iterations = Integer.parseInt(System.getenv().getOrDefault("BENCH_ITERATIONS", "30"));
		int warmup = Integer.parseInt(System.getenv().getOrDefault("BENCH_WARMUP", "5"));
		run(root, ENTRYPOINT, warmup, iterations);
	}

	private static void registerGameNatives() {
		// Wire les natives spécifiques au jeu (getMaxOperations, getTurn, BOSS_*, etc.)
		// pour que la phase semantic ne génère pas 1768 fausses erreurs UNDEFINED_FUNCTION.
		LeekFunctions.setExtraFunctions(FightFunctions.getFunctions(), "com.leekwars.generator.classes.*");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
		// Touch les enums pour forcer le static init
		FightFunctions.getFunctions();
		FightConstants.values();
	}

	private static void run(String rootPath, String entrypoint, int warmup, int iterations) throws Exception {
		System.out.println("=== BenchAnalyze " + entrypoint + " ===");
		System.out.println("root       = " + rootPath);
		System.out.println("warmup     = " + warmup);
		System.out.println("iterations = " + iterations);

		registerGameNatives();

		// Configure NativeFileSystem rooted on the AI dir.
		final Folder absRoot = new Folder(0, 0, rootPath, null, null, null, System.currentTimeMillis());
		absRoot.setParent(absRoot);
		absRoot.setRoot(absRoot);
		var nfs = new NativeFileSystem() {
			@Override public Folder getRoot() { return absRoot; }
			@Override public Folder getRoot(int owner) { return absRoot; }
			@Override public Folder getRoot(int owner, int farmer) { return absRoot; }
		};
		// Le Folder a besoin d'un FileSystem pour findFolder/findFile : on le rebind.
		java.lang.reflect.Field fsField = Folder.class.getDeclaredField("fs");
		fsField.setAccessible(true);
		fsField.set(absRoot, nfs);

		LeekScript.setFileSystem(nfs);
		try {
			AIFile ai = absRoot.resolve(entrypoint);
			System.out.println("entrypoint = " + ai.getPath());
			System.out.println("size       = " + ai.getCharCount() + " chars");

			// Premier analyze pour warmup et collecter la liste des inclus.
			System.out.println("\n--- warmup (n=" + warmup + ") ---");
			IACompiler.PHASE_TIMINGS_ENABLED = false;
			for (int i = 0; i < warmup; i++) {
				resetTokens(ai);
				new IACompiler().analyze(ai);
			}

			// Mesure
			System.out.println("\n--- measure (n=" + iterations + ") ---");
			long[] totalNs = new long[iterations];
			long[] lexNs = new long[iterations];
			long[] parseNs = new long[iterations];   // = lex + syntax
			long[] analyzeNs = new long[iterations];

			IACompiler.PHASE_TIMINGS_ENABLED = true;
			for (int i = 0; i < iterations; i++) {
				resetTokens(ai);
				IACompiler.resetPhaseTimings();
				long t0 = System.nanoTime();
				var result = new IACompiler().analyze(ai);
				totalNs[i] = System.nanoTime() - t0;
				lexNs[i] = IACompiler.LEX_NANOS.get();
				parseNs[i] = IACompiler.PARSE_NANOS.get();
				analyzeNs[i] = IACompiler.ANALYZE_NANOS.get();
				if (i == 0) {
					System.out.println("(success=" + result.success + ", problems=" + result.informations.size() + ", included=" + (result.includedAIs == null ? -1 : result.includedAIs.size()) + ")");
				}
			}
			IACompiler.PHASE_TIMINGS_ENABLED = false;

			report("total       ", totalNs);
			report("lex         ", lexNs);
			long[] syntaxNs = new long[iterations];
			for (int i = 0; i < iterations; i++) syntaxNs[i] = parseNs[i] - lexNs[i];
			report("syntax      ", syntaxNs);
			report("semantic    ", analyzeNs);
			long[] otherNs = new long[iterations];
			for (int i = 0; i < iterations; i++) otherNs[i] = totalNs[i] - parseNs[i] - analyzeNs[i];
			report("other (init)", otherNs);
		} finally {
			LeekScript.resetFileSystem();
		}
	}

	/**
	 * Reset les tokens parsés sur l'AI principal et tous les inclus (via setCode
	 * qui clear le tokenStream).
	 */
	private static void resetTokens(AIFile mainAi) {
		mainAi.setCode(mainAi.getCode());
		if (RESET_INCLUDES) {
			var included = mainAi.getIncludedAIs();
			if (included != null) {
				for (var inc : included) inc.setCode(inc.getCode());
			}
		}
		mainAi.clearErrors();
	}

	private static void report(String label, long[] samples) {
		long min = Long.MAX_VALUE, max = 0, sum = 0;
		for (long s : samples) {
			if (s < min) min = s;
			if (s > max) max = s;
			sum += s;
		}
		double mean = sum / (double) samples.length;
		long[] sorted = samples.clone();
		java.util.Arrays.sort(sorted);
		long median = sorted[sorted.length / 2];
		long p10 = sorted[Math.max(0, sorted.length / 10)];

		System.out.printf("%s : mean=%6.2f ms  median=%6.2f ms  min=%6.2f ms  max=%6.2f ms  p10=%6.2f ms%n",
				label, mean / 1e6, median / 1e6, min / 1e6, max / 1e6, p10 / 1e6);
	}
}
