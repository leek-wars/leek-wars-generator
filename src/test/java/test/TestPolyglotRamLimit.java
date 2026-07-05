package test;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Limite de RAM PAR POIREAU (par contexte) via le mode isolate GraalVM + sandbox.MaxHeapMemory.
 *
 * On teste directement {@link PolyglotSandbox#createContext} (sans le harnais de combat complet) :
 *  - un poireau qui RETIENT plus que son cap est annule, les autres du meme sandbox survivent ;
 *  - le cap suit le RETENU, pas l'alloue (le churn ne penalise pas) -> honnete, comme le mRAM LeekScript.
 *
 * Necessite les artefacts isolate (js/python-isolate-*-community) + `--enable-native-access=ALL-UNNAMED`
 * sur la JVM de test. Ne tourne donc qu'en CI/environnement avec l'isolate (pas en interpreted stock).
 */
public class TestPolyglotRamLimit {

	// Retient ~ chunks * 0.4 Mo dans un global (RETENU, pas churn).
	private static final String JS_RETAIN = "globalThis.keep=[]; for (var i=0;i<%d;i++){ keep.push(new Array(50000).fill(i)); } keep.length;";
	private static final String PY_RETAIN = "k=[[i]*50000 for i in range(%d)]\nlen(k)";

	@Test
	public void jsGreedyBounded_othersSurvive() {
		greedyBoundedOthersSurvive("js", JS_RETAIN);
	}

	/** Un poireau qui explose son cap RAM est annule ; un autre du meme sandbox (meme isolate) survit. */
	private void greedyBoundedOthersSurvive(String lang, String retainTemplate) {
		try (PolyglotSandbox sb = new PolyglotSandbox(lang)) {
			// Poireau glouton : cap 32 Mo, retient ~800 Mo -> annule. (La verification du cap est
			// liee aux passages GC de l'isolate : une retention trop proche du cap peut passer
			// entre deux collections -> on vise LARGE pour un declenchement fiable.)
			Context greedy = sb.createContext(lang, null, 32_000_000L);
			try {
				greedy.eval(lang, String.format(retainTemplate, 2000));
				// Repli isolate processus EXTERNE (la lib in-process d'un AUTRE langage est deja chargee
				// dans cette JVM, cf PolyglotSandbox.engineFor) : sandbox.MaxHeapMemory n'y est pas
				// applique, le cap par-poireau n'est verifiable qu'en mode in-process -> test saute.
				Assume.assumeFalse("cap par-poireau non verifiable : " + lang + " en isolate externe dans cette JVM",
						sb.isExternalIsolate(lang));
				Assert.fail("le poireau glouton aurait du etre annule par MaxHeapMemory");
			} catch (PolyglotException e) {
				Assert.assertTrue("annulation ressource attendue", e.isResourceExhausted() || e.isCancelled());
			}
			// Un AUTRE poireau sur le meme sandbox (meme isolate) survit : isolation par-poireau.
			Context nice = sb.createContext(lang, null, 32_000_000L);
			Assert.assertEquals(42, nice.eval(lang, "40+2").asInt());
		}
	}

	@Test
	public void jsUnderCapSurvives() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			Context ctx = sb.createContext("js", null, 128_000_000L);
			Assert.assertEquals(20, ctx.eval("js", String.format(JS_RETAIN, 20)).asInt()); // ~8 Mo << cap
		}
	}

	@Test
	public void jsChurnNotPenalized() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Alloue ~400 Mo mais NE RETIENT RIEN -> ne doit PAS etre annule (footprint, pas churn).
			Context ctx = sb.createContext("js", null, 32_000_000L);
			long r = ctx.eval("js", "var s=0; for(var i=0;i<1000;i++){ var t=new Array(50000).fill(i); s+=t[0]; } s;").asLong();
			Assert.assertTrue(r >= 0);
		}
	}

	@Test
	public void pythonGreedyBounded_othersSurvive() {
		greedyBoundedOthersSurvive("python", PY_RETAIN);
	}
}
