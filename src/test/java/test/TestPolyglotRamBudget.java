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
		Assert.assertEquals(6 * 8_000_000L * 8, ls);
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
		// RAM 50 : 3,2 Go en LeekScript, borne a la part d'isolate d'un contexte.
		Assert.assertEquals(Math.min(50 * 8_000_000L * 8, share), maxRAM(50, ".js"));
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
