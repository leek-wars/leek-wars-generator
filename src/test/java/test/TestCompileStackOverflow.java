package test;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.ErrorManager;
import com.leekwars.generator.Generator;
import com.leekwars.generator.leek.Leek;

import leekscript.compiler.AIFile;

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

	@Test
	public void deepExpressionInvalidatesOnlyItsAI() throws Exception {
		StringBuilder code = new StringBuilder("setRegister('ran', '1'); var x = 1");
		for (int i = 0; i < 50_000; i++) code.append(" + 1");
		code.append(";");
		attachAI(leek1, code.toString());
		attachAI(leek2, "setRegister('ran', '1');");

		var reported = new ArrayList<Throwable>();
		Generator.setErrorManager(new ErrorManager() {
			@Override public void exception(Throwable e, int fightID) { reported.add(e); }
			@Override public void exception(Throwable e, int fightID, int farmer, AIFile file) { reported.add(e); }
		});
		try {
			runFight();
		} finally {
			Generator.setErrorManager(null);
		}

		Assert.assertNull("l'IA qui ne compile pas n'a pas joué", leek1.getRegister("ran"));
		Assert.assertEquals("l'autre IA a joué son combat", "1", leek2.getRegister("ran"));
		Assert.assertTrue("le débordement reste signalé en erreur serveur : " + reported,
			reported.stream().anyMatch(e -> e instanceof StackOverflowError));
	}
}
