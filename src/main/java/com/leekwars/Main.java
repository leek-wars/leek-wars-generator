package com.leekwars;

import java.io.File;
import java.io.FileNotFoundException;

import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.test.LocalDbFileSystem;
import com.leekwars.generator.test.LocalDbRegisterManager;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.LeekScript;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.resolver.NativeFileSystem;

public class Main {

	private static final String TAG = Main.class.getSimpleName();

	public static void main(String[] args) {
		String file = null;
		boolean nocache = false;
		boolean db_resolver = false;
		boolean verbose = false;
		boolean analyze = false;
		int farmer = 0;
		int folder = 0;

		boolean test = false;
		String root = null;
		var files = new java.util.ArrayList<String>();

		for (String arg : args) {
			if (arg.startsWith("--")) {
				switch (arg.substring(2)) {
					case "test": test = true; break;
					case "nocache": nocache = true; break;
					case "dbresolver": db_resolver = true; break;
					case "verbose": verbose = true; break;
					case "analyze": analyze = true; break;
				}
				if (arg.startsWith("--farmer=")) {
					farmer = Integer.parseInt(arg.substring("--farmer=".length()));
				} else if (arg.startsWith("--folder=")) {
					folder = Integer.parseInt(arg.substring("--folder=".length()));
				} else if (arg.startsWith("--root=")) {
					root = arg.substring("--root=".length());
				}
			} else {
				file = arg;
				files.add(arg);
			}
		}
		Log.enable(verbose);
		Log.i(TAG, "Generator v1");
		// System.out.println("db_resolver " + db_resolver + " folder=" + folder + " farmer=" + farmer);
		if (file == null) {
			Log.i(TAG, "No scenario/ai file passed!");
			return;
		}

		// Data.checkData("https://leekwars.com/api/");
		if (db_resolver) {
			LeekScript.setFileSystem(new LocalDbFileSystem());
		} else {
			LeekScript.setFileSystem(new NativeFileSystem());
		}
		Generator generator = new Generator();
		generator.setCache(!nocache);
		if (test) {
			System.exit(runTests(generator, root, files) ? 0 : 1);
		}
		if (analyze) {
			try {
				var ai = LeekScript.getFileSystem().getRoot().resolve(file);
				AnalyzeResult result = generator.analyzeAI(ai, 0);
				System.out.println(result.informations);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found!");
			}
		} else {
			Scenario scenario = Scenario.fromFile(new File(file));
			if (scenario == null) {
				Log.e(TAG, "Failed to parse scenario!");
				return;
			}
			Outcome outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(), new LocalTrophyManager());
			System.out.println(outcome.toJson().toString());
		}
	}

	/**
	 * `--test [--root=<dossier>] <module de test>…` : lance les tests de chaque module (chemin
	 * relatif à la racine, `util/test/tas.test.leek`) dans des combats bac à sable et écrit le
	 * rapport en JSON. Rend vrai si tout est passé.
	 */
	private static boolean runTests(Generator generator, String root, java.util.List<String> modules) {
		if (root != null) {
			final leekscript.compiler.Folder[] holder = new leekscript.compiler.Folder[1];
			var fs = new NativeFileSystem() {
				@Override public leekscript.compiler.Folder getRoot() { return holder[0]; }
				@Override public leekscript.compiler.Folder getRoot(int owner) { return holder[0]; }
				@Override public leekscript.compiler.Folder getRoot(int owner, int farmer) { return holder[0]; }
			};
			// Propriétaire 1 : celui à qui les versions pas encore publiques sont ouvertes par défaut
			var folder = new leekscript.compiler.Folder(0, 1, root, null, null, fs, System.currentTimeMillis());
			folder.setParent(folder);
			folder.setRoot(folder);
			holder[0] = folder;
			LeekScript.setFileSystem(fs);
		}
		var results = new java.util.ArrayList<leekscript.testing.TestResult>();
		for (var path : modules) {
			try {
				var module = LeekScript.getFileSystem().getRoot().resolve(path);
				results.addAll(com.leekwars.generator.test.FightTestRunner.runModule(generator, module));
			} catch (Throwable e) {
				// Module introuvable ou qui ne compile pas : une erreur à son nom, les autres continuent
				results.add(new leekscript.testing.TestResult(path, "<module>", 0, leekscript.testing.TestResult.Status.ERROR, null, 0, 0, 0,
					java.util.List.of(), java.util.List.of(), String.valueOf(e), java.util.List.of()));
			}
		}
		var report = com.leekwars.generator.test.FightTestRunner.report(results);
		System.out.println(report.toString());
		return report.get("summary").get("ok").booleanValue();
	}
}
