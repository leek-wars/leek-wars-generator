package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;
import com.leekwars.generator.state.Entity;

import leekscript.common.Error;
import leekscript.runner.LeekRunException;

/**
 * Etape 1 du support polyglot : une IA JavaScript s'execute de bout en bout dans un vrai
 * etat de combat, appelle l'API de combat (via PolyglotAPIBridge) et lit l'etat reel.
 *
 * Prouve la chaine complete : source JS -&gt; Context.eval -&gt; binding ProxyExecutable
 * -&gt; methode statique de combat (EntityClass/FightClass) avec EntityAI injecte -&gt;
 * valeur de retour marshallee cote JS -&gt; valeur de retour de runIA cote Java.
 */
public class TestPolyglotFight extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "JsLeek");
		leek2 = defaultLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** Construit une IA JS prete a tourner pour leek1 dans le combat initialise. */
	private PolyglotEntityAI buildAI(PolyglotSandbox sandbox, String source) {
		PolyglotEntityAI ai = new PolyglotEntityAI("js", source, sandbox);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	@Test
	public void jsReadsOwnLife() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sandbox, "getLife();");
			Object result = ai.runIA();
			Assert.assertEquals((long) leek1.getLife(), ((Number) result).longValue());
		}
	}

	@Test
	public void jsCanCallFunctionWithArgument() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// getNearestEnemy() -> fid de leek2 ; getLife(fid) -> pv de leek2.
			EntityAI ai = buildAI(sandbox, "getLife(getNearestEnemy());");
			Object result = ai.runIA();
			Assert.assertEquals((long) leek2.getLife(), ((Number) result).longValue());
		}
	}

	@Test
	public void jsCanUseArithmeticOverApi() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sandbox, "getLife() + getStrength();");
			Object result = ai.runIA();
			long expected = (long) leek1.getLife() + (long) leek1.getStat(Entity.STAT_STRENGTH);
			Assert.assertEquals(expected, ((Number) result).longValue());
		}
	}

	@Test
	public void fightConstantsAreExposed() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// Valeurs scalaires : on verifie que les constantes sont definies ET justes.
			Assert.assertEquals(1L, ((Number) buildAI(sandbox, "ENTITY_LEEK;").runIA()).longValue());
			Assert.assertEquals(0L, ((Number) buildAI(sandbox, "CELL_EMPTY;").runIA()).longValue());
			Assert.assertEquals(2L, ((Number) buildAI(sandbox, "CELL_OBSTACLE;").runIA()).longValue());
		}
	}

	@Test(timeout = 30_000)
	public void infiniteLoopHitsStatementLimit() throws Exception {
		initFightOnly();
		// Boucle PURE guest (ne charge aucune op) : bornee par le statement limit GraalVM.
		try (PolyglotSandbox sandbox = new PolyglotSandbox(100_000, "js")) {
			EntityAI ai = buildAI(sandbox, "var i = 0; while (true) { i++; } i;");
			try {
				ai.runIA();
				Assert.fail("la boucle infinie aurait du etre interrompue par le statement limit");
			} catch (LeekRunException e) {
				Assert.assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
			}
		}
	}

	@Test(timeout = 30_000)
	public void hostWorkIsThrottledByOps() throws Exception {
		initFightOnly();
		// Vecteur DoS de la revue : une boucle guest legere appelant une fonction de combat
		// couteuse (getPathLength -> A*). Le statement limit ne compterait PAS ce travail hote ;
		// c'est le comptage d'ops (laisse actif) qui doit l'interrompre via TOO_MUCH_OPERATIONS.
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sandbox,
				"var c1 = getCell();"
				+ "var c2 = getCell(getNearestEnemy());"
				+ "for (var i = 0; i < 1000000; i++) { getPathLength(c1, c2); }"
				+ "0;");
			try {
				ai.runIA();
				Assert.fail("le travail hote en boucle aurait du etre interrompu par le comptage d'ops");
			} catch (LeekRunException e) {
				Assert.assertEquals(Error.TOO_MUCH_OPERATIONS, e.getError());
			}
		}
	}

	@Test
	public void disposeReleasesContextWithoutError() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = buildAI(sandbox, "getLife();");
			ai.runIA();
			ai.dispose();
			// Apres dispose, un nouvel appel reconstruit un contexte neuf (pas d'erreur).
			Object result = ai.runIA();
			Assert.assertEquals((long) leek1.getLife(), ((Number) result).longValue());
		}
	}
}
