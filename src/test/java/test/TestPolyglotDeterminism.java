package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Determinisme du compteur d'operations polyglot (point B). Une IA de recherche borne son exploration
 * sur {@code getOperations() > seuil} ; sous le runtime polyglot ce compteur doit etre REPRODUCTIBLE
 * (compteur de statements guest, cf {@link com.leekwars.generator.polyglot.StatementCounter}) sinon
 * les replays divergent. On verifie qu'une boucle bornee par getOperations() fait le MEME nombre
 * d'iterations d'un run a l'autre (et entre deux sandboxes / engines independants).
 */
public class TestPolyglotDeterminism extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Det1");
		leek2 = defaultLeek(2, "Det2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	// IA : chaque tour, boucle TANT QUE getOperations() < seuil et renvoie le nombre d'iterations.
	// (via turn() : le retour est capte a chaque tour, contrairement a une IA plate dont seul le tour 1
	// renvoie sa derniere expression.)
	private static final String BOUNDED_LOOP =
		"function turn() { var n = 0; while (getOperations() < 300000) { n++; } return n; }";

	private long boundedLoopIterations(PolyglotSandbox sandbox) throws Exception {
		PolyglotEntityAI ai = new PolyglotEntityAI("js", BOUNDED_LOOP, sandbox);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ((Number) ai.runIA()).longValue();
	}

	@Test(timeout = 30_000)
	public void operationsAreDeterministicAcrossRuns() throws Exception {
		initFightOnly();
		long a, b, c;
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			a = boundedLoopIterations(sandbox);
		}
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			b = boundedLoopIterations(sandbox);
		}
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			c = boundedLoopIterations(sandbox);
		}
		System.out.println("[determinisme] iterations bornees par getOperations(): " + a + " / " + b + " / " + c);
		Assert.assertTrue("la garde getOperations() doit avoir borne la boucle (iterations > 0)", a > 0);
		Assert.assertEquals("getOperations() DOIT etre deterministe (run 1 vs 2)", a, b);
		Assert.assertEquals("getOperations() DOIT etre deterministe (run 1 vs 3)", a, c);
	}

	private long[] threeTurnSequence() throws Exception {
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = new PolyglotEntityAI("js", BOUNDED_LOOP, sandbox);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			return new long[] {
				((Number) ai.runIA()).longValue(),
				((Number) ai.runIA()).longValue(),
				((Number) ai.runIA()).longValue(),
			};
		}
	}

	@Test(timeout = 30_000)
	public void multiTurnSequenceIsReproducible() throws Exception {
		initFightOnly();
		// Le compteur est remis a zero chaque tour (sinon le tour 2 partirait au-dela du seuil et
		// ferait 0 iteration). On verifie que REJOUER la sequence multi-tours donne la MEME sequence
		// (= reproductibilite du combat) ; on n'exige PAS l'egalite tour-a-tour (une IA plate est
		// evaluee directement au tour 1 puis via IIFE ensuite -> overhead de statements legerement
		// different, mais reproductible).
		long[] run1 = threeTurnSequence();
		long[] run2 = threeTurnSequence();
		System.out.println("[determinisme] sequence run1 = " + java.util.Arrays.toString(run1)
			+ " / run2 = " + java.util.Arrays.toString(run2));
		for (long t : run1) {
			Assert.assertTrue("chaque tour doit executer la boucle (reset OK), recu: " + t, t > 1000);
		}
		Assert.assertArrayEquals("la sequence multi-tours doit etre reproductible (replay fiable)", run1, run2);
	}
}
