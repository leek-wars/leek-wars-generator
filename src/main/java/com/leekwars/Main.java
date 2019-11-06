package com.leekwars;

import java.io.File;

import com.alibaba.fastjson.JSON;
import com.leekwars.generator.DbContext;
import com.leekwars.generator.DbResolver;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;

import leekscript.compiler.LeekScript;
import leekscript.compiler.IACompiler.AnalyzeResult;

public class Main {

	private static final String TAG = Main.class.getSimpleName();

	public static void main(String[] args) {
		String file = null;
		boolean nocache = false;
		boolean db_resolver = false;
		boolean verbose = false;
		boolean analyze = false;
		int farmer = 0;

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
		if (analyze) {
			AnalyzeResult result = generator.analyzeAI(file, new DbContext(farmer, 0));
			System.out.println(result.informations);
		} else {
			Scenario scenario = Scenario.fromFile(new File(file));
			if (scenario == null) {
				Log.e(TAG, "Failed to parse scenario!");
				return;
			}
			Outcome outcome = generator.runScenario(scenario);
			System.out.println(JSON.toJSONString(outcome.toJson(), false));
		}
	}
}