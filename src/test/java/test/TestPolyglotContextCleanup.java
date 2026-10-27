package test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Le NPE interne a GraalVM leve par le nettoyage d'un contexte collecte, en fin de
 * {@code Context.Builder.build()}, est reconnu (et le contexte reconstruit autant de fois qu'il le
 * faut) ; un autre NPE remonte.
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

	private static NullPointerException cleanupFailure() {
		return npe(
				frame("com.oracle.truffle.polyglot.isolate.ForeignContextDispatch", "resolveForeignDelegate"),
				frame("org.graalvm.polyglot.Engine$CleanableReference", "processReferenceQueue"),
				frame("org.graalvm.polyglot.Context$Builder", "build"));
	}

	/** Leve {@code failure} aux {@code failures} premiers appels, puis rend "contexte". */
	private static Supplier<String> failingBuild(int failures, Supplier<NullPointerException> failure, AtomicInteger calls) {
		return () -> {
			if (calls.incrementAndGet() <= failures) throw failure.get();
			return "contexte";
		};
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

	@Test
	public void severalCollectedContextsInTheQueueAreAllDrained() {
		// Un passage du GC peut laisser bien plus de trois references fautives dans la file.
		AtomicInteger calls = new AtomicInteger();
		Assert.assertEquals("contexte", PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(12, TestPolyglotContextCleanup::cleanupFailure, calls), "js"));
		Assert.assertEquals(13, calls.get());
	}

	@Test
	public void otherNullPointerExceptionIsNotRetried() {
		AtomicInteger calls = new AtomicInteger();
		NullPointerException other = npe(frame("com.leekwars.generator.polyglot.PolyglotSandbox", "createContext"));
		try {
			PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(1, () -> other, calls), "js");
			Assert.fail("le NPE devait remonter");
		} catch (NullPointerException e) {
			Assert.assertSame(other, e);
		}
		Assert.assertEquals(1, calls.get());
	}

	@Test
	public void retriesAreBounded() {
		AtomicInteger calls = new AtomicInteger();
		try {
			PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(Integer.MAX_VALUE, TestPolyglotContextCleanup::cleanupFailure, calls), "js");
			Assert.fail("le NPE devait remonter");
		} catch (NullPointerException e) {
			Assert.assertTrue(PolyglotSandbox.isCollectedContextCleanupFailure(e));
		}
		Assert.assertEquals(PolyglotSandbox.CONTEXT_BUILD_ATTEMPTS, calls.get());
	}
}
