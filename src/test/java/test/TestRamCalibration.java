package test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.polyglot.PolyglotEntityAI;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.runner.LeekRunException;

/**
 * CALIBRATION RAM inter-langages (cf PolyglotEntityAI.BYTES_PER_RAM_UNIT) : pour huit formes de
 * donnees, ce qu'un element coute en unites de RAM LeekScript (delta exact de getUsedRAM) et en
 * octets RETENUS par un contexte JS/TS ou Python, par le vrai chemin de combat (prelude, gardes).
 * Diagnostique : imprime le tableau, n'echoue pas.
 *
 * <p>Lancer seul, la mesure du retenu s'arme avant la creation des engines :
 * <pre>
 * gradle --offline :test --tests test.TestRamCalibration -Dleak.ramcalib=1
 * </pre>
 *
 * <p>Deux pieges de mesure. GraalVM ne calcule la taille retenue d'un contexte qu'une fois qu'il a
 * alloue une fraction de son cap, et ce calcul est asynchrone et lent sur un gros tas : chaque cas
 * finit donc par un long churn de temporaires, sinon le contexte est ferme avant qu'une mesure
 * aboutisse (une Map de 500 000 entrees sortait a 0 octet). Et le poireau a un budget d'operations
 * enorme : seule sa RAM doit borner LeekScript, sinon c'est sa limite d'operations qu'on mesure.
 */
public class TestRamCalibration extends FightTestBase {

	private static final boolean ENABLED = System.getProperty("leak.ramcalib") != null;
	private static final Path TRACE = Path.of(System.getProperty("java.io.tmpdir"), "lw-ramcalib-" + ProcessHandle.current().pid() + ".log");

	static {
		if (ENABLED) {
			// Lu a la creation des engines polyglot : le test doit tourner seul dans sa JVM.
			System.setProperty("polyglot.engine.AllowExperimentalOptions", "true");
			System.setProperty("polyglot.sandbox.TraceLimits", "true");
			System.setProperty("polyglot.sandbox.AllocatedBytesCheckFactor", "0.02");
			System.setProperty("polyglot.sandbox.AllocatedBytesCheckInterval", "1ms");
			System.setProperty("polyglot.log.file", TRACE.toString());
		}
	}

	private static final int CHURN = 60_000;
	private static final String JS_CHURN = "\nfor (let k = 0; k < " + CHURN + "; k++) { let t = new Float64Array(20000); t[0] = k; }\n";
	private static final String PY_CHURN = "\nfor _k in range(" + CHURN / 3 + "):\n    _t = bytearray(200000)\n";
	private static final Pattern HEAP = Pattern.compile("Maximum Heap Memory:\\s+([0-9.]+)(B|KB|MB|GB)");

	private Leek leek;
	private int traceLines;

	@BeforeClass
	public static void enabled() {
		Assume.assumeTrue("diagnostic : lancer seul avec -Dleak.ramcalib=1", ENABLED);
	}

	@Override
	protected void createLeeks() {
		// RAM 50 (cap le plus haut) et 1000 coeurs : seule la forme des donnees doit compter.
		leek = defaultLeek(1, "Calib");
		leek.setCores(1000);
		leek.setRAM(50);
		fight.getState().addEntity(0, leek);
		fight.getState().addEntity(1, defaultLeek(2, "Dummy"));
	}

	private record Shape(String name, int n, String ls, String ts, String py) {}

	private static final List<Shape> SHAPES = List.of(
		new Shape("entiers", 4_000_000,
			"global a = []\nfor (var i = 0; i < N; i++) { push(a, i) }",
			"const a: number[] = [];\nfor (let i = 0; i < N; i++) a.push(i);",
			"a = []\nfor i in range(N):\n    a.append(i)"),
		new Shape("grille [][100]", 4_000_000,
			"global a = []\nfor (var r = 0; r < N / 100; r++) { var row = []\nfor (var c = 0; c < 100; c++) { push(row, r + c) }\npush(a, row) }",
			"const a: number[][] = [];\nfor (let r = 0; r < N / 100; r++) { const row: number[] = []; for (let c = 0; c < 100; c++) row.push(r + c); a.push(row); }",
			"a = []\nfor r in range(N // 100):\n    a.append([r + c for c in range(100)])"),
		new Shape("reels", 2_000_000,
			"global a = []\nfor (var i = 0; i < N; i++) { push(a, i + 0.5) }",
			"const a: number[] = [];\nfor (let i = 0; i < N; i++) a.push(i + 0.5);",
			"a = []\nfor i in range(N):\n    a.append(i + 0.5)"),
		new Shape("instances (3 champs)", 500_000,
			"class P { x; y; z; constructor(v) { this.x = v; this.y = v + 1; this.z = v + 2 } }\nglobal a = []\nfor (var i = 0; i < N; i++) { push(a, new P(i)) }",
			"class P { x: number; y: number; z: number; constructor(v: number) { this.x = v; this.y = v + 1; this.z = v + 2; } }\nconst a: P[] = [];\nfor (let i = 0; i < N; i++) a.push(new P(i));",
			"class P:\n    def __init__(self, v):\n        self.x = v\n        self.y = v + 1\n        self.z = v + 2\na = []\nfor i in range(N):\n    a.append(P(i))"),
		new Shape("objets / dicts {x,y}", 500_000,
			"global a = []\nfor (var i = 0; i < N; i++) { push(a, {x: i, y: i + 1}) }",
			"const a: {x: number, y: number}[] = [];\nfor (let i = 0; i < N; i++) a.push({x: i, y: i + 1});",
			"a = []\nfor i in range(N):\n    a.append({'x': i, 'y': i + 1})"),
		new Shape("map int -> int", 500_000,
			"global m = [:]\nfor (var i = 0; i < N; i++) { m[i] = i }",
			"const m = new Map<number, number>();\nfor (let i = 0; i < N; i++) m.set(i, i);",
			"m = {}\nfor i in range(N):\n    m[i] = i"),
		new Shape("chaines courtes", 500_000,
			"global a = []\nfor (var i = 0; i < N; i++) { push(a, 'cell' + i) }",
			"const a: string[] = [];\nfor (let i = 0; i < N; i++) a.push('cell' + i);",
			"a = []\nfor i in range(N):\n    a.append('cell' + str(i))"),
		new Shape("ensemble d'entiers", 500_000,
			"global s = <>\nfor (var i = 0; i < N; i++) { setPut(s, i) }",
			"const s = new Set<number>();\nfor (let i = 0; i < N; i++) s.add(i);",
			"s = set()\nfor i in range(N):\n    s.add(i)"));

