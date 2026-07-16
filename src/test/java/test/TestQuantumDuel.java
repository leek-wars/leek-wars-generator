package test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

/**
 * DUEL : l'IA Quantum COMPLETE en JavaScript (ia-js) contre la MEME IA portee en Python (ia-py),
 * dans un vrai combat multi-tours sur le runtime polyglot.
 */
public class TestQuantumDuel extends FightTestBase {

	private static final String JS_DIR = "/home/pierre/dev/leek-wars/ia-js";
	private static final String PY_DIR = "/home/pierre/dev/leek-wars/ia-py";

	private Leek jsLeek;
	private Leek pyLeek;

	/** Leek de combat realiste : haute TP + force pour que l'IA juge l'attaque rentable. */
	private static Leek combatLeek(int id, String name) {
		// (id, name, farmer, level, life, TP, MP, force, agility, frequency, wisdom, resistance,
		//  science, magic, cores, ram, ...)
		return new Leek(id, name, 0, 150, 2500, 18, 6, 450, 200, 300, 100, 100, 0, 0, 8, 64,
			0, false, 0, 0, "", 0, "", "", "", 0);
	}

	@Override
	protected void createLeeks() {
		jsLeek = combatLeek(1, "Quantum-JS");
		pyLeek = combatLeek(2, "Quantum-PY");
		fight.getState().addEntity(0, jsLeek);
		fight.getState().addEntity(1, pyLeek);
	}

	/** FileSystem disque multi-owner : sert un dossier (et son extension) par owner. */
	static class MultiDiskFileSystem extends NativeFileSystem {
		private final Map<Integer, String> dirs;     // owner -> dossier
		private final Map<Integer, String> exts;      // owner -> extension (.js/.py)
		private final Map<Integer, Folder> roots = new HashMap<>();
		MultiDiskFileSystem(Map<Integer, String> dirs, Map<Integer, String> exts) {
			this.dirs = dirs;
			this.exts = exts;
			for (var e : dirs.entrySet()) {
				Folder r = new Folder(0, e.getKey(), e.getValue(), null, null, this, 0);
				r.setParent(r); r.setRoot(r);
				roots.put(e.getKey(), r);
			}
		}
		@Override public Folder getRoot() { return roots.values().iterator().next(); }
		@Override public Folder getRoot(int o) { return roots.getOrDefault(o, super.getRoot(o)); }
		@Override public Folder getRoot(int o, int f) { return getRoot(o); }
		@Override public Iterable<AIFile> listAllFiles(int o) {
			List<AIFile> result = new ArrayList<>();
			String dir = dirs.get(o);
			if (dir == null) return result;
			String ext = exts.get(o);
			Path base = Path.of(dir);
			try (Stream<Path> walk = Files.walk(base)) {
				walk.filter(p -> p.toString().endsWith(ext))
					.filter(p -> !p.toString().contains("/.git/"))
					.forEach(p -> {
						try {
							String rel = base.relativize(p).toString();
							result.add(new AIFile(rel, Files.readString(p), 0, LeekScript.LATEST_VERSION, o, false));
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

	private void attachEntry(Leek leek, String dir, String entry) throws Exception {
		String code = Files.readString(Path.of(dir, entry));
		AIFile f = new AIFile(entry, code, 0, LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(f);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void jsVsPython() throws Exception {
		// IA Quantum locale (repos ia-js/ia-py, non publics) : test machine de dev seulement.
		org.junit.Assume.assumeTrue("repos ia-js/ia-py absents, test saute (CI)",
			Files.isDirectory(Path.of(JS_DIR)) && Files.isDirectory(Path.of(PY_DIR)));
		new com.leekwars.generator.Generator();
		equipEverything(jsLeek);
		equipEverything(pyLeek);

		Map<Integer, String> dirs = new HashMap<>();
		dirs.put(jsLeek.getId(), JS_DIR);
		dirs.put(pyLeek.getId(), PY_DIR);
		Map<Integer, String> exts = new HashMap<>();
		exts.put(jsLeek.getId(), ".js");
		exts.put(pyLeek.getId(), ".py");
		LeekScript.setFileSystem(new MultiDiskFileSystem(dirs, exts));

		attachEntry(jsLeek, JS_DIR, "quantum.js");
		attachEntry(pyLeek, PY_DIR, "quantum.py");

		// Placement initial RAPPROCHE (a portee) pour que l'engagement domine la temporisation.
		jsLeek.setInitialCell(306);
		pyLeek.setInitialCell(318);

		runFight();

		System.out.println("[duel] === 30 PREMIERES LIGNES DE LOG ===");
		int[] shown = {0};
		for (String line : farmerLog.toJSON().toString().split("\\\\n|\",\"|\\],\\[")) {
			if (shown[0] < 40 && (line.contains("Best :") || line.contains("Move to end") || line.contains("success") || line.contains("failed!") || line.contains("Aucune action") || line.contains("No generator") || line.contains("TypeError") || line.contains("Error"))) {
				System.out.println("[duel] LOG| " + line); shown[0]++;
			}
		}
		System.out.println("[duel] IA JS  = " + jsLeek.getAI().getClass().getSimpleName()
			+ " | IA PY = " + pyLeek.getAI().getClass().getSimpleName());
		System.out.println("[duel] tours joues = " + fight.getState().getOrder().getTurn());
		System.out.println("[duel] " + jsLeek.getName() + " vie=" + jsLeek.getLife() + "/" + jsLeek.getTotalLife() + " cell=" + jsLeek.getCell());
		System.out.println("[duel] " + pyLeek.getName() + " vie=" + pyLeek.getLife() + "/" + pyLeek.getTotalLife() + " cell=" + pyLeek.getCell());
		System.out.println("[duel] gagnant = " + (jsLeek.getLife() <= 0 ? "Python" : (pyLeek.getLife() <= 0 ? "JS" : "egalite/timeout")));
	}
}
