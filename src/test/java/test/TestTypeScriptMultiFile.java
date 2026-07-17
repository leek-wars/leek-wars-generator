package test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

/**
 * Multi-fichiers en TypeScript via le VRAI pipeline {@code EntityAI.build} : chaque .ts est
 * transpile a la lecture par le PolyglotFileSystem, et les import (en .js facon TS, ou en .ts)
 * resolvent les voisins du joueur. Exerce la branche TS de {@code buildFileSystem} (alias .js/.mjs
 * + transpile-on-read) et le chargement de l'entree via son alias module.
 */
public class TestTypeScriptMultiFile extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "TsMF1");
		leek2 = defaultLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** FileSystem disque servant les .ts d'un owner (liste recursive). */
	static class TsDiskFileSystem extends NativeFileSystem {
		private final int owner;
		private final String dir;
		private final Folder rootFolder;
		TsDiskFileSystem(int owner, String dir) {
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
				walk.filter(p -> p.toString().endsWith(".ts"))
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

	private void attachTsEntry(Leek leek, String dir, String entryRel) throws Exception {
		String entry = Files.readString(Path.of(dir, entryRel));
		AIFile f = new AIFile(entryRel, entry, 0, LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(f);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void tsMultiFileImportsSiblingAndSubfolder() throws Exception {
		Path dir = Files.createTempDirectory("ts-mf-");
		Files.createDirectories(dir.resolve("lib"));
		// Voisin importe en .js (convention TS ESM) bien que le fichier soit en .ts.
		Files.writeString(dir.resolve("util.ts"), "export function bonus(): number { return 7; }\n");
		// Sous-dossier importe en .ts explicite.
		Files.writeString(dir.resolve("lib/helper.ts"), "export const FACTOR: number = 10;\n");
		Files.writeString(dir.resolve("main.ts"), String.join("\n",
			"import { bonus } from './util.js';",
			"import { FACTOR } from './lib/helper.ts';",
			"globalThis.turn = function(): void {",
			"  const v: number = bonus() * FACTOR;",
			"  Registers.set('mod', '' + v);",
			"};"));

		LeekScript.setFileSystem(new TsDiskFileSystem(leek1.getId(), dir.toString()));
		attachTsEntry(leek1, dir.toString(), "main.ts");
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue("leek1 doit utiliser une IA polyglot (TS multi-fichiers)",
			leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertEquals("le module TS importe (.js + .ts) doit s'executer", "70", leek1.getRegister("mod"));
	}

	/** Import SANS extension ({@code './util'}) : convention TS par defaut, probing .js/.mjs (#3179). */
	@Test
	public void tsMultiFileExtensionlessImport() throws Exception {
		Path dir = Files.createTempDirectory("ts-mf-noext-");
		Files.writeString(dir.resolve("util.ts"), "export function bonus(): number { return 7; }\n");
		Files.writeString(dir.resolve("main.ts"), String.join("\n",
			"import { bonus } from './util';",
			"globalThis.turn = function(): void { Registers.set('noext', '' + bonus()); };"));

		LeekScript.setFileSystem(new TsDiskFileSystem(leek1.getId(), dir.toString()));
		attachTsEntry(leek1, dir.toString(), "main.ts");
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertEquals("l'import TS sans extension doit resoudre le voisin .ts", "7", leek1.getRegister("noext"));
	}

	@Test
	public void tsMultiFileImportedErrorIsNotMasked() throws Exception {
		// Erreur de syntaxe dans un fichier IMPORTE : elle ne doit pas etre silencieusement masquee
		// (l'import doit echouer -> turn() ne s'execute pas), pas tourner sur du JS partiel.
		Path dir = Files.createTempDirectory("ts-mf-err-");
		Files.writeString(dir.resolve("util.ts"), "export const X: number = ;\n"); // syntaxe invalide
		Files.writeString(dir.resolve("main.ts"), String.join("\n",
			"import { X } from './util.js';",
			"globalThis.turn = function(): void { Registers.set('x', '' + X); };"));

		LeekScript.setFileSystem(new TsDiskFileSystem(leek1.getId(), dir.toString()));
		attachTsEntry(leek1, dir.toString(), "main.ts");
		attachAI(leek2, "");
		runFight();

		Assert.assertNull("l'erreur d'un module TS importe ne doit pas etre masquee (turn ne doit pas tourner)",
			leek1.getRegister("x"));
	}

	@Test
	public void tsMultiFileStatePersistsAcrossTurns() throws Exception {
		Path dir = Files.createTempDirectory("ts-mf-state-");
		Files.writeString(dir.resolve("mem.ts"), "export class Mem { static n: number = 0; }\n");
		Files.writeString(dir.resolve("main.ts"), String.join("\n",
			"import { Mem } from './mem.js';",
			"globalThis.turn = function(): void {",
			"  Mem.n = Mem.n + 1;",
			"  Registers.set('turns', '' + Mem.n);",
			"};"));

		LeekScript.setFileSystem(new TsDiskFileSystem(leek1.getId(), dir.toString()));
		attachTsEntry(leek1, dir.toString(), "main.ts");
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		int turns = Integer.parseInt(leek1.getRegister("turns"));
		Assert.assertTrue("la static d'un module TS importe doit persister entre les tours, recu: " + turns, turns > 1);
	}
}