	private EntityAI build(String path, String code) {
		AIFile file = new AIFile(path, code, System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		attachAI(leek, file);
		EntityAI ai = EntityAI.build(generator, file, leek);
		if (ai instanceof PolyglotEntityAI polyglot) {
			polyglot.setTurnWallClockLimitMs(300_000);
		}
		return ai;
	}

	/** Unites de RAM LeekScript consommees par la construction. */
	private double lsUnits(String code) throws Exception {
		EntityAI ai = build("calib_" + System.nanoTime(), "var before = getUsedRAM()\n" + code + "\nreturn getUsedRAM() - before");
		return ((Number) ai.runIA()).doubleValue() / 8;
	}

	/** Octets retenus au plus haut par le contexte de cette IA, d'apres le journal de sandbox.TraceLimits (ecrit a sa fermeture). */
	private long retained(String extension, String code) throws Exception {
		EntityAI ai = build("calib_" + System.nanoTime() + extension, code + (".py".equals(extension) ? PY_CHURN : JS_CHURN));
		try {
			ai.runIA();
		} catch (LeekRunException e) {
			System.out.println("    !! " + e.getError() + " sur " + code.lines().findFirst().orElse(""));
		}
		((PolyglotEntityAI) ai).dispose();
		Thread.sleep(300);
		List<String> lines = Files.exists(TRACE) ? Files.readAllLines(TRACE) : List.of();
		long max = 0;
		for (String line : lines.subList(Math.min(traceLines, lines.size()), lines.size())) {
			Matcher m = HEAP.matcher(line);
			if (m.find()) {
				long unit = switch (m.group(2)) { case "KB" -> 1L << 10; case "MB" -> 1L << 20; case "GB" -> 1L << 30; default -> 1L; };
				max = Math.max(max, (long) (Double.parseDouble(m.group(1)) * unit));
			}
		}
		traceLines = lines.size();
		return max;
	}

	@Test
	public void calibrateRam() throws Exception {
		initFightOnly();
		long tsBase = retained(".ts", "let z = 1;");
		long pyBase = retained(".py", "z = 1");
		long pyStdlib = retained(".py", "import math, random, heapq, collections, itertools, functools, bisect, typing, dataclasses, json, re, statistics, copy, enum");
		Assume.assumeTrue("aucune mesure du retenu : les engines existaient avant ce test", tsBase > 0 && pyBase > 0);
		System.out.printf("%n===== CALIBRATION RAM : unites LeekScript et octets retenus par element =====%n");
		System.out.printf("Contexte vide : JS/TS %.0f Mo, Python %.0f Mo (%.0f Mo avec 14 modules de la stdlib)%n%n",
			tsBase / 1048576.0, pyBase / 1048576.0, pyStdlib / 1048576.0);
		System.out.printf("%-22s %10s | %8s | %9s %9s | %9s %9s%n", "forme", "N", "LS u/elt", "TS o/elt", "TS o/u", "PY o/elt", "PY o/u");
		for (Shape s : SHAPES) {
			String n = String.valueOf(s.n());
			double ls = lsUnits(s.ls().replace("N", n)) / s.n();
			double ts = (retained(".ts", s.ts().replace("N", n)) - tsBase) / (double) s.n();
			double py = (retained(".py", s.py().replace("N", n)) - pyBase) / (double) s.n();
			System.out.printf("%-22s %10d | %8.2f | %9.1f %9.2f | %9.1f %9.2f%n", s.name(), s.n(), ls, ts, ts / ls, py, py / ls);
		}
		System.out.println("\n(o/u = octets retenus par unite LeekScript : la parite sur cette forme demanderait ce facteur.)");
		System.out.println("Un calcul du retenu qui tombe au mauvais moment fausse une ligne (valeur basse, ou negative si c'est");
		System.out.println("le contexte vide) : relancer, garder le max.\n");
		Files.deleteIfExists(TRACE);
	}
}
