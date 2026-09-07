package test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * GraalPy s'annonce Python 3.12 mais son module `math` n'a ni cbrt ni exp2 (ajoutes en CPython
 * 3.11) : une IA Python plantait en AttributeError sur `math.cbrt` (#5031). Le prelude comble ces
 * deux trous dans le module `math` lui-meme, la ou un auteur Python les cherche.
 */
public class TestPolyglotPythonMath extends FightTestBase {

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "M1");
		fight.getState().addEntity(0, leek1);
		Leek leek2 = defaultLeek(2, "M2");
		fight.getState().addEntity(1, leek2);
	}

	private String run(PolyglotSandbox sb, String expr) throws Exception {
		PolyglotEntityAI ai = new PolyglotEntityAI("python",
			"import math\ndef turn():\n    return repr(" + expr + ")", sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return String.valueOf(ai.runIA());
	}

	@Test
	public void cbrtAndExp2AreAvailableInMathModule() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			// Exact sur les cubes parfaits (x ** (1/3) donnerait 3.0000000000000004) et defini sur
			// les negatifs (x ** (1/3) donnerait un complexe).
			assertEquals("3.0", run(sb, "math.cbrt(27)"));
			assertEquals("-2.0", run(sb, "math.cbrt(-8)"));
			assertEquals("True", run(sb, "isinstance(math.cbrt(8), float)"));
			assertEquals("1024.0", run(sb, "math.exp2(10)"));
			assertEquals("0.5", run(sb, "math.exp2(-1)"));
		}
	}
}
