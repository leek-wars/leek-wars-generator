package test;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import org.junit.Assert;
import org.junit.Test;

/**
 * Spike GraalPy : valide la faisabilite de Python AVANT d'integrer (la stdlib embarquee de
 * GraalPy peut entrer en tension avec notre sandbox verrouille allowIO(NONE)).
 *
 * On verifie : eval de base, detection d'une fonction turn(), import de la stdlib sous sandbox,
 * appel d'un ProxyExecutable (bridge), re-eval d'un source "plat" (collision ?), statement limit,
 * et le seeding du module random.
 */
public class TestPythonSpike {

	private static final String PY = "python";

	private static Context locked() {
		// Config alignee sur PolyglotSandbox (verrouillee).
		return Context.newBuilder(PY)
				.allowHostAccess(HostAccess.NONE)
				.allowAllAccess(false)
				.allowCreateThread(false)
				.allowNativeAccess(false)
				.build();
	}

	@Test
	public void evalRuns() {
		try (Context c = Context.create(PY)) {
			Assert.assertEquals(7, c.eval(PY, "1 + 2 * 3").asInt());
		}
	}

	@Test
	public void turnFunctionIsVisibleViaBindings() {
		try (Context c = Context.create(PY)) {
			c.eval(PY, "def turn():\n    return 42\n");
			Value fn = c.getBindings(PY).getMember("turn");
			System.out.println("[py] getMember(turn) callable=" + (fn != null && fn.canExecute()));
			Assert.assertNotNull(fn);
			Assert.assertTrue(fn.canExecute());
			Assert.assertEquals(42, fn.execute().asInt());
		}
	}

	@Test
	public void stdlibImportUnderLockedSandbox() {
		// Point critique : GraalPy lit sa stdlib depuis un FS virtuel embarque. Est-ce que ca
		// marche avec allowIO par defaut (NONE) ? On teste un import courant.
		try (Context c = locked()) {
			Value r = c.eval(PY, "import math\nmath.floor(3.7)");
			System.out.println("[py] import math sous sandbox verrouille OK -> " + r);
			Assert.assertEquals(3, r.asInt());
		} catch (Exception e) {
			System.out.println("[py] import math ECHOUE sous sandbox verrouille: " + e.getMessage());
			throw e;
		}
	}

	@Test
	public void proxyExecutableBridge() {
		final int[] received = { -1 };
		ProxyExecutable getLife = (Value... args) -> {
			received[0] = args.length > 0 ? args[0].asInt() : 0;
			return 100 + received[0];
		};
		try (Context c = locked()) {
			c.getBindings(PY).putMember("getLife", getLife);
			Value r = c.eval(PY, "getLife(7)");
			Assert.assertEquals(7, received[0]);
			Assert.assertEquals(107, r.asInt());
		}
	}

	@Test
	public void flatSourceReEvalNoCollision() {
		// En Python, re-executer un source "plat" (assignations top-level) dans le meme contexte
		// ne pose pas le probleme "already declared" de JS. On verifie.
		try (Context c = locked()) {
			String src = "x = 21\nx * 2";
			Assert.assertEquals(42, c.eval(PY, src).asInt());
			Assert.assertEquals(42, c.eval(PY, src).asInt()); // 2e fois : pas de collision attendue
		}
	}

	@Test
	public void randomCanBeSeeded() {
		// Determinisme : deux contextes avec random.seed(meme valeur) -> meme sequence.
		try (Context a = locked(); Context b = locked()) {
			String src = "import random\nrandom.seed(12345)\nrandom.randint(0, 1000000)";
			int ra = a.eval(PY, src).asInt();
			int rb = b.eval(PY, src).asInt();
			System.out.println("[py] random seedé a=" + ra + " b=" + rb);
			Assert.assertEquals(ra, rb);
		}
	}

	@Test(timeout = 30_000)
	public void statementLimitStopsInfiniteLoop() {
		// Chemin PROD : sous isolate, la limite passe par sandbox.MaxStatements
		// (PolyglotSandbox.createContext) — ResourceLimits.statementLimit d'un contexte plain
		// n'est PAS applique par l'image isolate (boucle infinie non bornee, verifie).
		// NB : le corps de boucle ne doit PAS etre vide — GraalPy n'emet AUCUN statement comptable
		// pour `while True: pass` (boucle optimisee, verifie : jamais interrompue par MaxStatements).
		// En prod ce cas est couvert par le watchdog wall-clock de PolyglotEntityAI
		// (cf TestPythonFight.pyInfiniteLoopIsBounded).
		try (com.leekwars.generator.polyglot.PolyglotSandbox sb =
				new com.leekwars.generator.polyglot.PolyglotSandbox(1_000_000, PY)) {
		Context c = sb.createContext(PY);
		try {
			c.eval(PY, "i = 0\nwhile True:\n    i += 1\n");
			Assert.fail("la boucle infinie aurait du etre interrompue");
		} catch (org.graalvm.polyglot.PolyglotException e) {
			// Sur le runtime interprete (JDK standard), l'interruption GraalPy par le statement limit
			// remonte en erreur INTERNE (IllegalMonitorStateException) plutot qu'en resource-exhausted
			// propre. La boucle est neanmoins bornee (interrompue) : c'est ce qu'on verifie ici.
			System.out.println("[py] boucle infinie interrompue: cancelled=" + e.isCancelled()
					+ " exhausted=" + e.isResourceExhausted() + " internal=" + e.isInternalError());
			Assert.assertTrue("la boucle doit etre interrompue (ressource ou erreur interne)",
					e.isCancelled() || e.isResourceExhausted() || e.isInternalError());
		} finally {
			c.close(true);
		}
		}
	}
}
