package test;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Regression bas niveau du compteur de statements DETERMINISTE de l'image isolate JS CUSTOM
 * (repo graal-isolate) : l'instrument StatementCounter est compile DANS l'image native, active par
 * option a la creation de l'engine (cf PolyglotSandbox.engineFor) et lu par l'hote via les polyglot
 * bindings du contexte (le lookup de service hote ne traverse pas la frontiere isolate).
 *
 * Si ce test echoue sur "binding absent", l'image officielle (sans instrument) a probablement
 * remplace libs/js-isolate-resources-linux-amd64.jar -> les ops JS retombent en temps CPU (non
 * deterministe), cf TestPolyglotDeterminism pour l'effet de bout en bout.
 */
public class TestStatementCounterSpike {

	private static final String LOOP = "var s = 0; for (var i = 0; i < 100000; i++) { s += i; } s;";

	@Test
	public void counterBindingIsPublishedAndDeterministic() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			Context ctx = sb.createContext("js");
			Value counter = PolyglotSandbox.statementCounterBinding(ctx);
			Assert.assertNotNull("binding compteur absent (image isolate custom manquante ?)", counter);

			counter.execute(0); // reset
			ctx.eval("js", LOOP);
			long count1 = counter.execute().asLong();
			System.out.println("[spike] statements pour 100k iterations: " + count1);
			Assert.assertTrue("aucun statement compte (" + count1 + ")", count1 > 100_000);

			// Determinisme : meme code, memes entrees -> MEME compte.
			counter.execute(0);
			ctx.eval("js", LOOP);
			Assert.assertEquals("comptage non deterministe", count1, counter.execute().asLong());

			// Un nouveau contexte sur le meme sandbox (meme engine/isolate) compte pareil.
			Context ctx2 = sb.createContext("js");
			Value counter2 = PolyglotSandbox.statementCounterBinding(ctx2);
			Assert.assertNotNull(counter2);
			counter2.execute(0);
			ctx2.eval("js", LOOP);
			Assert.assertEquals("comptage non reproductible entre contextes", count1, counter2.execute().asLong());
		}
	}
}
