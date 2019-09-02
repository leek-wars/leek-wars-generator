package com.leekwars;

import com.leekwars.generator.DbContext;
import com.leekwars.generator.DbResolver;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;

import leekscript.compiler.LeekScript;

public class Main {

	private static final String TAG = Main.class.getSimpleName();

	public static void main(String[] args) {
		String file = null;
		boolean nocache = false;
		boolean db_resolver = false;
		boolean verbose = false;
		boolean compile = false;
		int farmer = 0;

		for (String arg : args) {
			if (arg.startsWith("--")) {
				switch (arg.substring(2)) {
					case "nocache": nocache = true; break;
					case "dbresolver": db_resolver = true; break;
					case "verbose": verbose = true; break;
					case "compile": compile = true; break;
				}
				if (arg.startsWith("--farmer=")) {
					farmer = Integer.parseInt(arg.substring("--farmer=".length()));
				}
			} else {
				file = arg;
			}
		}
		Log.enable(verbose);
		Log.i(TAG, "Generator v1");
		if (file == null) {
			Log.i(TAG, "No scenario/ai file passed!");
			return;
		}
		if (db_resolver) {
			DbResolver dbResolver = new DbResolver("../resolver.php");
			LeekScript.setResolver(dbResolver);
		}
		Generator generator = new Generator();
		generator.setNocache(nocache);
		if (compile) {
			String result = generator.compileAI(file, new DbContext(farmer, 0));
			System.out.println(result);
		} else {
			String result = generator.runScenarioFile(file);
			System.out.println(result);
		}
	}
}