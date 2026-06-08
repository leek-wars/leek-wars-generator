package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.MapLeekValue;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.runner.LeekRunException;

/**
 * Phase 2 : support des IA en Python (GraalPy) via le meme bridge que JavaScript.
 * Verifie l'execution de bout en bout dans un vrai combat, la persistance d'etat (attributs
 * de classe), le marshalling listes/tableaux, le determinisme et le bornage des boucles.
 */
public class TestPythonFight extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Py1");
		leek2 = defaultLeek(2, "Py2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private PolyglotEntityAI buildAI(PolyglotSandbox sandbox, String source) {
		PolyglotEntityAI ai = new PolyglotEntityAI("python", source, sandbox);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	private void attachPyAI(Leek leek, String code) {
		AIFile file = new AIFile("python_test_" + System.nanoTime() + ".py", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void pyReadsOwnLife() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("python")) {
			Object result = buildAI(sandbox, "getLife()").runIA();
			Assert.assertEquals((long) leek1.getLife(), ((Number) result).longValue());
		}
	}

	@Test
	public void pyClassAttributesPersistAcrossTurns() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("python")) {
			// Source evalue 1x (definit Mem + turn()), turn() rejouee chaque tour. L'attribut de
			// classe Mem.n persiste entre les tours (equivalent Python des statics JS).
			PolyglotEntityAI ai = buildAI(sandbox,
				"class Mem:\n    n = 0\ndef turn():\n    Mem.n += 1\n    return Mem.n\n");
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(2L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(3L, ((Number) ai.runIA()).longValue());
		}
	}

	@Test
	public void pyReceivesArrayFromApi() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("python")) {
			Assert.assertEquals(1L, ((Number) buildAI(sandbox, "len(getEnemies())").runIA()).longValue());
			Assert.assertEquals((long) leek2.getFId(),
				((Number) buildAI(sandbox, "getEnemies()[0]").runIA()).longValue());
		}
	}

	@Test
	public void pyPassesListToApi() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("python")) {
			// Une liste Python passee a une fonction attendant un GenericArrayLeekValue.
			Assert.assertEquals(7L, ((Number) buildAI(sandbox, "getMessageType([5, 7, 99])").runIA()).longValue());
		}
	}

	@Test
	public void pyRandomIsDeterministicForSeed() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("python")) {
			String src = "def turn():\n    import random\n    return random.randint(0, 1000000)\n";
			PolyglotEntityAI a = buildAI(sandbox, src);
			PolyglotEntityAI b = buildAI(sandbox, src);
			PolyglotEntityAI c = buildAI(sandbox, src);
			a.getRandom().seed(777);
			b.getRandom().seed(777);
			c.getRandom().seed(999); // graine differente
			long ra = ((Number) a.runIA()).longValue();
			long rb = ((Number) b.runIA()).longValue();
			long rc = ((Number) c.runIA()).longValue();
			Assert.assertEquals("meme graine -> meme tirage", ra, rb);
			// La graine doit reellement deriver du RNG du combat (sinon bug: graine toujours 0).
			Assert.assertNotEquals("graine differente -> tirage different", ra, rc);
		}
	}

	@Test
	public void pyDictMarshalsKeysNotMethods() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("python")) {
			// Un dict Python -> MapLeekValue avec ses VRAIES cles/valeurs (via l'API hashEntries),
			// pas ses methodes (pop, keys, ...) qui sont exposees via l'API members.
			Object result = buildAI(sandbox, "{'a': 1, 'b': 2}").runIA();
			Assert.assertTrue("dict Python -> MapLeekValue, recu: "
				+ (result == null ? "null" : result.getClass().getSimpleName()), result instanceof MapLeekValue);
			MapLeekValue map = (MapLeekValue) result;
			Assert.assertEquals(1L, ((Number) map.get("a")).longValue());
			Assert.assertEquals(2L, ((Number) map.get("b")).longValue());
		}
	}

	@Test
	public void pySetMarshalsToArray() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox("python")) {
			// Un set Python (iterable, ni indexe ni hash) -> tableau LeekScript via l'iterateur.
			Object result = buildAI(sandbox, "{10, 20, 30}").runIA();
			Assert.assertTrue("set Python -> tableau, recu: "
				+ (result == null ? "null" : result.getClass().getSimpleName()), result instanceof GenericArrayLeekValue);
			Assert.assertEquals(3, ((GenericArrayLeekValue) result).size());
		}
	}

	@Test
	public void pyAiRunsInFullMultiTurnFight() throws Exception {
		attachPyAI(leek1, "class C:\n    n = 0\ndef turn():\n    C.n += 1\n    setRegister('turns', str(C.n))\n");
		attachAI(leek2, ""); // adversaire LeekScript inerte
		runFight();
		Assert.assertTrue("leek1 doit utiliser une IA polyglot", leek1.getAI() instanceof PolyglotEntityAI);
		String registers = registerStore.get(leek1.getId());
		Assert.assertNotNull("registres de leek1 persistes", registers);
		var m = java.util.regex.Pattern.compile("\"turns\"\\s*:\\s*\"?(\\d+)\"?").matcher(registers);
		Assert.assertTrue(m.find());
		Assert.assertTrue("turn() Python execute sur plusieurs tours", Integer.parseInt(m.group(1)) > 1);
	}

	@Test
	public void invalidPythonSyntaxMakesAiInert() throws Exception {
		attachPyAI(leek1, "def turn(:\n    pass"); // syntaxe invalide
		attachAI(leek2, "");
		runFight();
		Assert.assertFalse("une IA Python au source invalide ne doit pas devenir polyglot",
			leek1.getAI() instanceof PolyglotEntityAI);
	}

	@Test
	public void twoPythonAisMoveTowardEachOtherInRealFight() throws Exception {
		// Demo : deux IA Python qui se rapprochent dans un VRAI combat (boucle de tours, pathfinding).
		String moverAi =
			"def turn():\n"
			+ "    e = getNearestEnemy()\n"
			+ "    if getRegister('startDist') is None:\n"
			+ "        setRegister('startDist', str(getCellDistance(getCell(), getCell(e))))\n"
			+ "    moveToward(e)\n"
			+ "    setRegister('endDist', str(getCellDistance(getCell(), getCell(e))))\n";
		attachPyAI(leek1, moverAi);
		attachPyAI(leek2, moverAi);
		runFight();

		int start1 = Integer.parseInt(leek1.getRegister("startDist"));
		int end1 = Integer.parseInt(leek1.getRegister("endDist"));
		int start2 = Integer.parseInt(leek2.getRegister("startDist"));
		int end2 = Integer.parseInt(leek2.getRegister("endDist"));
		System.out.println("[demo] === Combat avec 2 IA Python ===");
		System.out.println("[demo] IA (les deux poireaux) :\n" + moverAi);
		System.out.println("[demo] leek1 : distance ennemi " + start1 + " -> " + end1);
		System.out.println("[demo] leek2 : distance ennemi " + start2 + " -> " + end2);
		System.out.println("[demo] tours joues : " + fight.getState().getOrder().getTurn());

		Assert.assertTrue("leek1 (Python) doit s'etre rapproche : " + start1 + " -> " + end1, end1 < start1);
		Assert.assertTrue("leek2 (Python) doit s'etre rapproche : " + start2 + " -> " + end2, end2 < start2);
		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
	}

	@Test(timeout = 30_000)
	public void pyInfiniteLoopIsBounded() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sandbox = new PolyglotSandbox(1_000_000, "python")) {
			EntityAI ai = buildAI(sandbox, "while True:\n    pass\n");
			try {
				ai.runIA();
				Assert.fail("la boucle infinie aurait du etre interrompue");
			} catch (LeekRunException e) {
				// Sur le runtime interprete, l'interruption GraalPy remonte en erreur interne mappee
				// en AI_INTERRUPTED ; sur un runtime optimise ce serait TOO_MUCH_OPERATIONS. La boucle
				// est bornee dans les deux cas.
				Assert.assertTrue("erreur de bornage attendue, recue: " + e.getError(),
					e.getError() == Error.AI_INTERRUPTED || e.getError() == Error.TOO_MUCH_OPERATIONS);
			}
		}
	}
}
