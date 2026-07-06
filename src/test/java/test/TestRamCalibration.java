package test;

import org.graalvm.polyglot.Context;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * CALIBRATION RAM inter-langages. Le cap est {@code min(50,RAM)*8_000_000}, IDENTIQUE pour les 3
 * langages MAIS en unites differentes : mRAM logique (LeekScript, ~1/valeur) vs OCTETS retenus
 * (polyglot, sandbox.MaxHeapMemory). On mesure, a cap identique (32 Mo/unites), le N MAX d'un
 * tableau d'entiers [0..N] retenu avant annulation -> combien de "donnees utiles" chaque langage
 * peut construire. Diagnostique, n'echoue pas.
 */
public class TestRamCalibration extends FightTestBase {

	private static final long CAP = 32_000_000L; // 32 Mo (octets) OU 32M unites mRAM

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Ram1");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, defaultLeek(2, "Ram2"));
	}

	// LeekScript : construit [0..N] retenu, renvoie getUsedRAM() ; -1 si TOO_MUCH_RAM.
	private long lsSurvives(int n) {
		try {
			AIFile file = new AIFile("ram_ls_" + System.nanoTime(),
				"global a = []; for (var i = 0; i < " + n + "; i++) { push(a, i); } return getUsedRAM();",
				System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek1.getId(), false);
			leek1.setAIFile(file);
			leek1.setLogs(new LeekLog(farmerLog, leek1));
			leek1.setFight(fight);
			EntityAI ai = EntityAI.build(generator, file, leek1);
			ai.setMaxRAM((int) CAP);
			ai.runIA();
			return 1;
		} catch (Throwable e) {
			return -1;
		}
	}

	private long guestSurvives(PolyglotSandbox sb, String lang, String src) {
		try (Context c = sb.createContext(lang, null, CAP)) {
			c.eval(lang, src);
			return 1;
		} catch (Throwable e) {
			return -1;
		}
	}

	// plus grand N (parmi une echelle) que le langage retient sans annulation
	private long maxN(java.util.function.LongUnaryOperator survives) {
		long best = 0;
		for (long n = 500_000; n <= 40_000_000L; n = (long) (n * 1.4)) {
			if (survives.applyAsLong(n) > 0) best = n; else break;
		}
		return best;
	}

	@Test
	public void calibrateRam() throws Exception {
		initFightOnly();
		System.out.println("\n===== CALIBRATION RAM : N max d'un tableau [0..N] retenu sous cap 32M =====\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long ls = maxN(n -> lsSurvives((int) n));
			long js = maxN(n -> guestSurvives(sb, "js",
				"globalThis.a = []; for (var i = 0; i < " + n + "; i++) { a.push(i); } a.length;"));
			long py = maxN(n -> guestSurvives(sb, "python",
				"a = []\nfor i in range(" + n + "):\n    a.append(i)\nlen(a)"));
			System.out.printf("N max retenu  ->  LS=%,d   JS=%,d   PY=%,d%n", ls, js, py);
			System.out.printf("ratio vs LS   ->  JS x%.2f   PY x%.2f%n",
				ls == 0 ? 0 : (double) js / ls, ls == 0 ? 0 : (double) py / ls);
			System.out.println("\n(x<1 = le langage sature son cap RAM PLUS TOT que LS = joueur plus contraint ;");
			System.out.println(" x>1 = peut retenir PLUS de donnees a budget RAM egal.)");
		}
		System.out.println("\n=======================================================================\n");
	}
}
