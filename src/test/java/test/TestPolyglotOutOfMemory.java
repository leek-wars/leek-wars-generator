package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Classification du depassement de cap RAM guest ({@code sandbox.MaxHeapMemory}), sous ses trois
 * formes : cf {@link PolyglotSandbox#isMemoryExhaustion} et
 * {@link PolyglotSandbox#isGuestOutOfMemoryMessage} pour les incidents prod qui les ont revelees.
 *
 * <p>Test pur (pas d'isolate requis) : il garde la CLASSIFICATION, pas le declenchement (couvert
 * par TestPolyglotRamLimit).
 */
public class TestPolyglotOutOfMemory {

	/** Message exact remonte en prod par le CancelExecution echappe du setup de contexte. */
	private static final String PROD_MESSAGE =
			"Maximum heap memory limit of 33554432 bytes exceeded. Current memory at least 33554584 bytes.";

	@Test
	public void prodMessageIsRecognised() {
		Assert.assertTrue(PolyglotSandbox.isMemoryExhaustion(new RuntimeException(PROD_MESSAGE)));
	}

	/** La forme reellement observee : encapsulee par EntityAI ("Erreur importante dans l'IA ..."). */
	@Test
	public void wrappedCauseIsRecognised() {
		Throwable wrapped = new RuntimeException("Erreur importante dans l'IA 0",
				new IllegalStateException("boom", new RuntimeException(PROD_MESSAGE)));
		Assert.assertTrue(PolyglotSandbox.isMemoryExhaustion(wrapped));
	}

	/**
	 * Un {@link Error} JVM porteur du meme message compte aussi : c'est la forme du CancelExecution
	 * echappe (hors taxonomie PolyglotException/RuntimeException des catch de PolyglotEntityAI).
	 */
	@Test
	public void throwableErrorIsRecognised() {
		Assert.assertTrue(PolyglotSandbox.isMemoryExhaustion(new Error(PROD_MESSAGE)));
	}

	/**
	 * L'autre limite ne doit PAS etre requalifiee en RAM : elle reste TOO_MUCH_OPERATIONS, sinon on
	 * couperait l'IA pour tout le combat (traitement OUT_OF_MEMORY) au lieu de la laisser repartir au
	 * tour suivant.
	 */
	@Test
	public void statementLimitIsNotMemory() {
		Assert.assertFalse(PolyglotSandbox.isMemoryExhaustion(
				new RuntimeException("Maximum statement limit of 20000000 exceeded.")));
		Assert.assertFalse(PolyglotSandbox.isMemoryExhaustion(
				new RuntimeException("Maximum CPU time limit of 60000ms exceeded.")));
	}

	@Test
	public void unrelatedThrowablesAreNotMemory() {
		Assert.assertFalse(PolyglotSandbox.isMemoryExhaustion(new RuntimeException("NameError: name 'x' is not defined")));
		Assert.assertFalse(PolyglotSandbox.isMemoryExhaustion(new RuntimeException((String) null)));
		Assert.assertFalse(PolyglotSandbox.isMemoryExhaustion(new StackOverflowError()));
	}

	/** REGRESSION PROD #11812034 : la TROISIEME forme, un MemoryError guest sur le prelude Python. */
	@Test
	public void guestMemoryErrorIsRecognised() {
		Assert.assertTrue(PolyglotSandbox.isGuestOutOfMemoryMessage("MemoryError"));
		Assert.assertTrue(PolyglotSandbox.isGuestOutOfMemoryMessage("MemoryError: MemoryError"));
	}

	/** Le tri porte sur le TYPE leve (debut du message), pas sur un message de joueur qui le cite. */
	@Test
	public void guestMessageMentioningMemoryErrorIsNotMemory() {
		Assert.assertFalse(PolyglotSandbox.isGuestOutOfMemoryMessage("ValueError: MemoryError"));
		Assert.assertFalse(PolyglotSandbox.isGuestOutOfMemoryMessage("TypeError: bad operand"));
		Assert.assertFalse(PolyglotSandbox.isGuestOutOfMemoryMessage(null));
	}

	/** Chaine de causes cyclique : le parcours doit terminer plutot que boucler. */
	@Test
	public void selfReferencingCauseTerminates() {
		Throwable self = new RuntimeException("nope") {
			@Override
			public synchronized Throwable getCause() {
				return this;
			}
		};
		Assert.assertFalse(PolyglotSandbox.isMemoryExhaustion(self));
	}
}
