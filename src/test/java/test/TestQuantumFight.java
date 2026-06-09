package test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

/**
 * VRAI combat multi-tours : leek1 joue l'IA Quantum COMPLÈTE portée en JavaScript (ia-js, 188
 * fichiers, modules ES), montée via un FileSystem disque, contre un adversaire inerte.
 * Exerce le pipeline complet startFight -> EntityAI.build -> PolyglotEntityAI multi-fichiers.
 */
public class TestQuantumFight extends FightTestBase {

	private static final String JS_DIR = "/home/pierre/dev/leek-wars/ia-js";

	private Leek leek1;
	private Leek leek2;

	private static Leek combatLeek(int id, String name) {
		return new Leek(id, name, 0, 150, 2500, 18, 6, 450, 200, 300, 100, 100, 0, 0, 8, 64,
			0, false, 0, 0, "", 0, "", "", "", 0);
	}

	@Override
	protected void createLeeks() {
		leek1 = combatLeek(1, "Quantum-JS");
		leek2 = combatLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** FileSystem disque : sert le dossier d'un owner depuis le disque (racine = dir), liste récursive. */
	static class DiskFileSystem extends NativeFileSystem {
		private final int owner;
		private final String dir;
		private final Folder rootFolder;
		DiskFileSystem(int owner, String dir) {
			this.owner = owner;
			this.dir = dir;
			this.rootFolder = new Folder(0, owner, dir, null, null, this, 0);
			this.rootFolder.setParent(this.rootFolder);
			this.rootFolder.setRoot(this.rootFolder);
		}
		@Override public Folder getRoot() { return rootFolder; }
		@Override public Folder getRoot(int o) { return o == owner ? rootFolder : super.getRoot(o); }
		@Override public Folder getRoot(int o, int f) { return o == owner ? rootFolder : super.getRoot(o, f); }
		@Override public Iterable<AIFile> listAllFiles(int o) {
			List<AIFile> result = new ArrayList<>();
			if (o != owner) return result;
			Path base = Path.of(dir);
			try (Stream<Path> walk = Files.walk(base)) {
				walk.filter(p -> p.toString().endsWith(".js"))
					.filter(p -> !p.toString().contains("/.git/"))
					.forEach(p -> {
						try {
							String rel = base.relativize(p).toString();
							result.add(new AIFile(rel, Files.readString(p), 0, LeekScript.LATEST_VERSION, owner, false));
						} catch (Exception e) { throw new RuntimeException(e); }
					});
			} catch (Exception e) { throw new RuntimeException(e); }
			return result;
		}
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

	@Test
	public void quantumJsFullFight() throws Exception {
		new com.leekwars.generator.Generator(); // charge le catalogue puces/armes
		equipEverything(leek1);
		equipEverything(leek2);

		// FileSystem disque servant ia-js pour leek1
		LeekScript.setFileSystem(new DiskFileSystem(leek1.getId(), JS_DIR));

		// leek1 : entrée JS multi-fichiers ; leek2 : LeekScript inerte
		String entry = Files.readString(Path.of(JS_DIR, "quantum.js"));
		AIFile f1 = new AIFile("quantum.js", entry, 0, LeekScript.LATEST_VERSION, leek1.getId(), false);
		leek1.setAIFile(f1);
		leek1.setLogs(new LeekLog(farmerLog, leek1));
		leek1.setFight(fight);
		leek1.setBirthTurn(1);

		AIFile f2 = new AIFile("<dummy>", "", 0, LeekScript.LATEST_VERSION, leek2.getId(), false);
		leek2.setAIFile(f2);
		leek2.setLogs(new LeekLog(farmerLog, leek2));
		leek2.setFight(fight);
		leek2.setBirthTurn(1);

		leek1.setInitialCell(306);
		leek2.setInitialCell(318);

		runFight();

		System.out.println("[quantum-fight] type IA leek1 = " + leek1.getAI().getClass().getSimpleName());
		System.out.println("[quantum-fight] tours joues = " + fight.getState().getOrder().getTurn());
		System.out.println("[quantum-fight] leek1 vie = " + leek1.getLife() + "/" + leek1.getTotalLife()
			+ " cell=" + leek1.getCell() + " | leek2 vie = " + leek2.getLife() + " cell=" + leek2.getCell());
		String jsErr = leek1.getRegister("__errjs");
		if (jsErr != null) for (String l : jsErr.split("\n")) System.out.println("[quantum-fight] JSSTACK| " + l);
		String allLogs = farmerLog.toJSON().toString();
		int[] shown = {0};
		for (String line : allLogs.split("\\\\n|\",\"|\\],\\[")) {
			if (shown[0] < 60 && (line.contains("Best :") || line.contains("Aucune action") || line.contains("Items available")
				|| line.contains("success") || line.contains("failed!") || line.contains("Move to end") || line.contains("Error")
				|| line.contains("hard start") || line.contains("xception") || line.contains("peration") || line.contains("[8,\"\""))) {
				System.out.println("[quantum-fight] LOG| " + line); shown[0]++;
			}
		}
		// Compte les entrees d'erreur (code 8) toutes entites confondues
		int errCount = 0, idx = 0;
		while ((idx = allLogs.indexOf(",8,\"\",64,[", idx)) >= 0) { errCount++; idx += 5; }
		System.out.println("[quantum-fight] entrees code-8 (erreur IA) = " + errCount);
		System.out.println("[quantum-fight] 'hard start' occurrences = " + (allLogs.split("hard start", -1).length - 1));
		System.out.println("[quantum-fight] 'Items available' occurrences = " + (allLogs.split("Items available", -1).length - 1));
	}
}
