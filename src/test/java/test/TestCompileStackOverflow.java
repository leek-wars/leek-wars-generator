package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * 5pilow/leek-wars#5318 : le compilateur LeekScript est récursif et peut déborder la pile
 * sur une expression imbriquée très profondément. Le StackOverflowError remontait jusqu'au
 * worker et faisait tomber tout le combat ; seule l'IA fautive doit être invalide.
 */
public class TestCompileStackOverflow extends FightTestBase {

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "A");
		leek2 = defaultLeek(2, "B");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private static String deepExpression() {
		StringBuilder code = new StringBuilder("setRegister('ran', '1'); var x = 1");
		for (int i = 0; i < 50_000; i++) code.append(" + 1");
		return code.append(";").toString();
	}

	/** Même débordement à l'analyse (sauvegarde dans l'éditeur, via le démon) : erreur interne, pas d'exception. */
	@Test
	public void deepExpressionAnalyzeReturnsInternalError() throws Exception {
		var reported = captureReportedErrors(() -> {
			AIFile file = new AIFile("<deep_analyze>", deepExpression(), System.currentTimeMillis(), LeekScript.LATEST_VERSION, 1, false);
			var result = generator.analyzeAI(file, 0);
			Assert.assertFalse(result.success);
			Assert.assertEquals(leekscript.common.Error.INTERNAL_ERROR.ordinal(), result.informations.get(0).get(6).asInt());
		});
		Assert.assertTrue(reported.stream().anyMatch(e -> e instanceof StackOverflowError));
	}

	@Test
	public void deepExpressionInvalidatesOnlyItsAI() throws Exception {
		attachAI(leek1, deepExpression());
		attachAI(leek2, "setRegister('ran', '1');");

		var reported = captureReportedErrors(this::runFight);

		Assert.assertNull("l'IA qui ne compile pas n'a pas joué", leek1.getRegister("ran"));
		Assert.assertEquals("l'autre IA a joué son combat", "1", leek2.getRegister("ran"));
		Assert.assertTrue("le débordement reste signalé en erreur serveur : " + reported,
			reported.stream().anyMatch(e -> e instanceof StackOverflowError));
	}
}
