package test;

import java.io.File;

import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.LeekLog;

import leekscript.LSException;
import leekscript.compiler.LeekScript;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;
import leekscript.runner.values.AbstractLeekValue;

public class GeneratorCompilation {

	static {
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions("com.leekwars.generator.FightFunctions");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
	}

	public static boolean testScriptGenerator(Entity entity, Fight fight, String code, AbstractLeekValue s) throws Exception {
		EntityAI ai = (EntityAI) LeekScript.compileSnippet(code, "com.leekwars.generator.fight.entity.EntityAI");
		ai.setEntity(entity);
		ai.setLogs(new LeekLog(null, entity));
		ai.setFight(fight);
		AbstractLeekValue v = ai.runIA();
		System.out.println(v.getString(ai));
		if (!v.equals(ai, s)) {
			throw new LSException(0, v, s);
		}
		return true;
	}
}
