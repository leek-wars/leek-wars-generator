package test;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * Sonde : un attaquant TRIVIAL (setWeapon + moveToward + useWeapon) contre un dummy, avec des
 * leeks equipes. Tranche : si le dummy prend des degats, le moteur+equipement sont OK (donc le
 * USE_INVALID_TARGET de Quantum vient de SON ciblage) ; sinon c'est l'equipement du harness.
 */
public class TestAttackProbe extends FightTestBase {

	private Leek att;
	private Leek dummy;

	private static Leek combatLeek(int id, String name) {
		// PV moderes (1200) pour que le combat se TERMINE dans la limite de tours.
		return new Leek(id, name, 0, 150, 1200, 18, 6, 450, 200, 300, 100, 100, 0, 0, 8, 64,
			0, false, 0, 0, "", 0, "", "", "", 0);
	}

	@Override
	protected void createLeeks() {
		att = combatLeek(1, "Attaquant-A");
		dummy = combatLeek(2, "Attaquant-B");
		fight.getState().addEntity(0, att);
		fight.getState().addEntity(1, dummy);
	}

	private void equipEverything(Leek leek) {
		for (com.leekwars.generator.FightConstants c : com.leekwars.generator.FightConstants.values()) {
			String n = c.name();
			try {
				if (n.startsWith("CHIP_")) {
					var chip = com.leekwars.generator.chips.Chips.getChip(c.getIntValue());
					if (chip != null) leek.addChip(chip);
				} else if (n.startsWith("WEAPON_")) {
					var w = com.leekwars.generator.weapons.Weapons.getWeapon(c.getIntValue());
					if (w != null) leek.addWeapon(w);
				}
			} catch (Exception ignore) {}
		}
	}

	private void attach(Leek leek, String filename, String code) {
		AIFile file = new AIFile(filename, code, 0, LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void polyglotFightFinishes() throws Exception {
		new com.leekwars.generator.Generator();
		equipEverything(att);
		equipEverything(dummy);

		// Deux attaquants triviaux (JS plat, rejoue chaque tour) : se rapprochent et tirent.
		String attacker =
			"var e = getNearestEnemy();\n"
			+ "if (e != null && e != -1) {\n"
			+ "  var ws = getWeapons();\n"
			+ "  if (ws.length > 0) setWeapon(ws[0]);\n"
			+ "  var c = getCellToUseWeapon(e);\n"
			+ "  if (c != null && c != -1) moveTowardCell(c); else moveToward(e);\n"
			+ "  while (useWeapon(e) > 0) {}\n"
			+ "}\n";
		attach(att, "a.js", attacker);
		attach(dummy, "b.js", attacker);

		runFight();

		boolean finished = att.getLife() <= 0 || dummy.getLife() <= 0;
		String winner = att.getLife() <= 0 ? dummy.getName() : (dummy.getLife() <= 0 ? att.getName() : "aucun (timeout)");
		System.out.println("[probe] tours=" + fight.getState().getOrder().getTurn());
		System.out.println("[probe] " + att.getName() + " vie=" + att.getLife() + " | " + dummy.getName() + " vie=" + dummy.getLife());
		System.out.println("[probe] COMBAT TERMINE = " + finished + " | vainqueur = " + winner);
	}
}
