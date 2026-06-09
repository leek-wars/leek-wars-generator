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
 * Charge l'IA Quantum portee en Python (repo ia-py) via le FileSystem polyglot et execute UN
 * tour, pour faire remonter les erreurs de cablage runtime cote Python (acces globals
 * cross-module, cycles d'import, stdlib non bridgee...).
 */
public class TestQuantumPyLoad extends FightTestBase {

	private static final String ROOT = "/home/pierre/dev/leek-wars/ia-py";

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Quantum-PY");
		leek2 = defaultLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
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

	private Map<String, String> loadFiles() throws Exception {
		Map<String, String> files = new HashMap<>();
		Path base = Path.of(ROOT);
		try (Stream<Path> walk = Files.walk(base)) {
			walk.filter(p -> p.toString().endsWith(".py"))
				.filter(p -> !p.toString().contains("/.git/"))
				.forEach(p -> {
					try { files.put(base.relativize(p).toString(), Files.readString(p)); }
					catch (Exception e) { throw new RuntimeException(e); }
				});
		}
		return files;
	}

	@Test
	public void loadAndRunOneTurn() throws Exception {
		new com.leekwars.generator.Generator();
		equipEverything(leek1);
		initFightOnly();
		Map<String, String> files = loadFiles();
		System.out.println("[quantum-py] fichiers=" + files.size() + " entree=quantum.py presente=" + files.containsKey("quantum.py"));

		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			Path pass = PolyglotSandbox.pythonStdlibRoot();
			PolyglotFileSystem fs = new PolyglotFileSystem(files.keySet(), files::get, pass);
			PolyglotEntityAI ai = new PolyglotEntityAI("python", files.get("quantum.py"), "quantum.py", fs, sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			leek1.setAI(ai);
			try {
				Object r = ai.runIA();
				System.out.println("[quantum-py] OK tour 1, retour = " + r);
			} catch (leekscript.runner.LeekRunException e) {
				System.out.println("[quantum-py] LeekRunException code=" + e.getError());
				if (e.getParameters() != null)
					for (String p : e.getParameters())
						for (String line : p.split("\n")) System.out.println("[quantum-py] | " + line);
			} catch (Throwable t) {
				System.out.println("[quantum-py] ERREUR : " + t);
			}
			String err = leek1.getRegister("__err");
			if (err != null) for (String line : err.split("\n")) System.out.println("[quantum-py] TB| " + line);
			else System.out.println("[quantum-py] (pas de __err -> erreur a l'import/eval, pas dans turn())");
		}
	}
}
