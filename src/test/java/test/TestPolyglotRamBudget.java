package test;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * RAM des IA JS/TS et Python : la meme que celle de LeekScript, telle que l'annonce System.maxRAM
 * (cf PolyglotEntityAI.BYTES_PER_RAM_UNIT), avec un plancher pour les entites sans RAM et un plafond
 * a la part d'isolate d'un contexte.
 */
public class TestPolyglotRamBudget extends FightTestBase {

	/** System.maxRAM par extension de fichier ("" = LeekScript). */
	private static final Map<String, String> MAX_RAM = Map.of(
		"", "return getMaxRAM()",
		".ts", "function turn(): number { return System.maxRAM; }",
		".js", "function turn() { return System.maxRAM; }",
		".py", "def turn():\n    return System.maxRAM\n");

	private Leek leek;

	@Override
	protected void createLeeks() {
		leek = defaultLeek(1, "Ram");
		fight.getState().addEntity(0, leek);
		fight.getState().addEntity(1, defaultLeek(2, "Dummy"));
	}

	@After
	public void closeContexts() {
		fight.closePolyglotSandbox();
	}

	private EntityAI ai(int ram, String extension, String code) {
		leek.setRAM(ram);
		AIFile file = new AIFile("ram_" + System.nanoTime() + extension, code, System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		attachAI(leek, file);
		return EntityAI.build(generator, file, leek);
	}

	private long maxRAM(int ram, String extension) throws Exception {
		return ((Number) ai(ram, extension, MAX_RAM.get(extension)).runIA()).longValue();
	}

	@Test
	public void maxRamIsTheSameInEveryLanguage() throws Exception {
		initFightOnly();
		long ls = maxRAM(6, "");
		Assert.assertEquals(6L * EntityAI.RAM_UNITS_PER_POINT * EntityAI.BYTES_PER_RAM_UNIT, ls);
		Assume.assumeTrue("isolate reduit par l'environnement : la part d'isolate borne avant la RAM 6",
			PolyglotSandbox.maxContextHeap("js") >= ls && PolyglotSandbox.maxContextHeap("python") >= ls);
		Assert.assertEquals(ls, maxRAM(6, ".ts"));
		Assert.assertEquals(ls, maxRAM(6, ".js"));
		Assert.assertEquals(ls, maxRAM(6, ".py"));
	}

	@Test
	public void maxRamShowsTheFloorAndTheIsolateShare() throws Exception {
		initFightOnly();
		long share = PolyglotSandbox.maxContextHeap("js");
		// Entite sans RAM (scenario local) : le plancher qui laisse monter le runtime.
		Assert.assertEquals(Math.min(64_000_000L, share), maxRAM(0, ".js"));
		// RAM 50 : 1,6 Go en LeekScript, borne a la part d'isolate d'un contexte.
		Assert.assertEquals(Math.min(EntityAI.MAX_RAM_POINTS * EntityAI.RAM_UNITS_PER_POINT * EntityAI.BYTES_PER_RAM_UNIT, share), maxRAM(50, ".js"));
	}

	/**
	 * System.usedRAM suit le tas que retient le guest : l'IA remplit un tableau au premier tour, le lache
	 * des qu'elle voit usedRAM monter, et usedRAM redescend. Elle calcule un peu a chaque tour : le repos
	 * entre deux mesures se compte en temps de calcul de l'IA.
	 */
	@Test
	public void usedRamFollowsTheGuestHeap() throws Exception {
		Assume.assumeTrue("image isolate sans mesure du tas guest (avant v25.1.3-combined-6)", imageMeasuresHeap());
		initFightOnly();
		leek.setCores(100);
		assertUsedRamFollows(".ts", 2_000_000, """
			let a: number[] = [];
			let empty = -1, released = false, s = 0;
			function turn(): number {
			  for (let i = 0; i < 3_000_000; i++) s += i;
			  const used = System.usedRAM;
			  if (empty < 0) { empty = used; for (let i = 0; i < 2_000_000; i++) a.push(i); }
			  else if (!released && used - empty > 8_000_000) { released = true; a = []; }
			  return used;
			}
			""");
		assertUsedRamFollows(".py", 1_000_000, """
			a = []
			empty, released, s = -1, False, 0
			def turn():
			    global a, empty, released, s
			    for i in range(300_000):
			        s += i
			    used = System.usedRAM
			    if empty < 0:
			        empty = used
			        for i in range(1_000_000):
			            a.append(i)
			    elif not released and used - empty > 4_000_000:
			        released = True
			        a = []
			    return used
			""");
	}

	/** Les deux langages viennent de la meme image : sonder le JS suffit. */
	private static boolean imageMeasuresHeap() {
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			return PolyglotSandbox.heapSizeBinding(sandbox.createContext("js")) != null;
		}
	}

	private void assertUsedRamFollows(String extension, int numbers, String code) throws Exception {
		EntityAI ai = ai(6, extension, code);
		long empty = turn(ai), peak = empty, last = empty;
		for (int t = 0; t < 300 && !(peak - empty > numbers * 4L && last < peak - numbers * 2L); t++) {
			last = turn(ai);
			peak = Math.max(peak, last);
		}
		String ram = extension + " : usedRAM " + empty + " -> " + peak + " -> " + last;
		Assert.assertTrue(ram + ", " + numbers + " entiers retenus", peak - empty > numbers * 4L);
		Assert.assertTrue(ram + ", tableau lache", last < peak - numbers * 2L);
		Assert.assertTrue(ram + ", sous maxRAM", peak < ai.getMaxRAM() * 8);
	}

	private static long turn(EntityAI ai) throws Exception {
		ai.resetCounter();
		return ((Number) ai.runIA()).longValue();
	}

	/** RAM 6 : LeekScript y tient 48 M entiers, une IA TypeScript doit en tenir au moins 10 M. */
	@Test
	public void lowRamTypeScriptHoldsTenMillionNumbers() throws Exception {
		initFightOnly();
		leek.setCores(100); // seule la RAM doit limiter
		EntityAI ai = ai(6, ".ts", "const a: number[] = [];\nfunction turn(): number {\n  for (let i = 0; i < 2_000_000; i++) a.push(i);\n  return a.length;\n}\n");
		long length = 0;
		for (int turn = 0; turn < 5; turn++) {
			ai.resetCounter();
			length = ((Number) ai.runIA()).longValue();
		}
		Assert.assertEquals(10_000_000L, length);
	}
}
