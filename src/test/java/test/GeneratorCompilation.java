package test;

import java.io.File;

import com.leekwars.generator.FightFunctions;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;

import leekscript.LSException;
import leekscript.compiler.LeekScript;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class GeneratorCompilation {

	static {
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions(FightFunctions.getFunctions(), "com.leekwars.generator.classes.*");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
	}

	public static boolean testScriptGenerator(Entity entity, Fight fight, String code, Object s) throws Exception {
		EntityAI ai = (EntityAI) LeekScript.compileSnippet(code, "com.leekwars.generator.fight.entity.EntityAI");
		ai.setEntity(entity);
		var fl = new FarmerLog(fight, 0);
		ai.setLogs(new LeekLog(fl, entity));
		ai.setFight(fight);
		var v = ai.runIA();
		System.out.println(ai.string(v));
		if (!ai.eq(v, s)) {
			throw new LSException(0, v, s);
		}
		return true;
	}
}
