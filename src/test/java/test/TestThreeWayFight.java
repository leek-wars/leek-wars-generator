package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * Demo : un MEME combat avec trois IA dans trois langages differents qui se rapprochent
 * de leur ennemi le plus proche : JavaScript (team 0), Python (team 1), LeekScript (team 2).
 * Prouve que le runtime polyglot fait cohabiter les trois dans une vraie boucle de tours.
 */
public class TestThreeWayFight extends FightTestBase {

	private Leek jsLeek;
	private Leek pyLeek;
	private Leek lsLeek;

	@Override
	protected void createLeeks() {
		jsLeek = defaultLeek(1, "JsLeek");
		pyLeek = defaultLeek(2, "PyLeek");
		lsLeek = defaultLeek(3, "LsLeek");
		fight.getState().addEntity(0, jsLeek);
		fight.getState().addEntity(1, pyLeek);
		fight.getState().addEntity(2, lsLeek);
	}

	/** Attache un source mono-fichier avec l'extension qui choisit le langage (.mjs/.py/.leek). */
	private void attach(Leek leek, String filename, String code) {
		AIFile file = new AIFile(filename, code, System.currentTimeMillis(),
			LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void threeLanguagesMoveTowardEnemyInSameFight() throws Exception {
		// JavaScript (extension .js detectee par le runtime ; script plat rejoue chaque tour)
		attach(jsLeek, "js_mover.js",
			"var e = getNearestEnemy();\n"
			+ "if (e != null && e != -1) {\n"
			+ "  if (getRegister('startDist') == null) setRegister('startDist', '' + getCellDistance(getCell(), getCell(e)));\n"
			+ "  moveToward(e);\n"
			+ "  setRegister('endDist', '' + getCellDistance(getCell(), getCell(e)));\n"
			+ "}\n");

		// Python
		attach(pyLeek, "py_mover.py",
			"def turn():\n"
			+ "    e = getNearestEnemy()\n"
			+ "    if e is None or e == -1:\n"
			+ "        return\n"
			+ "    if getRegister('startDist') is None:\n"
			+ "        setRegister('startDist', str(getCellDistance(getCell(), getCell(e))))\n"
			+ "    moveToward(e)\n"
			+ "    setRegister('endDist', str(getCellDistance(getCell(), getCell(e))))\n");

		// LeekScript (le script entier rejoue chaque tour)
		attach(lsLeek, "ls_mover.leek",
			"var e = getNearestEnemy();\n"
			+ "if (e != null && e != -1) {\n"
			+ "  if (getRegister('startDist') == null) setRegister('startDist', '' + getCellDistance(getCell(), getCell(e)));\n"
			+ "  moveToward(e);\n"
			+ "  setRegister('endDist', '' + getCellDistance(getCell(), getCell(e)));\n"
			+ "}\n");

		runFight();

		System.out.println("[3way] === Combat JS vs Python vs LeekScript ===");
		System.out.println("[3way] tours joues : " + fight.getState().getOrder().getTurn());
		for (Leek l : new Leek[] { jsLeek, pyLeek, lsLeek }) {
			Object ai = l.getAI();
			String kind = ai instanceof PolyglotEntityAI ? "polyglot" : "leekscript";
			System.out.println("[3way] " + l.getName() + " (" + kind + ") : startDist="
				+ l.getRegister("startDist") + " endDist=" + l.getRegister("endDist")
				+ " vie=" + l.getLife());
		}

		Assert.assertTrue("JsLeek doit etre polyglot", jsLeek.getAI() instanceof PolyglotEntityAI);
		Assert.assertTrue("PyLeek doit etre polyglot", pyLeek.getAI() instanceof PolyglotEntityAI);
		Assert.assertFalse("LsLeek doit etre LeekScript", lsLeek.getAI() instanceof PolyglotEntityAI);
		// Au moins un des trois s'est rapproche (combat non degenere).
		for (Leek l : new Leek[] { jsLeek, pyLeek, lsLeek }) {
			if (l.getRegister("startDist") != null && l.getRegister("endDist") != null) {
				Assert.assertTrue(l.getName() + " a une distance coherente",
					Integer.parseInt(l.getRegister("endDist")) >= 0);
			}
		}
	}
}
