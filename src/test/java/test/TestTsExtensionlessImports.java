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
 * Imports TS bare et sans extension via le vrai pipeline {@code EntityAI.build}, dont le cas reel
 * (Pilow, prod 17/07) : un DOSSIER {@code test/} (vieux fichiers LeekScript) a la racine du compte
 * rendait {@code import './test'} (visant {@code test.ts}) irresoluble en combat :
 * "Cannot find module '/ai/test' imported from /ai/test.js". Resolution TypeScript attendue :
 * le fichier gagne sur le dossier homonyme.
 */
public class TestTsExtensionlessImports extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "TsNoExt1");
		leek2 = defaultLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** FileSystem disque servant TOUS les fichiers d'un owner (liste recursive, comme le vrai FS). */
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
				walk.filter(Files::isRegularFile)
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

	/** Bare, sans extension ({@code import {toto} from 'test'}), lib test.ts a la racine. */
	@Test
	public void tsBareExtensionlessImportRoot() throws Exception {
		Path dir = Files.createTempDirectory("ts-bare-noext-");
		Files.writeString(dir.resolve("test.ts"), "export const toto: number = 7;\n");
		Files.writeString(dir.resolve("main.ts"), String.join("\n",
			"import { toto } from 'test';",
			"globalThis.turn = function(): void { Registers.set('bare', '' + toto); };"));

		LeekScript.setFileSystem(new DiskFileSystem(leek1.getId(), dir.toString()));
		attachTsEntry(leek1, dir.toString(), "main.ts");
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertEquals("import bare sans extension doit resoudre test.ts", "7", leek1.getRegister("bare"));
	}

	/**
	 * Cas reel : dossier {@code test/} (LeekScript legacy) + fichier {@code test.ts} a la racine ;
	 * {@code import './test'} doit resoudre le FICHIER, pas buter sur le dossier.
	 */
	@Test
	public void tsExtensionlessImportShadowedByFolder() throws Exception {
		Path dir = Files.createTempDirectory("ts-noext-shadow-");
		Files.createDirectories(dir.resolve("test"));
		Files.writeString(dir.resolve("test/legacy"), "var x = 1\n"); // vieux fichier LeekScript
		Files.writeString(dir.resolve("test.ts"), "export function toto(x: number): number { return x * x; }\n");
		Files.writeString(dir.resolve("main.ts"), String.join("\n",
			"import { toto } from './test';",
			"globalThis.turn = function(): void { Registers.set('shadow', '' + toto(12)); };"));

		LeekScript.setFileSystem(new DiskFileSystem(leek1.getId(), dir.toString()));
		attachTsEntry(leek1, dir.toString(), "main.ts");
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertEquals("test.ts doit gagner sur le dossier test/", "144", leek1.getRegister("shadow"));
	}

	/** Meme cas en specificateur BARE ({@code from 'test'}), la forme du rapport utilisateur. */
	@Test
	public void tsBareImportShadowedByFolder() throws Exception {
		Path dir = Files.createTempDirectory("ts-bare-shadow-");
		Files.createDirectories(dir.resolve("test"));
		Files.writeString(dir.resolve("test/legacy"), "var x = 1\n");
		Files.writeString(dir.resolve("test.ts"), "export function toto(x: number): number { return x + 1; }\n");
		Files.writeString(dir.resolve("main.ts"), String.join("\n",
			"import { toto } from 'test';",
			"globalThis.turn = function(): void { Registers.set('bareShadow', '' + toto(41)); };"));

		LeekScript.setFileSystem(new DiskFileSystem(leek1.getId(), dir.toString()));
		attachTsEntry(leek1, dir.toString(), "main.ts");
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertEquals("bare 'test' doit resoudre test.ts malgre le dossier test/", "42", leek1.getRegister("bareShadow"));
	}
}
