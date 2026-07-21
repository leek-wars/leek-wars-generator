package test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * DIAGNOSTIC #OOM worker : le RSS natif du worker prod monte de façon monotone (isolates GraalVM)
 * jusqu'à se faire tuer par le cgroup. On enchaîne ici N combats INDÉPENDANTS avec des IA JS qui
 * font du vrai travail, chaque combat créant puis fermant son propre {@code PolyglotSandbox}
 * (Engine isolate par langage), et on imprime le RSS toutes les 5 itérations.
 *
 * <p>Si le RSS croît sans se stabiliser alors que chaque sandbox est fermé, la fuite est
 * reproduite hors prod : {@code PolyglotSandbox.close()} / {@code Engine.close()} ne rend pas la
 * mémoire native de l'isolate. Purement diagnostique (imprime), n'assert rien de dur par défaut.
 */
public class TestPolyglotSandboxLeak {

	private static final String JS_AI = String.join("\n",
		"function turn() {",
		"  var s = 0;",
		"  for (var i = 0; i < 200000; i++) { s += (i * 7) % 13; }",
		"  var e = Fight.getNearestEnemy();",
		"  if (e != null) Fight.me.moveToward(e);",
		"  return s;",
		"}");

	private static long rssMB() {
		try {
			for (String line : Files.readAllLines(Paths.get("/proc/self/status"))) {
				if (line.startsWith("VmRSS:")) {
					String[] p = line.trim().split("\\s+");
					return Long.parseLong(p[1]) / 1024; // kB -> MB
				}
			}
		} catch (Exception ignore) {}
		return -1;
	}

	/** Même stats que FightTestBase.defaultLeek (cores > 0 pour un vrai budget d'ops). */
	private static Leek leek(int id, String name) {
		return new Leek(id, name, 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 8, 30, 0, false, 0, 0, "", 0, "", "", "", 0);
	}

	private void attachJs(Fight fight, FarmerLog farmerLog, Leek leek, String code) {
		AIFile file = new AIFile("leak_" + System.nanoTime() + ".js", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	/** Un combat complet, sandbox créé et fermé, avec deux IA JS actives. */
	private void oneFight(Generator generator) throws Exception {
		Fight fight = new Fight(generator);
		final HashMap<Integer, String> registerStore = new HashMap<>();
		fight.getState().setRegisterManager(new RegisterManager() {
			@Override public String getRegisters(int leek) { return registerStore.get(leek); }
			@Override public void saveRegisters(int leek, String registers, boolean is_new) { registerStore.put(leek, registers); }
		});
		fight.setStatisticsManager(new LocalTrophyManager());
		Leek a = leek(1, "LeakA");
		Leek b = leek(2, "LeakB");
		fight.getState().addEntity(0, a);
		fight.getState().addEntity(1, b);
		FarmerLog farmerLog = new FarmerLog(fight, 0);
		fight.getState().setRestatPotionsAvailable(0, 999);
		attachJs(fight, farmerLog, a, JS_AI);
		attachJs(fight, farmerLog, b, JS_AI);
		fight.startFight(true); // ferme le sandbox en fin de combat (finishFight -> closePolyglotSandbox)
		if (!(a.getAI() instanceof PolyglotEntityAI)) {
			throw new IllegalStateException("IA non polyglot : le pipeline JS ne s'est pas branché");
		}
	}

	@Test
	public void manyFightsDoNotLeakNativeMemory() throws Exception {
		int N = Integer.getInteger("leak.fights", 40);
		Generator generator = new Generator();
		long base = rssMB();
		System.out.printf("[leak] base RSS = %d MB, %d combats%n", base, N);
		long first5 = -1, last = base;
		for (int i = 1; i <= N; i++) {
			oneFight(generator);
			if (i % 5 == 0) {
				System.gc();
				Thread.sleep(200);
				long rss = rssMB();
				System.out.printf("[leak] après %3d combats : RSS = %5d MB  (delta base %+d MB, delta 5 derniers %+d MB)%n",
					i, rss, rss - base, rss - last);
				if (i == 5) first5 = rss;
				last = rss;
			}
		}
		long growthAfterWarmup = last - first5;
		long perFight = growthAfterWarmup / Math.max(1, N - 5);
		System.out.printf("[leak] croissance après warmup (5 -> %d combats) : %+d MB (%+d MB/combat)%n", N, growthAfterWarmup, perFight);

		// Avec l'Engine PAR COMBAT (fuite), chaque combat retenait ~215 Mo de natif JAMAIS rendu,
		// linéairement. Avec l'Engine STATIQUE partagé, le natif plafonne (l'isolate commit/rend sous
		// GC). Seuil à 50 Mo/combat : très au-dessus du bruit du plateau (~8 Mo/combat mesuré), très
		// en-dessous de la fuite (~215 Mo/combat) -> sépare franchement les deux régimes.
		assertTrue("Fuite mémoire native probable : +" + perFight + " Mo/combat après warmup (seuil 50). "
			+ "L'Engine isolate doit être partagé (statique), pas recréé par combat.", perFight < 50);
	}
}
