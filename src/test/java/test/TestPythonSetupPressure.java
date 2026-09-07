package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.common.Error;
import leekscript.runner.LeekRunException;

/**
 * Harnais de MESURE (#4999, #5000) : que retient vraiment un contexte Python monte (runtime GraalPy
 * + prelude objects.py), et le cap {@code POLYGLOT_PYTHON_MIN_HEAP_MB} le couvre-t-il ?
 *
 * <p>GraalVM ne verifie {@code sandbox.MaxHeapMemory} que sous PRESSION de l'isolate (tas de
 * l'isolate au-dela de {@code sandbox.RetainedBytesCheckFactor} = 70 % de {@code engine.MaxIsolateMemory}).
 * Un test ordinaire ne met jamais l'isolate sous pression, donc le setup passe avec n'importe quel
 * cap. Ici on FORCE la pression : on monte des contextes Python en les gardant vivants jusqu'a ce
 * que l'isolate soit plein, ce qui declenche le calcul de taille retenue sur chacun d'eux. Le
 * premier depassement journalise (cf PolyglotEntityAI.outOfMemory) la taille retenue mesuree.
 *
 * <p>Lancer avec un isolate REDUIT pour arriver vite a la pression, ex :
 * <pre>
 * POLYGLOT_MAX_ISOLATE_MB=1200 POLYGLOT_PYTHON_MIN_HEAP_MB=384 PRESSURE_CONTEXTS=40 \
 *   gradle --offline :test --tests test.TestPythonSetupPressure
 * </pre>
 * Le test est SAUTE sans {@code PRESSURE_CONTEXTS} (il est lent et depend de l'environnement). La
 * tache Gradle {@code :pythonLeakTest} (CI) le lance en JVM dediee avec un isolate de 1000 Mo et
 * 200 contextes : c'est le test de non-regression de la fuite de #4999. {@code :cleanTest} est
 * necessaire en manuel : l'environnement n'est pas une entree Gradle, le test serait "up-to-date".
 *
 * <p>Ce qui a ete etabli avec cet instrument (07/09/2026) : GraalVM (mode ISOLATED) ne mesure la
 * taille retenue d'un contexte qu'une fois que celui-ci a ALLOUE son cap (d'ou le churn) ; un
 * contexte Python monte retient moins de 32 Mo ; la panne prod n'etait pas le cap mais l'isolate
 * partage qui se remplissait de ~8 Mo par contexte ferme (`import uuid` de la garde de
 * determinisme), cf rawContextsSurviveIsolatePressure pour la bissection.
 */
public class TestPythonSetupPressure extends FightTestBase {

