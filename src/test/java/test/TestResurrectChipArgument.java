package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * Le 3e paramètre de resurrect() passe depuis LeekScript, JS et Python. La cible est
 * vivante : l'appel s'arrête sur USE_RESURRECT_INVALID_ENTITY (-6), ce qui prouve qu'il
 * a atteint le moteur. Le comportement de la puce choisie est testé dans TestResurrectLife.
 */
public class TestResurrectChipArgument extends FightTestBase {

	private Leek leek, ally;

	@Override
	protected void createLeeks() {
		leek = defaultLeek(1, "A");
		ally = defaultLeek(2, "C");
		fight.getState().addEntity(0, leek);
		fight.getState().addEntity(0, ally);
		fight.getState().addEntity(1, defaultLeek(3, "B"));
	}

	private void run(AIFile file) throws Exception {
		attachAI(leek, file);
		for (var entity : fight.getState().getEntities().values()) {
			if (entity != leek) attachAI((Leek) entity, "");
		}
		runFight();
		Assert.assertEquals("-6", leek.getRegister("r"));
	}

	private static AIFile file(String extension, String code) {
		return new AIFile("resurrect_chip_" + System.nanoTime() + extension, code, System.currentTimeMillis(), LeekScript.LATEST_VERSION, 1, false);
	}

	@Test
	public void leekscript() throws Exception {
		run(newAIFile("setRegister('r', '' + resurrect(getNearestAlly(), getCell(), CHIP_AWAKENING))", 1));
	}

	@Test
	public void javascript() throws Exception {
		run(file(".js", "function turn() { Registers.set('r', '' + Fight.me.resurrect(Fight.getNearestAlly(), Fight.me.cell, 415)); }"));
	}

	@Test
	public void python() throws Exception {
		run(file(".py", "def turn():\n    Registers.set('r', str(Fight.me.resurrect(Fight.getNearestAlly(), Fight.me.cell, 415)))\n"));
	}
}
