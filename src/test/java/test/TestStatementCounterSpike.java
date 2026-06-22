package test;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.StatementCounter;

/**
 * Spike : valide qu'un instrument Truffle peut compter les statements guest de facon DETERMINISTE
 * depuis l'embedding polyglot (decouverte de l'instrument, comptage, reproductibilite, surcout),
 * AVANT de brancher ce compteur dans PolyglotEntityAI.getOperations() (determinisme, point B).
 */
public class TestStatementCounterSpike {

	private static final String LOOP = "var s = 0; for (var i = 0; i < 100000; i++) { s += i; } s;";

	@Test
	public void instrumentIsDiscovered() {
		try (Engine engine = Engine.newBuilder("js").build()) {
			StatementCounter.Counter counter = engine.getInstruments()
					.get(StatementCounter.ID).lookup(StatementCounter.Counter.class);
			Assert.assertNotNull("instrument lw-statement-counter non decouvert", counter);
		}
	}

	@Test
	public void countsStatementsDeterministically() {
		try (Engine engine = Engine.newBuilder("js").build()) {
			StatementCounter.Counter counter = engine.getInstruments()
					.get(StatementCounter.ID).lookup(StatementCounter.Counter.class);
			try (Context ctx = Context.newBuilder("js").engine(engine).build()) {
				counter.reset();
				long t0 = System.nanoTime();
				ctx.eval("js", LOOP);
				long count1 = counter.get();
				long ms = (System.nanoTime() - t0) / 1_000_000;
				System.out.println("[spike] statements pour 100k iterations: " + count1 + " (" + ms + " ms)");
				Assert.assertTrue("aucun statement compte (" + count1 + ")", count1 > 100_000);

				// Determinisme : meme code, memes entrees -> MEME compte.
				counter.reset();
				ctx.eval("js", LOOP);
				Assert.assertEquals("comptage non deterministe", count1, counter.get());

				// Un nouveau contexte sur le meme engine compte pareil (reproductible).
				try (Context ctx2 = Context.newBuilder("js").engine(engine).build()) {
					counter.reset();
					ctx2.eval("js", LOOP);
					Assert.assertEquals("comptage non reproductible entre contextes", count1, counter.get());
				}
			}
		}
	}
}
