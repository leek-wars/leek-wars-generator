package test;

import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.FightFunctions;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.IACompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

import tools.jackson.databind.JsonNode;

/**
 * Vérif de COMPILATION de l'IA (entrypoint @strict) avant tout push.
 * Compile l'IA locale (LEEK_AI_ROOT, défaut = repo ia) et imprime tous les problèmes ;
 * échoue (assert) s'il reste la moindre ERREUR (level 0). level 0 = ERROR, 1 = WARNING.
 *
 * Usage : LEEK_AI_ROOT=/home/pierre/dev/leek-wars/ia gradle :test --tests test.TestCompileCheck --rerun-tasks
 */
public class TestCompileCheck {

	private static final String DEFAULT_ROOT = "/home/pierre/dev/leek-wars/ia";
	private static final String ENTRYPOINT = "Quantum";

	public static void main(String[] args) throws Exception {
		String root = System.getenv().getOrDefault("LEEK_AI_ROOT", DEFAULT_ROOT);
		String entry = System.getenv().getOrDefault("LEEK_ENTRYPOINT", ENTRYPOINT);
		int errors = run(root, entry);
		System.exit(errors > 0 ? 1 : 0);
	}

	@Test
	public void compileCheck() throws Exception {
		String root = System.getenv().getOrDefault("LEEK_AI_ROOT", DEFAULT_ROOT);
		String entry = System.getenv().getOrDefault("LEEK_ENTRYPOINT", ENTRYPOINT);
		org.junit.Assume.assumeTrue("LEEK_AI_ROOT absent (" + root + ")", new java.io.File(root).isDirectory());
		int errors = run(root, entry);
		org.junit.Assert.assertEquals("erreurs de compilation LeekScript (level 0)", 0, errors);
	}

	private static void registerGameNatives() {
		LeekFunctions.setExtraFunctions(FightFunctions.getFunctions(), "com.leekwars.generator.classes.*");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
		FightFunctions.getFunctions();
		FightConstants.values();
	}

	private static int run(String rootPath, String entrypoint) throws Exception {
		System.out.println("=== CompileCheck " + entrypoint + " (root=" + rootPath + ") ===");
		registerGameNatives();

		final Folder absRoot = new Folder(0, 0, rootPath, null, null, null, System.currentTimeMillis());
		absRoot.setParent(absRoot);
		absRoot.setRoot(absRoot);
		var nfs = new NativeFileSystem() {
			@Override public Folder getRoot() { return absRoot; }
			@Override public Folder getRoot(int owner) { return absRoot; }
			@Override public Folder getRoot(int owner, int farmer) { return absRoot; }
		};
		java.lang.reflect.Field fsField = Folder.class.getDeclaredField("fs");
		fsField.setAccessible(true);
		fsField.set(absRoot, nfs);
		LeekScript.setFileSystem(nfs);

		int errorCount = 0;
		try {
			AIFile ai = absRoot.resolve(entrypoint);
			System.out.println("entrypoint = " + ai.getPath() + " (" + ai.getCharCount() + " chars)");

			var result = new IACompiler().analyze(ai);
			System.out.println("success    = " + result.success);
			System.out.println("included   = " + (result.includedAIs == null ? -1 : result.includedAIs.size()));
			System.out.println("problems   = " + result.informations.size());
			System.out.println("\n--- problèmes ([level, file, l1, c1, l2, c2, errorOrdinal, params]) ---");
			for (JsonNode p : result.informations) {
				int level = p.get(0).asInt();
				if (level == 0) {
					errorCount++;
					System.out.println("ERROR    " + p.toString());
				}
			}
			int warnings = result.informations.size() - errorCount;
			System.out.println("\n=== ERREURS = " + errorCount + " | warnings = " + warnings + " ===");
			if (errorCount == 0) System.out.println("OK : 0 erreur de compilation, l'IA est saine.");
			else System.out.println("KO : NE PAS PUSHER, corriger les " + errorCount + " erreurs ci-dessus.");
		} finally {
			LeekScript.resetFileSystem();
		}
		return errorCount;
	}
}
