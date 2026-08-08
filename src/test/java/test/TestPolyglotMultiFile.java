package test;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotFileSystem;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.compiler.LeekScript;
import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.resolver.FileSystem;
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
		return multiFileAI(sb, lang, files, entryPath, leek1);
	}

	private PolyglotEntityAI multiFileAI(PolyglotSandbox sb, String lang, Map<String, String> files, String entryPath, Leek entity) {
		// Python : on delegue la stdlib GraalPy (sinon le FS custom la casserait).
		Path passthrough = "python".equals(lang) ? PolyglotSandbox.pythonStdlibRoot() : null;
		// Miroir de PolyglotEntityAI.buildFileSystem : probing des imports sans extension pour JS.
		List<String> probe = "js".equals(lang) ? PolyglotFileSystem.JS_PROBE_EXTENSIONS : List.of();
		PolyglotFileSystem fs = new PolyglotFileSystem(files.keySet(), files::get, passthrough, probe, entryPath);
		PolyglotEntityAI ai = new PolyglotEntityAI(lang, files.get(entryPath), entryPath, fs, sb);
		ai.setEntity(entity);
		ai.setLogs(new LeekLog(farmerLog, entity));
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
			+ "globalThis.turn = function() { return pick() + Fight.me.life; };\n");
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
		files.put("main.py", "import strategie\ndef turn():\n    return strategie.pick() + Fight.me.life\n");
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
	public void pythonPlayerFileWithStdlibNameResolves() throws Exception {
		// Regression : un fichier joueur nomme comme un module stdlib NON sensible (ici `string`) doit
		// resoudre vers le fichier du JOUEUR. Avant le montage /ai en tete, `from string import toto`
		// resolvait la stdlib -> ImportError. Complementaire de pythonPlayerFileCannotShadowStdlib (les
		// modules SENSIBLES, eux, restent proteges).
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("string.py", "def toto():\n    return 7\n");
		files.put("main.py", "from string import toto\ndef turn():\n    return toto()\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "main.py").runIA()).longValue();
			Assert.assertEquals("un fichier joueur nomme comme un module stdlib non sensible doit primer", 7, r);
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

	/** Import SANS extension ({@code './strategie'}) : habitude Node/TS, probing .js/.mjs (#3179). */
	@Test
	public void jsMultiFileExtensionlessImport() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("strategie.js", "export function pick() { return 42; }\n");
		files.put("main.js",
			"import { pick } from './strategie';\n"
			+ "globalThis.turn = function() { return pick() + Fight.me.life; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "js", files, "main.js").runIA()).longValue();
			Assert.assertEquals(42 + leek1.getLife(), r);
		}
	}

	/** Specificateur bare ({@code 'strategie.js'} sans ./) : accepte, resolu contre /ai. */
	@Test
	public void jsMultiFileBareImport() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("strategie.js", "export function pick() { return 42; }\n");
		files.put("main.js",
			"import { pick } from 'strategie.js';\n"
			+ "globalThis.turn = function() { return pick() + Fight.me.life; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "js", files, "main.js").runIA()).longValue();
			Assert.assertEquals(42 + leek1.getLife(), r);
		}
	}

	/**
	 * Specificateur bare depuis un SOUS-DOSSIER, lib dans le MEME dossier ({@code import 'include.js'}
	 * depuis {@code ia-ts/test.js}) : cas reel remonte par Pilow sur beta — doit resoudre relativement
	 * au fichier importeur (option js.esm-bare-specifier-relative-lookup).
	 */
	@Test
	public void jsMultiFileBareImportSameFolder() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("ia-ts/include.js", "export function tout() { return 42; }\n");
		files.put("ia-ts/test.js",
			"import { tout } from 'include.js';\n"
			+ "globalThis.turn = function() { return tout() + Fight.me.life; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "js", files, "ia-ts/test.js").runIA()).longValue();
			Assert.assertEquals(42 + leek1.getLife(), r);
		}
	}

	/** Bare depuis un sous-dossier, lib a la RACINE : repli racine du FS (comportement historique). */
	@Test
	public void jsMultiFileBareImportRootFallback() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("strategie.js", "export function pick() { return 42; }\n");
		files.put("dossier/main.js",
			"import { pick } from 'strategie.js';\n"
			+ "globalThis.turn = function() { return pick() + Fight.me.life; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "js", files, "dossier/main.js").runIA()).longValue();
			Assert.assertEquals(42 + leek1.getLife(), r);
		}
	}

	/**
	 * Import sans extension quand un FICHIER {@code lib.js} et un DOSSIER {@code lib/} coexistent :
	 * le fichier gagne (resolution TypeScript). Cas reel : un dossier {@code test/} dans le compte
	 * rendait {@code import './test'} (visant test.ts) irresoluble en combat.
	 */
	@Test
	public void jsExtensionlessProbePrefersFileOverFolder() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("lib.js", "export const WHERE = 'racine';\n");
		files.put("lib/index.js", "export const WHERE = 'dossier';\n");
		files.put("main.js",
			"import { WHERE } from './lib';\n"
			+ "globalThis.turn = function() { return WHERE; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Object r = multiFileAI(sb, "js", files, "main.js").runIA();
			Assert.assertEquals("le fichier lib.js doit gagner sur le dossier lib/", "racine", String.valueOf(r));
		}
	}

	/** Import d'un dossier SANS fichier candidat : echoue toujours (pas de magie index.js, comme Node ESM). */
	@Test
	public void jsImportFolderWithoutCandidateFileFails() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("lib/index.js", "export const WHERE = 'dossier';\n");
		files.put("main.js",
			"import { WHERE } from './lib';\n"
			+ "globalThis.turn = function() { return WHERE; };\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			PolyglotEntityAI ai = multiFileAI(sb, "js", files, "main.js");
			try {
				ai.runIA();
				Assert.fail("l'import d'un dossier sans lib.js voisin aurait du echouer (pas de magie index.js)");
			} catch (LeekRunException e) {
				Assert.assertEquals(Error.AI_INTERRUPTED, e.getError());
			}
		}
	}

	/**
	 * Poireau bas niveau (RAM 6 -> cap guest ~12,6 Mo avant plancher) : la machinerie d'import GraalPy
	 * explosait le cap heap sur un simple {@code import voisin} alors que le mono-fichier passait.
	 * Le plancher Python doit laisser passer l'import (#3179).
	 */
	@Test
	public void pythonMultiFileImportFitsLowLevelRamCap() throws Exception {
		initFightOnly();
		Leek small = lowRamLeek();
		Map<String, String> files = new HashMap<>();
		files.put("strategie.py", "def pick():\n    return 42\n");
		files.put("main.py", "import strategie\ndef turn():\n    return strategie.pick() + Fight.me.life\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "main.py", small).runIA()).longValue();
			Assert.assertEquals(42 + small.getLife(), r);
		}
	}

	/**
	 * Meme garde que ci-dessus, mais entree en SOUS-DOSSIER : la forme signalee en prod (2026-07, deux
	 * joueurs annules a 33554432 octets des le setup du contexte sur du code trivial, dont un sur
	 * {@code leek-war-scripts/basic.py}). Cumule les deux conditions du signalement, poireau bas
	 * niveau ET entree en sous-dossier.
	 *
	 * <p>HONNETETE : ce cas minimal passait DEJA au plancher de 32 Mo en local (verifie en abaissant
	 * temporairement la constante ; il ne casse qu'a 8 Mo). Il ne reproduit donc pas le franchissement
	 * observe en prod, ou le vrai code joueur et l'etat de combat reel allouent davantage. Il garde le
	 * chemin, pas le seuil.
	 */
	@Test
	public void pythonSubfolderImportFitsLowLevelRamCap() throws Exception {
		initFightOnly();
		Leek small = lowRamLeek();
		Map<String, String> files = new HashMap<>();
		files.put("leek-war-scripts/strategie.py", "def pick():\n    return 42\n");
		files.put("leek-war-scripts/basic.py", "import strategie\ndef turn():\n    return strategie.pick() + Fight.me.life\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "leek-war-scripts/basic.py", small).runIA()).longValue();
			Assert.assertEquals(42 + small.getLife(), r);
		}
	}

	/**
	 * #4747 : poireau RAM 6 dont l'entree {@code import}e un module VOISIN a top-level charge (le vrai
	 * cas signale : un {@code main.py} de 16 Ko qui, a l'import, construit un dict de 613 cases, delie
	 * des listes-litterales de bordures et definit ses fonctions). A 64 Mo de plancher, ce setup
	 * saturait le cap heap guest AVANT la 1re action -> IA annulee, 0 operation, poireau inerte tout le
	 * combat. La baseline additive (cf PolyglotEntityAI) doit le laisser tourner.
	 *
	 * <p>Sandbox Python SEUL (et non "js","python") a dessein : c'est la config ou l'isolate Python est
	 * charge EN PROCESSUS, donc ou {@code sandbox.MaxHeapMemory} est reellement applique. Avec les deux
	 * langages, l'un bascule en isolate externe sans cap par contexte (cf PolyglotSandbox) et le test
	 * ne verifierait alors rien du budget RAM.
	 *
	 * <p>HONNETETE (comme les tests voisins) : ce cas passe des ~16 Mo en local ; il ne reproduit pas le
	 * seuil de prod (image isolate + etat de combat reels allouent davantage). Il garde le CHEMIN
	 * — import multi-fichiers d'un module lourd sur poireau bas niveau, cap enforce — pas le seuil.
	 */
	@Test
	public void pythonHeavyModuleImportFitsLowLevelRamCap() throws Exception {
		initFightOnly();
		Leek small = lowRamLeek();
		Map<String, String> files = new HashMap<>();
		// Voisin a top-level charge : dict de 613 cases + grosses listes-litterales, a la main.py de #4747.
		files.put("strat.py",
			"open_cells = {}\n"
			+ "for entry in Field.getObstacles():\n    open_cells[entry.id] = False\n"
			+ "for i in range(0, 613):\n    open_cells.setdefault(i, True)\n"
			+ "left_border = [0, 18, 35, 53, 70, 88, 105, 123, 140, 158, 175, 193, 210, 228, 245]\n"
			+ "right_border = [17, 34, 52, 69, 87, 104, 122, 139, 157, 174, 192, 209, 227, 244, 262]\n"
			+ "def pick():\n    return 42\n");
		files.put("main.py", "import strat\ndef turn():\n    return strat.pick() + Fight.me.life\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "main.py", small).runIA()).longValue();
			Assert.assertEquals(42 + small.getLife(), r);
		}
	}

	/** Poireau RAM 6 enregistre dans l'etat : Fight.me resout par fid, un fid non attribue viserait leek1. */
	private Leek lowRamLeek() {
		// defaultLeek plutot qu'un 26e appel positionnel a new Leek(...) : le seul stat qui compte ici
		// est la RAM, et il devient lisible au lieu d'etre le 16e argument d'une liste anonyme.
		Leek small = defaultLeek(3, "Small");
		small.setRAM(6);
		small.setFight(fight);
		fight.getState().addEntity(1, small);
		return small;
	}

	@Test
	public void pythonSiblingImportInSubfolder() throws Exception {
		// Hypothèse : fichiers dans un dossier -> montés sur /ai/<dossier>/... ; `from test import toto`
		// doit résoudre le voisin même quand l'entrée n'est pas à la racine de /ai.
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("MonIA/test.py", "def toto():\n    return 7\n");
		files.put("MonIA/main.py", "from test import toto\ndef turn():\n    return toto()\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "MonIA/main.py").runIA()).longValue();
			Assert.assertEquals("un voisin dans le même dossier doit être importable", 7, r);
		}
	}

	@Test
	public void pythonFlatAiNoneReturnDoesNotCrash() throws Exception {
		// Regression : une IA plate Python finissant sur None / une affectation / un import renvoie None
		// (ou les globals du module) ; la valeur de retour etant IGNOREE en combat, son marshalling ne
		// doit PAS interrompre l'IA en STACKOVERFLOW (ex utilisateur : base.py finissant par print(...)).
		initFightOnly();
		for (String last : new String[] { "None", "x = toto(2)", "from test import toto" }) {
			Map<String, String> files = new HashMap<>();
			files.put("ia-ts/test.py", "def toto(x):\n    return x * x\n");
			files.put("ia-ts/base.py", "from test import toto\n" + last + "\n");
			try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
				multiFileAI(sb, "python", files, "ia-ts/base.py").runIA(); // ne doit pas lever
			}
		}
	}

	@Test
	public void pythonReturnCyclicObjectDoesNotCrash() throws Exception {
		// Renforce le fix de marshalling : une IA plate renvoyant un objet CYCLIQUE (dict auto-referent,
		// ou `me` cyclique via cell.entity) ne doit pas interrompre l'IA (retour ignore en combat).
		initFightOnly();
		for (String last : new String[] { "me = Fight.me\nme", "d = {}\nd['x'] = d\nd" }) {
			Map<String, String> files = new HashMap<>();
			files.put("base.py", last + "\n");
			try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
				multiFileAI(sb, "python", files, "base.py").runIA(); // ne doit pas lever
			}
		}
	}

	@Test
	public void pythonSubfolderSiblingMultiTurn() throws Exception {
		// Import frere en sous-dossier + turn() rejouee : l'etat du voisin persiste, le montage du
		// dossier tient sur plusieurs tours.
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("dir/util.py", "count = 0\ndef step():\n    global count\n    count += 1\n    return count\n");
		files.put("dir/main.py", "from util import step\ndef turn():\n    return step()\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			var ai = multiFileAI(sb, "python", files, "dir/main.py");
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(2L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(3L, ((Number) ai.runIA()).longValue());
		}
	}

	@Test
	public void pythonDeeplyNestedSubfolderImport() throws Exception {
		// Import frere dans un dossier profond (a/b/c) : le dossier de l'entree est bien monte.
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("a/b/c/helper.py", "def val():\n    return 9\n");
		files.put("a/b/c/main.py", "from helper import val\ndef turn():\n    return val()\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			long r = ((Number) multiFileAI(sb, "python", files, "a/b/c/main.py").runIA()).longValue();
			Assert.assertEquals(9, r);
		}
	}

	@Test
	public void pythonBuildPathSubfolderImportEndToEnd() throws Exception {
		// Contrairement a multiFileAI (construction directe), CE test passe par le VRAI chemin de prod
		// PolyglotEntityAI.build() -> buildFileSystem() -> LeekScript.getFileSystem().listAllFiles() ->
		// montage. C'est le chemin qui n'etait PAS couvert et qui a laisse passer les regressions.
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("ia-ts/test.py", "def toto(x):\n    return x * x\n");
		files.put("ia-ts/base.py", "from test import toto\ndef turn():\n    return toto(3)\n");
		// build() lit entity.getLogs()/getFight() (createLeeks ne les cable pas, seul attachAI le fait).
		leek1.setLogs(new LeekLog(farmerLog, leek1));
		leek1.setFight(fight);
		leek1.setBirthTurn(1);
		FileSystem prev = LeekScript.getFileSystem();
		try {
			LeekScript.setFileSystem(new MemFileSystem(1, files));
			AIFile entry = new AIFile("ia-ts/base.py", files.get("ia-ts/base.py"), 1L, 4, 1, false);
			EntityAI ai = PolyglotEntityAI.build(generator, entry, leek1, "python");
			long r = ((Number) ai.runIA()).longValue();
			Assert.assertEquals("build() doit enumerer + monter le voisin en sous-dossier", 9, r);
		} finally {
			LeekScript.setFileSystem(prev);
			fight.closePolyglotSandbox();
		}
	}

	/** FileSystem LeekScript in-memory minimale (owner unique) pour tester le chemin build()/buildFileSystem. */
	static final class MemFileSystem extends FileSystem {
		private final int owner;
		private final Map<String, String> code;
		private final Folder root;
		MemFileSystem(int owner, Map<String, String> code) {
			this.owner = owner;
			this.code = code;
			this.root = new Folder(owner, this);
			this.root.setParent(root);
			this.root.setRoot(root);
		}
		private static String pathOf(Folder folder, String name) {
			var parts = new ArrayList<String>();
			parts.add(name);
			var cur = folder;
			while (cur != null && cur.getParent() != cur) {
				if (cur.getName() != null) parts.add(0, cur.getName());
				cur = cur.getParent();
			}
			return String.join("/", parts);
		}
		@Override public Iterable<AIFile> listAllFiles(int o) {
			var list = new ArrayList<AIFile>();
			for (var e : code.entrySet()) list.add(new AIFile(e.getKey(), e.getValue(), 1L, 4, owner, false));
			return list;
		}
		@Override public Folder getRoot() { return root; }
		@Override public Folder getRoot(int o) { return root; }
		@Override public Folder getRoot(int o, int f) { return root; }
		@Override public AIFile findFile(String name, Folder folder) throws FileNotFoundException {
			String p = pathOf(folder, name);
			String c = code.get(p);
			if (c == null) throw new FileNotFoundException(p);
			return new AIFile(p, c, 1L, 4, folder, owner, Math.abs(p.hashCode()) & 0xffffff, false);
		}
		@Override public Folder findFolder(String name, Folder folder) {
			String prefix = pathOf(folder, name) + "/";
			boolean exists = code.keySet().stream().anyMatch(k -> k.startsWith(prefix));
			return exists ? new Folder(Math.abs(prefix.hashCode()), owner, name, folder, root, this, 1L) : null;
		}
		@Override public AIFile getFileById(int id, int f) { return null; }
		@Override public Folder getFolderById(int id, int f) { return root; }
		@Override public long getAITimestamp(AIFile ai) { return 1L; }
		@Override public void loadDependencies(AIFile ai) { }
		@Override public long getFolderTimestamp(Folder folder) { return 1L; }
	}
}
