package test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.leek.Leek;

import leekscript.compiler.AIFile;

/**
 * 5pilow/leek-wars#5318 : le compilateur LeekScript est récursif et peut déborder la pile
 * sur une expression imbriquée très profondément. Le StackOverflowError remontait jusqu'au
 * worker et faisait tomber tout le combat ; seule l'IA fautive doit être invalide.
 */
public class TestCompileStackOverflow extends FightTestBase {

	private Leek leek1, leek2, leek3;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "A");
		leek2 = defaultLeek(2, "B");
		leek3 = defaultLeek(3, "C");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
		fight.getState().addEntity(0, leek3);
	}

	/**
	 * Pile du thread qui compile. Explicite et petite : 50 000 termes la débordent à coup sûr,
	 * quelle que soit la pile par défaut du thread de test (-Xss, JVM, plateforme).
	 */
	private static final long SMALL_STACK_BYTES = 512 * 1024;

	/** Exécute `action` dans un thread à pile de {@link #SMALL_STACK_BYTES} et relance son échec. */
	private static void runWithSmallStack(ThrowingRunnable action) throws Exception {
		var failure = new AtomicReference<Throwable>();
		Thread thread = new Thread(null, () -> {
			try {
				action.run();
			} catch (Throwable t) {
				failure.set(t);
			}
		}, "small-stack", SMALL_STACK_BYTES);
		thread.start();
		thread.join();
		Throwable t = failure.get();
		if (t instanceof Exception e) throw e;
		if (t instanceof Error e) throw e;
	}

	private static String deepExpression() {
		StringBuilder code = new StringBuilder("setRegister('ran', '1'); var x = 1");
		for (int i = 0; i < 50_000; i++) code.append(" + 1");
		return code.append(";").toString();
	}

	private static long countOverflows(List<Throwable> reported) {
		return reported.stream().filter(e -> e instanceof StackOverflowError).count();
	}

	/**
	 * Même débordement à l'analyse (sauvegarde dans l'éditeur, via le démon) : erreur interne,
	 * pas d'exception. Analysée deux fois, l'IA n'est signalée qu'une fois en erreur serveur.
	 */
	@Test
	public void deepExpressionAnalyzeReturnsInternalError() throws Exception {
		// Chemin unique : l'id de l'IA (dérivé du chemin) sert au dédoublonnage, qui vit tout le processus
		AIFile file = newAIFile(deepExpression(), 1);
		var reported = captureReportedErrors(() -> runWithSmallStack(() -> {
			for (int i = 0; i < 2; i++) {
				var result = generator.analyzeAI(file, 0);
				Assert.assertFalse(result.success);
				Assert.assertEquals(leekscript.common.Error.INTERNAL_ERROR.ordinal(), result.informations.get(0).get(6).asInt());
			}
		}));
		Assert.assertEquals("signalé une fois par IA : " + reported, 1, countOverflows(reported));
	}

	/** Téléchargement de l'IA fusionnée (includes) : le débordement rend un message, il ne s'échappe plus. */
	@Test
	public void deepExpressionDownloadReturnsMessage() throws Exception {
		AIFile file = newAIFile(deepExpression(), 1);
		var merged = new AtomicReference<String>();
		runWithSmallStack(() -> merged.set(generator.downloadAI(file)));
		Assert.assertEquals(Generator.compilerStackOverflowMessage(file), merged.get());
	}

	/**
	 * Deux entités partagent l'IA fautive : elles seules sont invalides, le joueur lit pourquoi,
	 * et l'erreur serveur n'est signalée qu'une fois (pas une par entité ni par combat).
	 */
	@Test
	public void deepExpressionInvalidatesOnlyItsAI() throws Exception {
		AIFile deep = newAIFile(deepExpression(), leek1.getId());
		attachAI(leek1, deep);
		attachAI(leek3, deep);
		attachAI(leek2, "setRegister('ran', '1');");

		var reported = captureReportedErrors(() -> runWithSmallStack(this::runFight));

		Assert.assertNull("l'IA qui ne compile pas n'a pas joué", leek1.getRegister("ran"));
		Assert.assertNull("l'IA qui ne compile pas n'a pas joué (2e entité)", leek3.getRegister("ran"));
		Assert.assertEquals("l'autre IA a joué son combat", "1", leek2.getRegister("ran"));
		Assert.assertEquals("signalé une fois pour l'IA, pas par entité : " + reported, 1, countOverflows(reported));
		Assert.assertTrue("le joueur lit la cause", farmerLog.toJSON().toString().contains("expression too deeply nested"));
	}
}
