package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * console.* pour les IA JS/TS : graaljs fournit console mais sa sortie est jetee (nullOutputStream) ;
 * on reroute console.* vers debug() (visible dans le rapport), avec un try/catch pour ne JAMAIS faire
 * echouer l'IA si debug leve. Les IA utilisent console.log par reflexe.
 */
public class TestPolyglotConsole extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "C1");
		leek2 = defaultLeek(2, "C2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	@Test
	public void consoleNeverThrows() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = new PolyglotEntityAI("js",
				"console.log('a', 1, {x:2}); console.error('e'); console.warn('w'); console.info('i'); 42;", sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			Object r = ai.runIA();
			Assert.assertEquals("console.* ne doit JAMAIS faire echouer l'IA", 42L, ((Number) r).longValue());
		}
	}

	@Test
	public void consoleLogInRealFight() throws Exception {
		// En vrai combat : une IA qui fait console.log doit continuer (setRegister apres console.log).
		AIFile f = new AIFile("console_test.js",
			"function turn() { console.log('turn', getLife()); setRegister('ran', '1'); }",
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek1.getId(), false);
		leek1.setAIFile(f);
		leek1.setLogs(new LeekLog(farmerLog, leek1));
		leek1.setFight(fight);
		leek1.setBirthTurn(1);
		attachAI(leek2, "");
		runFight();
		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertEquals("le tour doit continuer apres console.log", "1", leek1.getRegister("ran"));
	}
}
