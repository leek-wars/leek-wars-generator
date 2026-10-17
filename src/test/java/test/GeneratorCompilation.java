package test;

import java.io.File;

import com.leekwars.generator.state.Entity;
import com.leekwars.generator.FightFunctions;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;

import leekscript.LSException;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class GeneratorCompilation {

	static {
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions(FightFunctions.getFunctions(), "com.leekwars.generator.classes.*");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
	}

	public static boolean testScriptGenerator(Entity entity, Fight fight, String code, Object expected) throws Exception {
		EntityAI ai = compile(entity, fight, code, new Options(true));
		var result = ai.runIA();
		var resultStr = ai.export(result);
		System.out.println(resultStr);
		// Compare via export() to avoid type mismatches (Integer vs Long, ArrayLeekValue vs LegacyArrayLeekValue, etc.)
		var expectedStr = ai.export(expected);
		if (!resultStr.equals(expectedStr)) {
			throw new LSException(0, result, expected);
		}
		return true;
	}

	/** Exécute un extrait comme IA de `entity` et renvoie son résultat exporté. */
	public static String runSnippet(Entity entity, Fight fight, String code, Options options) throws Exception {
		EntityAI ai = compile(entity, fight, code, options);
		return ai.export(ai.runIA());
	}

	private static EntityAI compile(Entity entity, Fight fight, String code, Options options) throws Exception {
		EntityAI ai = (EntityAI) LeekScript.compileSnippet(code, "com.leekwars.generator.fight.entity.EntityAI", options);
		ai.setEntity(entity);
		ai.setLogs(new LeekLog(new FarmerLog(fight, 0), entity));
		ai.setFight(fight);
		return ai;
	}
}
