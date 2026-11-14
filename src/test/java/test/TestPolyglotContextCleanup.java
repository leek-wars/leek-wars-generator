package test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.graalvm.nativebridge.MarshalledException;
import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Le NPE interne a GraalVM leve par le nettoyage d'un contexte collecte, en fin de
 * {@code Context.Builder.build()}, est reconnu (la file videe et le contexte reconstruit) ; une autre
 * exception remonte. Sous isolate, ce NPE arrive en {@link MarshalledException} (pas en
 * NullPointerException) : c'est la forme de la prod, celle que cleanupFailure() reproduit.
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

	private static final StackTraceElement[] CLEANUP_FRAMES = {
			frame("com.oracle.truffle.polyglot.isolate.ForeignContextDispatch", "resolveForeignDelegate"),
			frame("com.oracle.truffle.polyglot.isolate.ForeignContextDispatchGen$HSToNativeStartPoint", "onContextCollectedImpl0"),
			frame("com.oracle.truffle.polyglot.isolate.ForeignContextDispatch", "onContextCollected"),
			frame("org.graalvm.polyglot.Engine$ContextReference", "clean"),
			frame("org.graalvm.polyglot.Engine$CleanableReference", "processReferenceQueue"),
			frame("org.graalvm.polyglot.Engine$APIAccessImpl", "processReferenceQueue"),
			frame("com.oracle.truffle.polyglot.isolate.ForeignEngineDispatch", "createContext"),
			frame("org.graalvm.polyglot.Context$Builder", "build"),
			frame("com.leekwars.generator.polyglot.PolyglotSandbox", "createContext") };

	/** Le NPE de l'isolate tel que la prod le recoit : une MarshalledException qui s'affiche en NullPointerException. */
	private static RuntimeException cleanupFailure() {
		return new MarshalledException(NullPointerException.class.getName(), null, CLEANUP_FRAMES);
	}

	/** Un autre echec du nettoyage (isolate plein), lui aussi venu de l'isolate. */
	private static RuntimeException otherCleanupFailure() {
		return new MarshalledException(IllegalStateException.class.getName(), "isolate plein", CLEANUP_FRAMES);
	}

	/** Leve {@code failure} aux {@code failures} premiers appels, puis rend "contexte". */
	private static Supplier<String> failingBuild(int failures, RuntimeException failure, AtomicInteger calls) {
		return () -> {
			if (calls.incrementAndGet() <= failures) throw failure;
			return "contexte";
		};
	}

	@Test
	public void cleanupOfCollectedContextIsRecognised() {
		RuntimeException failure = cleanupFailure();
		Assert.assertFalse("la prod ne recoit pas un NullPointerException", failure instanceof NullPointerException);
		Assert.assertEquals("java.lang.NullPointerException", failure.toString());
		Assert.assertTrue(PolyglotSandbox.isCollectedContextCleanupFailure(failure));
		Assert.assertTrue(PolyglotSandbox.isCollectedContextCleanupFailure(npe(CLEANUP_FRAMES)));
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
		Assert.assertEquals("contexte", PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(12, cleanupFailure(), calls), "js"));
		Assert.assertEquals(13, calls.get());
	}

	@Test
	public void queueIsDrainedBeforeEachBuild() {
		// Plus de cent references fautives : la vidange les saute toutes avant que build() ne les rencontre.
		AtomicInteger calls = new AtomicInteger();
		AtomicInteger drains = new AtomicInteger();
		Assert.assertEquals("contexte", PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(1, cleanupFailure(), calls), () -> {
			drains.incrementAndGet();
			return 150;
		}, "js"));
		Assert.assertEquals(2, calls.get());
		Assert.assertEquals(2, drains.get());
	}

	/** La vidange directe passe par une methode interne de GraalVM : une montee de version qui la retire doit se voir ici. */
	@Test
	public void graalVmReferenceQueueDrainIsAvailable() {
		Assert.assertTrue(PolyglotSandbox.canDrainCollectedContexts());
		Assert.assertEquals(0, PolyglotSandbox.drainCollectedContexts());
	}

	@Test
	public void otherNullPointerExceptionIsNotRetried() {
		AtomicInteger calls = new AtomicInteger();
		NullPointerException other = npe(frame("com.leekwars.generator.polyglot.PolyglotSandbox", "createContext"));
		try {
			PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(1, other, calls), "js");
			Assert.fail("le NPE devait remonter");
		} catch (NullPointerException e) {
			Assert.assertSame(other, e);
		}
		Assert.assertEquals(1, calls.get());
	}

	@Test
	public void retriesAreBounded() {
		RuntimeException failure = cleanupFailure();
		try {
			PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(Integer.MAX_VALUE, failure, new AtomicInteger()), "js");
			Assert.fail("l'echec devait remonter");
		} catch (MarshalledException e) {
			Assert.assertSame(failure, e);
		}
	}

	/** Leve {@code failures[i]} au i-eme appel, puis rend la main (file videe). */
	private static Runnable failingQueue(AtomicInteger calls, RuntimeException... failures) {
		return () -> {
			int call = calls.getAndIncrement();
			if (call < failures.length) throw failures[call];
		};
	}

	@Test
	public void drainSkipsEveryCleanupFailure() {
		// Le NPE connu, puis un echec d'un autre genre (isolate plein) : tous deux saute, la vidange continue.
		AtomicInteger calls = new AtomicInteger();
		RuntimeException other = otherCleanupFailure();
		Assert.assertEquals(3, PolyglotSandbox.drainCollectedContexts(failingQueue(calls, cleanupFailure(), other, cleanupFailure())));
		Assert.assertEquals(4, calls.get());
	}

	@Test
	public void drainRethrowsWhatIsNotACleanup() {
		IllegalStateException other = new IllegalStateException("pas un nettoyage");
		try {
			PolyglotSandbox.drainCollectedContexts(failingQueue(new AtomicInteger(), other));
			Assert.fail("l'exception devait remonter");
		} catch (IllegalStateException e) {
			Assert.assertSame(other, e);
		}
	}

	@Test
	public void buildDoesNotRetryOtherCleanupFailures() {
		// Reconstruire abandonne un contexte : on ne le fait pas pour un isolate plein.
		AtomicInteger calls = new AtomicInteger();
		RuntimeException other = otherCleanupFailure();
		try {
			PolyglotSandbox.buildPastCollectedContextCleanup(failingBuild(1, other, calls), () -> 0, "js");
			Assert.fail("l'echec devait remonter");
		} catch (MarshalledException e) {
			Assert.assertSame(other, e);
		}
		Assert.assertEquals(1, calls.get());
	}
}
