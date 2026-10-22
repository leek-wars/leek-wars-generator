package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Le NPE interne a GraalVM leve par le nettoyage d'un contexte collecte, en tete de
 * {@code Context.Builder.build()}, est reconnu (et le contexte reconstruit) ; un autre NPE remonte.
 *
 * <p>Test pur (pas d'isolate requis) : il garde la CLASSIFICATION sur la pile observee en prod.
 */
public class TestPolyglotContextCleanup {

	private static NullPointerException npe(StackTraceElement... frames) {
		NullPointerException e = new NullPointerException();
		e.setStackTrace(frames);
		return e;
	}

	private static StackTraceElement frame(String className, String method) {
		return new StackTraceElement(className, method, null, -1);
	}

	@Test
	public void cleanupOfCollectedContextIsRecognised() {
		Assert.assertTrue(PolyglotSandbox.isCollectedContextCleanupFailure(npe(
				frame("com.oracle.truffle.polyglot.isolate.ForeignContextDispatch", "resolveForeignDelegate"),
				frame("com.oracle.truffle.polyglot.isolate.ForeignContextDispatch", "onContextCollected"),
				frame("org.graalvm.polyglot.Engine$ContextReference", "clean"),
				frame("org.graalvm.polyglot.Engine$CleanableReference", "processReferenceQueue"),
				frame("org.graalvm.polyglot.Engine$APIAccessImpl", "processReferenceQueue"),
				frame("com.oracle.truffle.polyglot.isolate.ForeignEngineDispatch", "createContext"),
				frame("org.graalvm.polyglot.Context$Builder", "build"),
				frame("com.leekwars.generator.polyglot.PolyglotSandbox", "createContext"))));
	}

	@Test
	public void otherNullPointerExceptionIsNotRecognised() {
		Assert.assertFalse(PolyglotSandbox.isCollectedContextCleanupFailure(npe(
				frame("org.graalvm.polyglot.Context$Builder", "build"),
				frame("com.leekwars.generator.polyglot.PolyglotSandbox", "createContext"))));
		Assert.assertFalse(PolyglotSandbox.isCollectedContextCleanupFailure(npe(
				frame("com.leekwars.generator.Other", "processReferenceQueue"))));
	}
}
