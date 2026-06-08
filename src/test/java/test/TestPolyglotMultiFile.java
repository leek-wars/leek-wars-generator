package test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotFileSystem;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.common.Error;
import leekscript.runner.LeekRunException;

/**
 * Multi-fichiers en combat : une IA dont l'entree importe des fichiers voisins du joueur (modules
 * ES en JS, import en Python), resolus via le {@link PolyglotFileSystem}. Verifie l'execution,
 * l'usage de l'API de combat depuis un module importe, et la persistance d'etat multi-fichiers.
 */
public class TestPolyglotMultiFile extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "MF1");
		leek2 = defaultLeek(2, "MF2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private PolyglotEntityAI multiFileAI(PolyglotSandbox sb, String lang, Map<String, String> files, String entryPath) {
		// Python : on delegue la stdlib GraalPy (sinon le FS custom la casserait).
		Path passthrough = "python".equals(lang) ? PolyglotSandbox.pythonStdlibRoot() : null;
		PolyglotFileSystem fs = new PolyglotFileSystem(files.keySet(), files::get, passthrough);
		PolyglotEntityAI ai = new PolyglotEntityAI(lang, files.get(entryPath), entryPath, fs, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	@Test
	public void jsMultiFileEsmImport() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("strategie.mjs", "export function pick() { return 42; }\n");
		files.put("main.mjs",
			"import { pick } from './strategie.mjs';\n"
			+ "globalThis.turn = function() { return pick() + getLife(); };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "js", files, "main.mjs").runIA()).longValue();
			Assert.assertEquals(42 + leek1.getLife(), r);
		}
	}

	@Test
	public void pythonMultiFileImport() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("strategie.py", "def pick():\n    return 42\n");
		files.put("main.py", "import strategie\ndef turn():\n    return strategie.pick() + getLife()\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "main.py").runIA()).longValue();
			Assert.assertEquals(42 + leek1.getLife(), r);
		}
	}

	@Test
	public void pythonMultiFilePackageImport() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		// Un sous-dossier "lib" = un package Python (resolu par le listing de dossier du FS).
		files.put("lib/helper.py", "def bonus():\n    return 7\n");
		files.put("main.py", "from lib import helper\ndef turn():\n    return helper.bonus()\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "main.py").runIA()).longValue();
			Assert.assertEquals(7, r);
		}
	}

	@Test
	public void pythonMultiFileStatePersistsAcrossTurns() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("mem.py", "class Mem:\n    n = 0\n");
		files.put("main.py", "from mem import Mem\ndef turn():\n    Mem.n += 1\n    return Mem.n\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			PolyglotEntityAI ai = multiFileAI(sb, "python", files, "main.py");
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(2L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(3L, ((Number) ai.runIA()).longValue());
		}
	}

	@Test
	public void pythonMultiFileSiblingAndStdlibTogether() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("strat.py", "def base():\n    return 40\n");
		// Importe a la fois un voisin (/ai) ET la stdlib (math) dans le meme contexte.
		files.put("main.py", "import strat, math\ndef turn():\n    return strat.base() + math.floor(2.7)\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "main.py").runIA()).longValue();
			Assert.assertEquals(42, r); // 40 + floor(2.7)=2
		}
	}

	@Test
	public void pythonPlayerFileCannotShadowStdlib() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("random.py", "def randint(a, b):\n    return 999\n"); // tentative de masquage de la stdlib
		files.put("main.py", "import random\ndef turn():\n    return random.randint(5, 5)\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "main.py").runIA()).longValue();
			Assert.assertEquals("la stdlib doit primer sur un fichier joueur random.py", 5, r);
		}
	}

	@Test
	public void pythonMultiFileCannotEscapeToHost() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		// Le FS composant delegue UNIQUEMENT le python-home (stdlib) ; tout autre chemin hote
		// reste inaccessible meme avec le passthrough actif.
		files.put("main.py", "def turn():\n    return open('/etc/passwd').read()\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			PolyglotEntityAI ai = multiFileAI(sb, "python", files, "main.py");
			try {
				ai.runIA();
				Assert.fail("la lecture d'un fichier hote aurait du echouer");
			} catch (LeekRunException e) {
				// attendu : acces refuse
			}
		}
	}

	@Test
	public void jsMultiFileLoadErrorIsReported() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("util.mjs", "export const X = ;\n"); // erreur de syntaxe dans un fichier importe
		files.put("main.mjs", "import { X } from './util.mjs';\nglobalThis.turn = function() { return X; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			PolyglotEntityAI ai = multiFileAI(sb, "js", files, "main.mjs");
			try {
				ai.runIA();
				Assert.fail("une erreur de chargement de module aurait du etre rapportee (pas silencieuse)");
			} catch (LeekRunException e) {
				Assert.assertEquals(Error.AI_INTERRUPTED, e.getError());
			}
		}
	}

	@Test
	public void jsMultiFileStatePersistsAcrossTurns() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("mem.mjs", "export class Mem { static n = 0; }\n");
		files.put("main.mjs",
			"import { Mem } from './mem.mjs';\n"
			+ "globalThis.turn = function() { Mem.n++; return Mem.n; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			PolyglotEntityAI ai = multiFileAI(sb, "js", files, "main.mjs");
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(2L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(3L, ((Number) ai.runIA()).longValue());
		}
	}
}
