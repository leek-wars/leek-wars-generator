package com.leekwars.generator.polyglot;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import com.leekwars.generator.fight.entity.EntityAI;

import leekscript.common.Error;
import leekscript.compiler.LeekScript;
import leekscript.runner.LeekRunException;
import leekscript.runner.Session;

/**
 * IA executee via GraalVM Polyglot (JavaScript / Python) plutot que compilee en Java
 * depuis LeekScript. Branche parallele au pipeline LeekScript : meme surface
 * {@link EntityAI} (le moteur de combat l'utilise sans le savoir), mais
 * {@link #runIA(Session)} delegue a {@code context.eval(languageId, source)}.
 *
 * Deux gardes complementaires bornent l'execution d'une IA non fiable :
 *   - le statement limit GraalVM (cf {@link PolyglotSandbox}) borne les boucles pures guest ;
 *   - le comptage d'operations LeekScript reste ACTIF (on n'override pas {@code ops()}) :
 *     les fonctions de combat couteuses chargent {@code ai.ops(...)} avant leur travail hote,
 *     que le statement limit ne compterait pas. Le budget d'ops est remis a zero a chaque tour
 *     par {@code runTurn()} (resetCounter), et le compteur de statements GraalVM est lui aussi
 *     remis a zero chaque tour ici via {@link Context#resetLimits()}.
 */
public class PolyglotEntityAI extends EntityAI {

	private final String languageId;
	private final String source;
	private final PolyglotSandbox sandbox;
	private Context context;

	public PolyglotEntityAI(String languageId, String source, PolyglotSandbox sandbox) {
		super(0, LeekScript.LATEST_VERSION);
		this.languageId = languageId;
		this.source = source;
		this.sandbox = sandbox;
		this.valid = true;
	}

	private void ensureContext() {
		if (context == null) {
			// Budgets calques sur le pipeline LeekScript (cf EntityAI.build) : RAM pour les
			// LeekValue alloues cote hote, ops pour borner le travail hote des fonctions de combat.
			setMaxRAM(Math.min(50, mEntity.getRAM()) * 8_000_000);
			setMaxOperations((int) Math.min(Integer.MAX_VALUE, (long) mEntity.getCores() * 1_000_000));
			context = sandbox.createContext(languageId);
			PolyglotAPIBridge.install(context, languageId, this);
		}
	}

	@Override
	public Object runIA(Session session) throws LeekRunException {
		ensureContext();
		// Budget de statements par tour : le contexte est reutilise entre tours (etat statique
		// guest persistant) mais le statement limit GraalVM est cumulatif sur la vie du contexte.
		context.resetLimits();
		try {
			Value result = context.eval(languageId, source);
			// Peut lever LeekRunException (marshalling du retour : ops/profondeur) : propagee telle quelle.
			return TypeMarshaller.toJava(result, this);
		} catch (PolyglotException e) {
			throw mapException(e);
		} catch (RuntimeException e) {
			// Filet : erreur hote inattendue pendant le marshalling du retour (hors eval).
			LeekRunException unwrapped = unwrapLeekRunException(e);
			throw unwrapped != null ? unwrapped : new LeekRunException(Error.AI_INTERRUPTED, new String[] { String.valueOf(e.getMessage()) });
		}
	}

	private LeekRunException mapException(PolyglotException e) {
		// Limite atteinte : le contexte passe en etat "cancelled", son close() auto relancerait
		// l'exception -> on le ferme defensivement ici (le prochain tour en reconstruira un neuf).
		if (e.isResourceExhausted() || e.isCancelled()) {
			closeContext();
			return new LeekRunException(Error.TOO_MUCH_OPERATIONS);
		}
		// Erreur hote remontee par le bridge (ex: LeekRunException issue d'une fonction de combat).
		if (e.isHostException()) {
			LeekRunException unwrapped = unwrapLeekRunException(e.asHostException());
			if (unwrapped != null) {
				return unwrapped;
			}
		}
		// Erreur guest (JS/Python) : erreur utilisateur classique.
		return new LeekRunException(Error.AI_INTERRUPTED, new String[] { String.valueOf(e.getMessage()) });
	}

	private static LeekRunException unwrapLeekRunException(Throwable t) {
		while (t != null) {
			if (t instanceof LeekRunException) {
				return (LeekRunException) t;
			}
			t = t.getCause();
		}
		return null;
	}

	private void closeContext() {
		if (context != null) {
			try {
				context.close(true); // cancelIfExecuting : ferme sans relancer
			} catch (Exception ignore) {
				// best effort
			}
			context = null;
		}
	}

	/** A appeler en fin de combat pour liberer le contexte (sinon ferme par PolyglotSandbox.close). */
	public void dispose() {
		closeContext();
	}
}
