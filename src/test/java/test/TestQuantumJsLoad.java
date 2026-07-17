package test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotFileSystem;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Charge l'IA Quantum portee en JavaScript (repo ia-js, ~187 fichiers) via le FileSystem
 * polyglot et execute UN tour, pour faire remonter les vraies erreurs de cablage runtime
 * (init circulaire, globals, BigInt...). Non assertif : on logge ce qui se passe.
 */
public class TestQuantumJsLoad extends FightTestBase {

	private static final String ROOT = "/home/pierre/dev/leek-wars/ia-js";

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Quantum-JS");
		leek2 = defaultLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private Map<String, String> loadFiles(String ext) throws Exception {
		Map<String, String> files = new HashMap<>();
		Path base = Path.of(ROOT);
		try (Stream<Path> walk = Files.walk(base)) {
			walk.filter(p -> p.toString().endsWith(ext))
				.filter(p -> !p.toString().contains("/.git/"))
				.forEach(p -> {
					try {
						String rel = base.relativize(p).toString();
						files.put(rel, Files.readString(p));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
		}
		return files;
	}

	/** Equipe le leek avec tout le catalogue (puces + armes) pour que Item.init trouve JUMP, etc. */
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
			} catch (Exception ignore) {
				// constante sans entree catalogue : on saute
			}
		}
	}

	@Test
	public void loadAndRunOneTurn() throws Exception {
		// IA Quantum locale (repo ia-js, non public) : test machine de dev seulement.
		org.junit.Assume.assumeTrue("repo ia-js absent, test saute (CI)", Files.isDirectory(Path.of(ROOT)));
		System.out.println("[quantum-js] cwd=" + System.getProperty("user.dir")
			+ " chips=" + com.leekwars.generator.chips.Chips.getTemplates().size()
			+ " weapons=" + com.leekwars.generator.weapons.Weapons.getTemplates().size());
		try {
			String s = com.leekwars.generator.util.Util.readFile("data/chips.json");
			System.out.println("[quantum-js] chips.json read len=" + (s == null ? "null" : s.length()));
			var o = com.leekwars.generator.util.Json.parseObject(s);
			System.out.println("[quantum-js] chips.json parsed keys=" + o.size());
		} catch (Throwable t) {
			System.out.println("[quantum-js] read/parse error: " + t);
		}
		// Recharge explicite du catalogue pour ce test
		new com.leekwars.generator.Generator();
		System.out.println("[quantum-js] apres reload: chips=" + com.leekwars.generator.chips.Chips.getTemplates().size());
		equipEverything(leek1);
		initFightOnly();
		Map<String, String> files = loadFiles(".js");
		System.out.println("[quantum-js] fichiers charges : " + files.size() + ", entree=quantum.js presente=" + files.containsKey("quantum.js"));

		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Sonde : que renvoie Chip.getAll() ? contient-il Chip.jump ? quel type ?
			PolyglotEntityAI probe = new PolyglotEntityAI("js",
				"var ac = Chip.getAll(); var n = ac.length; var found = -1;"
				+ "for (var i = 0; i < n; i++) { if (ac[i] === Chip.jump) found = i; }"
				+ "var isArr = Array.isArray(ac); var hasMap = (typeof ac.map);"
				+ "'len=' + n + ' t0=' + (typeof ac[0]) + ' v0=' + (ac[0] && ac[0].id)"
				+ " + ' Chip.jump=' + (Chip.jump && Chip.jump.id) + ' foundAt=' + found"
				+ " + ' isArray=' + isArr + ' typeof_map=' + hasMap;",
				sb);
			probe.setEntity(leek1);
			probe.setLogs(new LeekLog(farmerLog, leek1));
			probe.setFight(fight);
			try { System.out.println("[quantum-js] SONDE: " + probe.runIA()); }
			catch (Throwable pe) {
				System.out.println("[quantum-js] SONDE erreur: " + pe);
				if (pe instanceof leekscript.runner.LeekRunException le && le.getParameters() != null)
					for (String p : le.getParameters()) System.out.println("[quantum-js] SONDE| " + p);
			}

			PolyglotFileSystem fs = new PolyglotFileSystem(files.keySet(), files::get, null);
			PolyglotEntityAI ai = new PolyglotEntityAI("js", files.get("quantum.js"), "quantum.js", fs, sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			leek1.setAI(ai); // sinon leek.getAI() est null et le logging (debug) NPE
			System.out.println("[quantum-js] leek1 cell AVANT = " + leek1.getCell() + " | ennemi cell=" + leek2.getCell() + " dist=" + fight.getState().getMap().getDistance(leek1.getCell(), leek2.getCell()));
			for (int t = 1; t <= 5; t++) {
				try {
					ai.resetCounter(); // reset budget ops par tour (comme runTurn)
					Object r = ai.runIA();
					System.out.println("[quantum-js] OK tour " + t + ", retour = " + r + " | leek1 cell APRES = " + leek1.getCell() + " life enemy=" + leek2.getLife());
				} catch (leekscript.runner.LeekRunException e) {
					System.out.println("[quantum-js] tour " + t + " LeekRunException code=" + e.getError());
					if (e.getParameters() != null) {
						for (String p : e.getParameters()) {
							for (String line : p.split("\n")) System.out.println("[quantum-js] | " + line);
						}
					}
					String err = leek1.getRegister("__err");
					if (err != null) for (String line : err.split("\n")) System.out.println("[quantum-js] STACK| " + line);
					break;
				} catch (Throwable th) {
					System.out.println("[quantum-js] tour " + t + " ERREUR : " + th);
					break;
				}
			}
			System.out.println("[quantum-js] === LOGS IA ===");
			for (String line : farmerLog.toJSON().toString().split("\\\\n|\",\"|\\],\\[")) {
				if (line.contains("Quantum") || line.contains("Best") || line.contains("ction") || line.contains("Sequence") || line.contains("Destiny"))
					System.out.println("[quantum-js] LOG| " + line);
			}
		}
	}
}
