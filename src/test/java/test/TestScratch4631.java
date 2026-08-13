package test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Assert;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotFileSystem;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/** Repro issue #4631 : etat Python entre combats + accumulation isolate partage. */
public class TestScratch4631 extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private PolyglotEntityAI ai(PolyglotSandbox sb, Map<String, String> files, String entry) {
		Path passthrough = PolyglotSandbox.pythonStdlibRoot();
		PolyglotFileSystem fs = new PolyglotFileSystem(files.keySet(), files::get, passthrough, List.of(), entry);
		PolyglotEntityAI ai = new PolyglotEntityAI("python", files.get(entry), entry, fs, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	private static long rssMB() {
		try {
			for (String line : Files.readAllLines(Paths.get("/proc/self/status"))) {
				if (line.startsWith("VmRSS:")) {
					return Long.parseLong(line.trim().split("\\s+")[1]) / 1024;
				}
			}
		} catch (Exception ignore) {}
		return -1;
	}

	@Test
	public void moduleDictDoesNotPersistAcrossFights() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("dev_functions.py", "cache = {}\n");
		files.put("main.py",
			"import dev_functions\n"
			+ "def turn():\n"
			+ "    dev_functions.cache[len(dev_functions.cache)] = 'x'\n"
			+ "    return len(dev_functions.cache)\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			PolyglotEntityAI a1 = ai(sb, files, "main.py");
			long r1a = ((Number) a1.runIA()).longValue();
			long r1b = ((Number) a1.runIA()).longValue();
			a1.dispose();
			PolyglotEntityAI a2 = ai(sb, files, "main.py");
			long r2 = ((Number) a2.runIA()).longValue();
			a2.dispose();
			System.out.println("[4631] fight1 tour1=" + r1a + " tour2=" + r1b + " | fight2 tour1=" + r2);
			Assert.assertEquals(1, r1a);
			Assert.assertEquals(2, r1b); // persistance INTRA-combat voulue
			Assert.assertEquals("Etat de module Python persiste entre deux combats !", 1, r2);
		}
	}

	private void manyFights(boolean changingSource, int n) throws Exception {
		long base = -1;
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			for (int i = 0; i < n; i++) {
				String rev = changingSource ? String.valueOf(i) : "fixe";
				Map<String, String> files = new HashMap<>();
				files.put("dev_functions.py",
					"# version " + rev + "\n"
					+ "cache = {}\n"
					+ "for k in range(20000):\n"
					+ "    cache[k] = 'v' * 50\n");
				files.put("main.py",
					"# rev " + rev + "\n"
					+ "import dev_functions\n"
					+ "def turn():\n"
					+ "    return len(dev_functions.cache)\n");
				PolyglotEntityAI a = ai(sb, files, "main.py");
				try {
					long r = ((Number) a.runIA()).longValue();
					Assert.assertEquals(20000, r);
				} finally {
					a.dispose();
				}
				if (i == 4) { System.gc(); Thread.sleep(200); base = rssMB(); }
				if (i % 10 == 9) {
					System.gc();
					Thread.sleep(200);
					System.out.printf("[4631] iter %3d : RSS = %d MB%n", i + 1, rssMB());
				}
			}
		}
		System.gc();
		Thread.sleep(500);
		long finalRss = rssMB();
		System.out.printf("[4631] source=%s base(5)=%d MB final=%d MB delta=%+d MB%n",
			changingSource ? "changeante" : "constante", base, finalRss, finalRss - base);
	}

	@Test
	public void changingSourcesDoNotAccumulateInSharedIsolate() throws Exception {
		initFightOnly();
		manyFights(true, Integer.getInteger("leak.fights", 60));
	}

	@Test
	public void constantSourceStaysFlat() throws Exception {
		initFightOnly();
		manyFights(false, Integer.getInteger("leak.fights", 60));
	}

	@Test
	public void contextOomDoesNotPoisonNextFights() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			// 1) Une IA qui explose le cap RAM par contexte (le cas GSoMan : dict enorme)
			for (int i = 0; i < 3; i++) {
				Map<String, String> boom = new HashMap<>();
				boom.put("main.py",
					"# boom " + i + "\n"
					+ "cache = {}\n"
					+ "i = 0\n"
					+ "while True:\n"
					+ "    cache[i] = 'v' * 10000\n"
					+ "    i += 1\n"
					+ "def turn():\n"
					+ "    return 1\n");
				PolyglotEntityAI a = ai(sb, boom, "main.py");
				try {
					a.runIA();
					System.out.println("[4631] boom " + i + " : pas d'erreur ?");
				} catch (Exception e) {
					System.out.println("[4631] boom " + i + " : " + String.valueOf(e.getMessage()).substring(0, Math.min(120, String.valueOf(e.getMessage()).length())));
				} finally {
					a.dispose();
				}
			}
			// 2) Une IA triviale ensuite : doit marcher (l'isolate ne doit pas etre empoisonne)
			Map<String, String> ok = new HashMap<>();
			ok.put("main.py", "def turn():\n    return 42\n");
			PolyglotEntityAI a = ai(sb, ok, "main.py");
			try {
				long r = ((Number) a.runIA()).longValue();
				System.out.println("[4631] apres booms, IA triviale = " + r);
				Assert.assertEquals(42, r);
			} finally {
				a.dispose();
			}
		}
	}
}