	private static final int CONTEXTS = Integer.parseInt(System.getenv().getOrDefault("PRESSURE_CONTEXTS", "0"));
	private static final int TURNS = Integer.parseInt(System.getenv().getOrDefault("PRESSURE_TURNS", "3"));
	/**
	 * Mo de TEMPORAIRES alloues par tour (jamais retenus). En mode ISOLATED, GraalVM ne calcule la
	 * taille retenue d'un contexte qu'une fois que celui-ci a ALLOUE au moins son cap
	 * (sandbox.AllocatedBytesCheckFactor = 1.0 x sandbox.MaxHeapMemory) : sans churn, un contexte
	 * trivial n'est JAMAIS mesure et passe avec n'importe quel cap. Le churn force la mesure.
	 */
	private static final int CHURN_MB = Integer.parseInt(System.getenv().getOrDefault("PRESSURE_CHURN_MB", "0"));

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Py1");
		leek2 = defaultLeek(2, "Py2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** Langage guest : python (defaut) ou js, pour comparer les deux runtimes a conditions egales. */
	private static final String LANG = System.getenv().getOrDefault("PRESSURE_LANG", "python");
	/** PRESSURE_HOST_GC=1 : System.gc() cote HOTE apres chaque fermeture de sandbox (discrimine une retention par handles hote). */
	private static final boolean HOST_GC = "1".equals(System.getenv("PRESSURE_HOST_GC"));

	private static String source(int context) {
		if ("js".equals(LANG)) {
			StringBuilder sb = new StringBuilder("// contexte " + context + "\n");
			int n = 0;
			while (sb.length() < SOURCE_KB * 1024) {
				sb.append("function f_").append(context).append('_').append(n).append("(a, b) { var c = a * ").append(n)
				  .append(" + b; if (c > ").append(context).append(") return [c, a, b, ").append(n)
				  .append("]; return {k: c, n: ").append(n).append("}; }\n");
				n++;
			}
			return sb + "var x = 1;\nfunction turn() {\n  for (var i = 0; i < " + CHURN_MB + "; i++) { var t = new Uint8Array(1 << 20); }\n  return x;\n}\n";
		}
		return "# contexte " + context + "\n" + ballast(context) + "x = 1\ndef turn():\n"
				+ "    for _ in range(" + CHURN_MB + "):\n        t = bytearray(1 << 20)\n"
				+ "    return x\n";
	}

	private PolyglotEntityAI buildAI(PolyglotSandbox sandbox, String source) {
		PolyglotEntityAI ai = new PolyglotEntityAI(LANG, source, sandbox);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	/**
	 * Contextes par sandbox : 0 = un seul sandbox, tous les contextes restent VIVANTS (pression par
	 * accumulation) ; N > 0 = un sandbox ferme tous les N contextes, comme des COMBATS successifs en
	 * prod (l'engine partage vieillit, les contextes fermes ne sont recuperes que sous pression).
	 */
	private static final int PER_SANDBOX = Integer.parseInt(System.getenv().getOrDefault("PRESSURE_PER_SANDBOX", "0"));

	/** Ko de code Python UNIQUE (fonctions numerotees) ajoutes a chaque source, pour peser sur le cache de l'engine. */
	private static final int SOURCE_KB = Integer.parseInt(System.getenv().getOrDefault("PRESSURE_SOURCE_KB", "0"));

	private static String ballast(int context) {
		StringBuilder sb = new StringBuilder();
		int n = 0;
		while (sb.length() < SOURCE_KB * 1024) {
			sb.append("def f_").append(context).append('_').append(n).append("(a, b):\n")
			  .append("    c = a * ").append(n).append(" + b\n")
			  .append("    if c > ").append(context).append(":\n        return [c, a, b, ").append(n).append("]\n")
			  .append("    return {'k': c, 'n': ").append(n).append("}\n");
			n++;
		}
		return sb.toString();
	}

	/**
	 * PRESSURE_RAW=1 : contextes BRUTS (createContext + eval + close), sans PolyglotEntityAI, donc
	 * sans bridge, sans prelude objects.py ni gardes de determinisme. Si la fuite disparait ici,
	 * elle vient de notre setup ; si elle persiste, elle est dans GraalPy/engine partage.
	 * PRESSURE_CLOSE_CLEAN=1 : close() au lieu du close(true) (cancel) de la prod.
	 */
	private static final boolean RAW = "1".equals(System.getenv("PRESSURE_RAW"));
	private static final boolean CLOSE_CLEAN = "1".equals(System.getenv("PRESSURE_CLOSE_CLEAN"));

	/**
	 * BISSECTION du setup Python (mode brut) : PRESSURE_STEPS = liste (separateur '|') des etapes de
	 * PolyglotEntityAI.ensureContext a rejouer sur le contexte brut, dans l'ordre de la prod :
	 * bridge (sac __lw), charge (__lw_charge), guard (determinisme), chargeguard, console, objects
	 * (prelude objects.py), plus import:&lt;module&gt; et exec:&lt;code&gt; (';' = retour a la ligne) pour
	 * descendre dans la stdlib. Les textes prives sont lus par reflexion : c'est un instrument de
	 * diagnostic, pas une API.
	 *
	 * <p>Verdict du 07/09/2026 (#4999), isolate 600-1200 Mo, 4 contextes par sandbox, churn 64 Mo :
	 * contextes bruts, bridge, prelude objects.py, garde de facturation, console : 0 fuite ;
	 * `import uuid` (via platform.system() au chargement) : ~8 Mo retenus par contexte, isolate
	 * plein au ~70e contexte (600 Mo) / ~120e (1200 Mo). JS : 0 fuite dans les memes conditions.
	 */
	private static final Set<String> STEPS = new HashSet<>(Arrays.asList(System.getenv().getOrDefault("PRESSURE_STEPS", "").split("\\|")));

	private static Object privateStatic(String field) throws Exception {
		java.lang.reflect.Field f = PolyglotEntityAI.class.getDeclaredField(field);
		f.setAccessible(true);
		return f.get(null);
	}

	private static void applySteps(org.graalvm.polyglot.Context c, PolyglotEntityAI ai, int i) throws Exception {
		if (STEPS.contains("bridge")) {
			com.leekwars.generator.polyglot.PolyglotAPIBridge.install(c, LANG, ai);
		}
		if (STEPS.contains("charge")) {
			java.lang.reflect.Method m = PolyglotEntityAI.class.getDeclaredMethod("chargeProxy");
			m.setAccessible(true);
			c.getBindings(LANG).putMember("__lw_charge", m.invoke(ai));
		}
		if (STEPS.contains("guard")) {
			java.lang.reflect.Method m = PolyglotEntityAI.class.getDeclaredMethod("pythonDeterminismGuard", long.class);
			m.setAccessible(true);
			c.eval(LANG, (String) m.invoke(null, (long) i));
		}
		// Sous-bissection de la garde de determinisme : imports seuls / patches sans le bloc
		// datetime / imports + bloc datetime seul.
		if (STEPS.contains("guard-imports") || STEPS.contains("guard-pre") || STEPS.contains("guard-dt")) {
			java.lang.reflect.Method m = PolyglotEntityAI.class.getDeclaredMethod("pythonDeterminismGuard", long.class);
			m.setAccessible(true);
			String guard = (String) m.invoke(null, (long) i);
			int dt = guard.indexOf("try:\n");
			String imports = "import sys\nimport os, random, uuid, time, datetime\n";
			if (STEPS.contains("guard-imports")) c.eval(LANG, imports);
			if (STEPS.contains("guard-pre")) c.eval(LANG, guard.substring(0, dt));
			if (STEPS.contains("guard-dt")) c.eval(LANG, imports + guard.substring(dt));
		}
		// import:<module> : un import de la stdlib isole (bissection des imports de la garde).
		for (String step : STEPS) {
			if (step.startsWith("import:")) {
				c.eval(LANG, "import " + step.substring("import:".length()) + "\n");
			}
			// exec:<code> : code Python arbitraire (';' = retour a la ligne).
			if (step.startsWith("exec:")) {
				c.eval(LANG, step.substring("exec:".length()).replace(';', '\n') + "\n");
			}
		}
		if (STEPS.contains("chargeguard")) {
			c.eval(LANG, (String) privateStatic("PY_CHARGE_GUARD"));
		}
		// Sous-bissection de la garde de facturation : sans le wrap des TYPES (list/dict... par
		// metaclasse), ou sans le wrap des FONCTIONS (sum/sorted/min/max).
		if (STEPS.contains("chargeguard-notype")) {
			c.eval(LANG, ((String) privateStatic("PY_CHARGE_GUARD")).replace("for _n in ('list','tuple','set','frozenset','dict'):", "for _n in ():"));
		}
		if (STEPS.contains("chargeguard-nofn")) {
			c.eval(LANG, ((String) privateStatic("PY_CHARGE_GUARD")).replace("for _n in ('sum','sorted','min','max'):", "for _n in ():"));
		}
		if (STEPS.contains("console")) {
			c.eval(LANG, (String) privateStatic("PY_CONSOLE_SETUP"));
		}
		if (STEPS.contains("objects")) {
			c.eval(LANG, (String) privateStatic("PY_OBJECT_API"));
		}
	}

	@Test
	public void rawContextsSurviveIsolatePressure() throws Exception {
		Assume.assumeTrue("PRESSURE_RAW non pose", RAW && CONTEXTS > 0);
		initFightOnly();
		int oom = 0, ok = 0;
		PolyglotSandbox sandbox = new PolyglotSandbox(LANG);
		List<org.graalvm.polyglot.Context> alive = new ArrayList<>();
		try {
			for (int i = 0; i < CONTEXTS; i++) {
				if (PER_SANDBOX > 0 && i > 0 && i % PER_SANDBOX == 0) {
					for (org.graalvm.polyglot.Context c : alive) {
						if (CLOSE_CLEAN) c.close(); else c.close(true);
					}
					alive.clear();
					sandbox.close();
					if (HOST_GC) {
						System.gc();
					}
					sandbox = new PolyglotSandbox(LANG);
				}
				org.graalvm.polyglot.Context c = sandbox.createContext(LANG, null, 95_000_000L);
				alive.add(c);
				boolean failed = false;
				try {
					applySteps(c, buildAI(sandbox, "x = 0\n"), i);
					c.eval(LANG, source(i));
					org.graalvm.polyglot.Value turn = c.getBindings(LANG).getMember("turn");
					for (int t = 0; t < TURNS; t++) {
						turn.execute();
					}
				} catch (org.graalvm.polyglot.PolyglotException e) {
					failed = true;
					System.out.println("[pressure] " + e.getMessage());
				}
				if (failed) oom++; else ok++;
				System.out.println("[pressure] contexte " + (i + 1) + "/" + CONTEXTS + " : " + (failed ? "OUT_OF_MEMORY" : "ok")
						+ " (ok=" + ok + ", oom=" + oom + ")");
			}
		} finally {
			for (org.graalvm.polyglot.Context c : alive) {
				try { c.close(true); } catch (Exception ignore) {}
			}
			sandbox.close();
		}
		System.out.println("[pressure] bilan brut : " + ok + " ok, " + oom + " echecs sur " + CONTEXTS);
		Assert.assertEquals(0, oom);
	}

	@Test
	public void trivialSetupSurvivesIsolatePressure() throws Exception {
		Assume.assumeTrue("PRESSURE_CONTEXTS non pose : harnais de calibration, saute", CONTEXTS > 0);
		com.leekwars.generator.Log.enable(true); // journal des depassements (message GraalVM avec la taille retenue)
		initFightOnly();
		List<PolyglotEntityAI> alive = new ArrayList<>();
		int oom = 0, ok = 0;
		PolyglotSandbox sandbox = new PolyglotSandbox(LANG);
		try {
			for (int i = 0; i < CONTEXTS; i++) {
				if (PER_SANDBOX > 0 && i > 0 && i % PER_SANDBOX == 0) {
					// Fin de "combat" : on rend les contextes et le sandbox, comme le worker.
					for (PolyglotEntityAI ai : alive) {
						ai.dispose();
					}
					alive.clear();
					sandbox.close();
					if (HOST_GC) {
						System.gc();
					}
					sandbox = new PolyglotSandbox(LANG);
				}
				// x = 1 : l'IA la plus simple possible, celle de la repro prod de #4999. Tout ce qui
				// est retenu vient du runtime et du prelude, jamais du joueur.
				// Source DISTINCTE par contexte (commentaire numerote + lest de fonctions uniques) :
				// l'engine partage met en cache chaque source vue, comme en prod ou chaque IA de
				// joueur est differente et pese des dizaines de Ko.
				PolyglotEntityAI ai = buildAI(sandbox, source(i));
				alive.add(ai);
				boolean failed = false;
				for (int turn = 0; turn < TURNS && !failed; turn++) {
					try {
						ai.runIA();
					} catch (LeekRunException e) {
						failed = true;
						if (e.getError() == Error.OUT_OF_MEMORY) {
							oom++;
						} else {
							throw e;
						}
					}
				}
				if (!failed) {
					ok++;
				}
				System.out.println("[pressure] contexte " + (i + 1) + "/" + CONTEXTS + " : " + (failed ? "OUT_OF_MEMORY" : "ok")
						+ " (ok=" + ok + ", oom=" + oom + ")");
			}
		} finally {
			for (PolyglotEntityAI ai : alive) {
				ai.dispose();
			}
			sandbox.close();
		}
		System.out.println("[pressure] bilan : " + ok + " contextes ok, " + oom + " annules au setup sur " + CONTEXTS);
		Assert.assertEquals("un setup Python trivial ne doit jamais depasser son cap, meme isolate plein", 0, oom);
	}
}
