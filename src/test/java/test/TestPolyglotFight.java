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
import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
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
			EntityAI ai = buildAI(sandbox, "Fight.me.life;");
			Object result = ai.runIA();
			Assert.assertEquals((long) leek1.getLife(), ((Number) result).longValue());
		}
	}

	@Test
	public void jsCanCallFunctionWithArgument() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// getNearestEnemy() -> fid de leek2 ; getLife(fid) -> pv de leek2.
			EntityAI ai = buildAI(sandbox, "Fight.getNearestEnemy().life;");
			Object result = ai.runIA();
			Assert.assertEquals((long) leek2.getLife(), ((Number) result).longValue());
		}
	}

	@Test
	public void jsCanUseArithmeticOverApi() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sandbox, "Fight.me.life + Fight.me.strength;");
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
			Assert.assertEquals(1L, ((Number) buildAI(sandbox, "Entity.Type.LEEK;").runIA()).longValue());
			Assert.assertEquals(0L, ((Number) buildAI(sandbox, "Cell.Type.EMPTY;").runIA()).longValue());
			Assert.assertEquals(2L, ((Number) buildAI(sandbox, "Cell.Type.OBSTACLE;").runIA()).longValue());
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
				"var c1 = Fight.me.cell;"
				+ "var c2 = Fight.getNearestEnemy().cell;"
				+ "for (var i = 0; i < 1000000; i++) { c1.pathLength(c2); }"
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
	public void jsReceivesArrayFromApi() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// getEnemies() -> ArrayLeekValue marshalle en ProxyArray cote JS (length + indexation).
			Assert.assertEquals(1L, ((Number) buildAI(sandbox, "Fight.getEnemies().length;").runIA()).longValue());
			Assert.assertEquals((long) leek2.getFId(),
				((Number) buildAI(sandbox, "Fight.getEnemies()[0].id;").runIA()).longValue());
			// Iteration cote guest sur le tableau renvoye par l'API.
			Object sum = buildAI(sandbox,
				"var e = Fight.getEnemies(); var s = 0; for (var i = 0; i < e.length; i++) s += e[i].id; s;").runIA();
			Assert.assertEquals((long) leek2.getFId(), ((Number) sum).longValue());
		}
	}

	@Test
	public void jsPassesArrayToApi() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// Un tableau JS passe a une fonction hote attendant un GenericArrayLeekValue, via la vue
			// objet Message (raw = [auteur, type, params] ; type lit l'index 1, params l'index 2).
			Assert.assertEquals(7L, ((Number) buildAI(sandbox, "new Message([5, 7, 99]).type;").runIA()).longValue());
			Assert.assertEquals(99L, ((Number) buildAI(sandbox, "new Message([5, 7, 99]).params;").runIA()).longValue());
		}
	}

	@Test
	public void jsMapRoundTrip() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// setRegister (args string) puis getRegisters() -> MapLeekValue marshalle en ProxyObject.
			Object value = buildAI(sandbox, "Registers.set('foo', 'bar'); Registers.all()['foo'];").runIA();
			Assert.assertEquals("bar", value);
		}
	}

	@Test(timeout = 30_000)
	public void cyclicGuestArrayIsBounded() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// Tableau auto-reference renvoye par l'IA : le marshalling doit s'arreter proprement
			// (garde de profondeur/budget), pas en StackOverflowError non controle ni en boucle.
			// La valeur de retour de runIA etant IGNOREE en combat, son echec de marshalling est
			// avale et runIA rend null au lieu de lever (cf PolyglotEntityAI.runIA).
			EntityAI ai = buildAI(sandbox, "var a = []; a[0] = a; a;");
			Assert.assertNull("le marshalling d'un tableau cyclique doit etre interrompu (retour null)",
				ai.runIA());
		}
	}

	@Test
	public void classStaticsPersistAcrossTurns() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// Modele LeekScript : source evalue 1x (definit Mem + turn()), turn() rejoue chaque tour.
			// La static de classe persiste entre les tours, les locales de turn() sont oubliees.
			PolyglotEntityAI ai = new PolyglotEntityAI("js",
				"class Mem { static n = 0; } function turn() { Mem.n++; return Mem.n; }", sandbox);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(2L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(3L, ((Number) ai.runIA()).longValue());
		}
	}

	@Test
	public void topLevelRunsOnceWhenTurnDefined() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// Le code top-level (setup) ne s'execute qu'une fois : le compteur reste a 1.
			PolyglotEntityAI ai = new PolyglotEntityAI("js",
				"globalThis.setup = (globalThis.setup || 0) + 1; function turn() { return globalThis.setup; }", sandbox);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
		}
	}

	@Test
	public void jsAiRunsInFullMultiTurnFight() throws Exception {
		// IA JS attachee via un fichier .js -> EntityAI.build() dispatche vers le polyglot.
		// turn() incremente une static et l'ecrit dans un registre ; apres le combat le registre
		// doit valoir > 1, ce qui prouve l'execution multi-tours ET la persistance de la static.
		attachJsAI(leek1, "class C { static n = 0; } function turn() { C.n++; Registers.set('turns', '' + C.n); }");
		attachAI(leek2, ""); // adversaire LeekScript inerte
		runFight();

		Assert.assertTrue("leek1 doit utiliser une IA polyglot", leek1.getAI() instanceof PolyglotEntityAI);
		String registers = registerStore.get(leek1.getId());
		Assert.assertNotNull("les registres de leek1 doivent etre persistes", registers);
		int turns = extractTurns(registers);
		Assert.assertTrue("turn() doit s'etre execute sur plusieurs tours (static persistante), recu: " + turns, turns > 1);
	}

	/** Attache une IA JS a un poireau via un AIFile dont le chemin se termine par .js. */
	private void attachJsAI(Leek leek, String code) {
		AIFile file = new AIFile("polyglot_test_" + System.nanoTime() + ".js", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	/** Extrait la valeur entiere du registre "turns" du JSON de registres. */
	private static int extractTurns(String registersJson) {
		var m = java.util.regex.Pattern.compile("\"turns\"\\s*:\\s*\"?(\\d+)\"?").matcher(registersJson);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	@Test
	public void mathRandomIsDeterministicForSeed() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// Deux IA identiques avec la meme seed -> meme sequence Math.random (replays reproductibles).
			PolyglotEntityAI a = buildAI(sandbox, "function turn(){ return Math.floor(Math.random() * 1000000); }");
			PolyglotEntityAI b = buildAI(sandbox, "function turn(){ return Math.floor(Math.random() * 1000000); }");
			a.getRandom().seed(12345);
			b.getRandom().seed(12345);
			Assert.assertEquals(((Number) a.runIA()).longValue(), ((Number) b.runIA()).longValue());
			Assert.assertEquals(((Number) a.runIA()).longValue(), ((Number) b.runIA()).longValue());
		}
	}

	@Test
	public void twoJsAisMoveTowardEachOtherInRealFight() throws Exception {
		// Demo : deux IA JS qui se rapprochent l'une de l'autre dans un VRAI combat
		// (vraie boucle de tours, pathfinding, consommation de MP). On affiche le deroule.
		String moverAi =
			"function turn() {"
			+ "  var e = Fight.getNearestEnemy();"
			+ "  var before = Fight.me.cell.distance(e);"
			+ "  if (Registers.get('startDist') == null) Registers.set('startDist', '' + before);"
			+ "  Fight.me.moveToward(e);"
			+ "  Registers.set('endDist', '' + Fight.me.cell.distance(e));"
			+ "  Registers.set('cell', '' + Fight.me.cell.id);"
			+ "}";
		attachJsAI(leek1, moverAi);
		attachJsAI(leek2, moverAi);
		runFight();

		int start1 = Integer.parseInt(leek1.getRegister("startDist"));
		int end1 = Integer.parseInt(leek1.getRegister("endDist"));
		int start2 = Integer.parseInt(leek2.getRegister("startDist"));
		int end2 = Integer.parseInt(leek2.getRegister("endDist"));

		System.out.println("[demo] === Combat avec 2 IA JavaScript ===");
		System.out.println("[demo] IA (les deux poireaux) :\n" + moverAi);
		System.out.println("[demo] leek1 : distance ennemi " + start1 + " -> " + end1 + " (cellule finale " + leek1.getRegister("cell") + ")");
		System.out.println("[demo] leek2 : distance ennemi " + start2 + " -> " + end2 + " (cellule finale " + leek2.getRegister("cell") + ")");
		System.out.println("[demo] tours joues : " + fight.getState().getOrder().getTurn());

		Assert.assertTrue("leek1 (JS) doit s'etre rapproche : " + start1 + " -> " + end1, end1 < start1);
		Assert.assertTrue("leek2 (JS) doit s'etre rapproche : " + start2 + " -> " + end2, end2 < start2);
		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertTrue(leek2.getAI() instanceof PolyglotEntityAI);
	}

	@Test
	public void flatJsAiWithLetRunsEveryTurn() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// IA plate (sans turn()) utilisant let : doit tourner CHAQUE tour sans "already declared".
			PolyglotEntityAI ai = buildAI(sandbox, "let me = Fight.me.life; Registers.set('seen', '' + me);");
			ai.runIA();
			ai.runIA(); // avant le fix : SyntaxError "Variable me has already been declared"
			ai.runIA();
			Assert.assertEquals("" + leek1.getLife(), leek1.getRegister("seen"));
		}
	}

	@Test
	public void flatJsAiUsingReservedLikeNameRunsEveryTurn() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			// Regression : une IA plate qui declare un let nomme comme un eventuel wrapper interne
			// ne doit pas entrer en collision (on rejoue via une IIFE anonyme, sans nom interne).
			PolyglotEntityAI ai = buildAI(sandbox, "let __lw_turn = Fight.me.life; Registers.set('v', '' + __lw_turn);");
			ai.runIA();
			ai.runIA();
			ai.runIA();
			Assert.assertEquals("" + leek1.getLife(), leek1.getRegister("v"));
		}
	}

	@Test
	public void invalidJsSyntaxMakesAiInert() throws Exception {
		// Source JS invalide -> rejete au build (parse), comme une IA LeekScript qui ne compile pas.
		attachJsAI(leek1, "function turn() { return 1"); // accolade manquante
		attachAI(leek2, "");
		runFight();
		Assert.assertFalse("une IA JS au source invalide ne doit pas devenir polyglot",
			leek1.getAI() instanceof PolyglotEntityAI);
	}

	@Test
	public void disposeReleasesContextWithoutError() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("js")) {
			PolyglotEntityAI ai = buildAI(sandbox, "Fight.me.life;");
			ai.runIA();
			ai.dispose();
			// Apres dispose, un nouvel appel reconstruit un contexte neuf (pas d'erreur).
			Object result = ai.runIA();
			Assert.assertEquals((long) leek1.getLife(), ((Number) result).longValue());
		}
	}
}
