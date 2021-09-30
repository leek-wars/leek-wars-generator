package com.leekwars;

import java.io.File;

import com.alibaba.fastjson.JSON;
import com.leekwars.generator.Data;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.test.LocalDbRegisterManager;
import com.leekwars.generator.test.LocalDbResolver;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.LeekScript;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.resolver.FileSystemContext;
import leekscript.compiler.resolver.FileSystemResolver;

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

		for (String arg : args) {
			if (arg.startsWith("--")) {
				switch (arg.substring(2)) {
					case "nocache": nocache = true; break;
					case "dbresolver": db_resolver = true; break;
					case "verbose": verbose = true; break;
					case "analyze": analyze = true; break;
				}
				if (arg.startsWith("--farmer=")) {
					farmer = Integer.parseInt(arg.substring("--farmer=".length()));
				} else if (arg.startsWith("--folder=")) {
					folder = Integer.parseInt(arg.substring("--folder=".length()));
				}
			} else {
				file = arg;
			}
		}
		Log.enable(verbose);
		Log.i(TAG, "Generator v1");
		System.out.println("db_resolver " + db_resolver + " folder=" + folder + " farmer=" + farmer);
		if (file == null) {
			Log.i(TAG, "No scenario/ai file passed!");
			return;
		}

		Data.checkData("https://leekwars.com/api/");
		if (db_resolver) {
			LeekScript.setResolver(new LocalDbResolver());
		} else {
			LeekScript.setResolver(new FileSystemResolver());
		}
		Generator generator = new Generator();
		generator.setCache(!nocache);
		if (analyze) {
			AnalyzeResult result = generator.analyzeAI(file, new FileSystemContext(new File(".")));
			// AnalyzeResult result = generator.analyzeAI(file, new DbContext(farmer, folder));
			System.out.println(result.informations);
		} else {
			Scenario scenario = Scenario.fromFile(new File(file));
			if (scenario == null) {
				Log.e(TAG, "Failed to parse scenario!");
				return;
			}
			Outcome outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(), new LocalTrophyManager());
			System.out.println(JSON.toJSONString(outcome.toJson(), false));
		}
	}
}