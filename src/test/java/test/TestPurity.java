package test;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.leekwars.generator.FightFunctions;

import leekscript.common.Error;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.runner.AI;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

/**
 * Vérifie que la pureté déclarée sur les fonctions du générateur (FightFunctions)
 * est bien prise en compte par l'annotation {@code @pure} de LeekScript : appeler
 * une fonction de combat à effet de bord (déplacement, action, I/O, registre…)
 * depuis une fonction {@code @pure} doit être signalé, tandis qu'un getter en
 * lecture seule reste autorisé.
 */
public class TestPurity {

	@BeforeClass
	public static void setUp() {
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions(FightFunctions.getFunctions(), "com.leekwars.generator.classes.*");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
	}

	private static boolean notPure(String code) throws Exception {
		AI ai = LeekScript.compileSnippet(code, "com.leekwars.generator.fight.entity.EntityAI", new Options());
		List<AnalyzeError> errors = ai.getFile().getErrors();
		return errors.stream().anyMatch(e -> e.error == Error.ANNOTATION_NOT_PURE);
	}

	@Test
	public void impureFightFunctionsFlagged() throws Exception {
		// Déplacement
		Assert.assertTrue(notPure("@pure function f() { moveToward(getNearestEnemy()); return 0 } return f()"));
		// Action de combat
		Assert.assertTrue(notPure("@pure function f() { useChip(1, getNearestEnemy()); return 0 } return f()"));
		// I/O
		Assert.assertTrue(notPure("@pure function f() { say(\"hi\"); return 0 } return f()"));
		// Registre persistant
		Assert.assertTrue(notPure("@pure function f() { setRegister(\"a\", \"b\"); return 0 } return f()"));
	}

	@Test
	public void pureFightFunctionsAllowed() throws Exception {
		// Les getters ne lisent que l'état du combat : aucun effet de bord
		Assert.assertFalse(notPure("@pure function f() { return getLife() } return f()"));
		Assert.assertFalse(notPure("@pure function f() { return getCellDistance(1, 2) } return f()"));
		Assert.assertFalse(notPure("@pure function f() { return getNearestEnemy() } return f()"));
	}

	@Test
	public void impurityReachedThroughHelperFlagged() throws Exception {
		// La pureté est transitive : une fonction non annotée qui appelle une
		// fonction de combat impure rend impure la fonction @pure qui l'appelle.
		Assert.assertTrue(notPure("function helper() { say(\"x\"); return 0 } @pure function f() { return helper() } return f()"));
	}
}
