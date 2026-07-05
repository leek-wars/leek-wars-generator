package test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import org.junit.Assert;
import org.junit.Test;

/**
 * Spike GraalVM Polyglot : valide la faisabilite avant de construire le bridge complet.
 *
 * Verifie sur ce poste (OpenJDK 25, runtime Truffle non optimise) :
 *   1. Que les deps polyglot:25.0.2 + js-community se resolvent et tournent sur JDK 25.
 *   2. Le cout du warmup Truffle (1er eval vs evals suivants) -> justifie le partage d'Engine.
 *   3. Qu'un ProxyExecutable (le mecanisme du futur PolyglotAPIBridge) fonctionne :
 *      le guest JS appelle une "fonction de combat" Java qui lit l'etat injecte et renvoie une valeur.
 *   4. Que ResourceLimits.statementLimit interrompt une boucle infinie (modele de sandbox).
 *   5. Que le modele de sandbox refuse VRAIMENT les acces host (prouve par contraste NONE vs ALL,
 *      pas par un simple "ca leve une exception").
 */
public class TestPolyglotSpike {

	private static final String JS = "js";

	@Test
	public void evalRunsOnJdk25() {
		try (Context context = Context.create(JS)) {
			Value result = context.eval(JS, "1 + 2 * 3");
			Assert.assertEquals(7, result.asInt());
		}
	}

	@Test
	public void warmupCostIsMeasured() {
		// Engine partage = warmup amorti entre contextes (modele "1 Engine par combat").
		// Le script calcule sum(0..999) = 499500 ; on asserte le resultat pour prouver que
		// les deux evals tournent reellement, et on logue le temps a titre informatif.
		final String script = "var s = 0; for (var i = 0; i < 1000; i++) s += i; s;";
		final int expected = 499500;

		try (Engine engine = Engine.newBuilder(JS).build()) {

			long firstStart = System.nanoTime();
			int first;
			try (Context c = Context.newBuilder(JS).engine(engine).build()) {
				first = c.eval(JS, script).asInt();
			}
			long firstMs = (System.nanoTime() - firstStart) / 1_000_000;

			long secondStart = System.nanoTime();
			int second;
			try (Context c = Context.newBuilder(JS).engine(engine).build()) {
				second = c.eval(JS, script).asInt();
			}
			long secondMs = (System.nanoTime() - secondStart) / 1_000_000;

			System.out.println("[spike] warmup 1er contexte = " + firstMs + " ms, 2e contexte = " + secondMs + " ms");
			Assert.assertEquals("le 1er eval doit calculer sum(0..999)", expected, first);
			Assert.assertEquals("le 2e eval doit calculer sum(0..999)", expected, second);
		}
	}

	@Test
	public void proxyExecutableBridgesJavaToJs() {
		// Coeur du bridge : une "fonction de combat" Java exposee au JS. Le bridge cote Java
		// prepend l'EntityAI (et donc l'etat du combat) ; on le simule ici par une closure
		// sur un faux etat. Le proxy lit cet etat et renvoie une valeur, recue cote JS.
		// Les assertions sont faites APRES l'eval (pas dans le lambda, qui est appele par Truffle).
		final Map<Integer, Integer> fakeFightState = Map.of(7, 250); // entityId -> pv, "porte" par l'EntityAI
		final int[] receivedArg = { -1 };

		ProxyExecutable getLife = (Value... args) -> {
			int entityId = args.length > 0 ? args[0].asInt() : 0;
			receivedArg[0] = entityId;
			return fakeFightState.getOrDefault(entityId, 100); // valeur issue de l'etat injecte
		};

		try (Context context = Context.newBuilder(JS)
				.allowHostAccess(HostAccess.NONE)
				.build()) {
			context.getBindings(JS).putMember("getLife", getLife);
			Value result = context.eval(JS, "getLife(7)");
			Assert.assertEquals("le proxy doit recevoir l'argument passe cote JS", 7, receivedArg[0]);
			Assert.assertEquals("le retour doit provenir de l'etat injecte cote Java", 250, result.asInt());
		}
	}

	@Test(timeout = 30_000)
	public void statementLimitStopsInfiniteLoop() {
		// Chemin PROD : sous isolate, la limite de statements passe par sandbox.MaxStatements
		// (PolyglotSandbox.createContext) — l'ancien ResourceLimits.statementLimit d'un contexte
		// plain n'est PAS applique par l'image isolate (boucle infinie non bornee, verifie).
		// LECON SPIKE (toujours vraie) : un contexte epuise passe "cancelled", son close() auto
		// RELANCE la PolyglotException -> fermeture defensive close(true).
		try (com.leekwars.generator.polyglot.PolyglotSandbox sb =
				new com.leekwars.generator.polyglot.PolyglotSandbox(100_000, JS)) {
			Context context = sb.createContext(JS);
			try {
				context.eval(JS, "var i = 0; while (true) { i++; }");
				Assert.fail("la boucle infinie aurait du etre interrompue par le statement limit");
			} catch (PolyglotException e) {
				System.out.println("[spike] boucle infinie interrompue: cancelled=" + e.isCancelled()
						+ " resourceExhausted=" + e.isResourceExhausted());
				Assert.assertTrue("doit etre une interruption de ressource", e.isCancelled() || e.isResourceExhausted());
			} finally {
				context.close(true); // cancelIfExecuting = true : ferme sans relancer
			}
		}
	}

	@Test
	public void hostClassLookupDeniedByDefault() {
		// Notre config bridge = allowAllAccess(false) (defaut). Sous cette config, le builtin
		// `Java` (host class lookup) n'est PAS expose au guest. On le PROUVE par contraste :
		// absent par defaut, present uniquement si allowAllAccess(true). Une simple "exception
		// levee" ne prouverait rien (Java.type() leve un ReferenceError guest dans les deux cas).
		try (Context restricted = Context.newBuilder(JS).build()) {
			Assert.assertEquals("Java ne doit pas etre expose par defaut",
					"undefined", restricted.eval(JS, "typeof Java").asString());
		}
		try (Context open = Context.newBuilder(JS).allowAllAccess(true).build()) {
			Assert.assertNotEquals("avec allowAllAccess, Java devient accessible (controle = allowAllAccess, pas HostAccess)",
					"undefined", open.eval(JS, "typeof Java").asString());
		}
	}

	@Test
	public void hostAccessNoneDeniesMemberAccess() {
		// HostAccess.NONE : un objet Java passe au guest est opaque (pas d'appel de methode).
		// PROUVE par contraste avec HostAccess.ALL ou le meme appel reussit.
		final AtomicInteger holder = new AtomicInteger(42);

		try (Context restricted = Context.newBuilder(JS).allowHostAccess(HostAccess.NONE).build()) {
			restricted.getBindings(JS).putMember("holder", holder);
			try {
				restricted.eval(JS, "holder.get()");
				Assert.fail("HostAccess.NONE aurait du interdire l'appel de methode host");
			} catch (PolyglotException e) {
				System.out.println("[spike] HostAccess.NONE refuse holder.get(): " + e.getMessage());
				Assert.assertTrue("doit etre une erreur cote guest", e.isGuestException());
			}
		}

		try (Context open = Context.newBuilder(JS).allowHostAccess(HostAccess.ALL).build()) {
			open.getBindings(JS).putMember("holder", holder);
			Assert.assertEquals("HostAccess.ALL doit autoriser l'appel de methode publique",
					42, open.eval(JS, "holder.get()").asInt());
		}
	}
}
