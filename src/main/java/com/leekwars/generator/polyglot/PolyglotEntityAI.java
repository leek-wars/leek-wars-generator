package com.leekwars.generator.polyglot;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.state.Entity;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
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

	/** Nom de la fonction d'entree appelee a chaque tour (style LeekScript : corps re-execute). */
	private static final String TURN_FUNCTION = "turn";

	private final String languageId;
	private final String source;
	private final PolyglotSandbox sandbox;
	private Context context;
	private boolean initialized;
	private Value entry; // fonction turn() si le source en definit une

	public PolyglotEntityAI(String languageId, String source, PolyglotSandbox sandbox) {
		super(0, LeekScript.LATEST_VERSION);
		this.languageId = languageId;
		this.source = source;
		this.sandbox = sandbox;
		this.valid = true;
	}

	/** Detecte le langage polyglot a partir de l'extension du fichier (null = LeekScript). */
	public static String detectLanguage(String path) {
		if (path == null) {
			return null;
		}
		if (path.endsWith(".js")) {
			return "js";
		}
		// .py : phase 2 (GraalPy a ajouter). On laisse passer au pipeline LeekScript pour l'instant.
		return null;
	}

	/**
	 * Construit une IA polyglot pour une entite, en reutilisant le sandbox du combat.
	 * Erreur de construction -&gt; IA vide (l'entite n'agira pas), comme le chemin LeekScript.
	 */
	public static EntityAI build(Generator generator, AIFile file, Entity entity, String languageId) {
		try {
			Fight fight = (Fight) entity.getFight();
			PolyglotSandbox sandbox = fight.getPolyglotSandbox(languageId);
			PolyglotEntityAI ai = new PolyglotEntityAI(languageId, file.getCode(), sandbox);
			ai.setEntity(entity);
			ai.setLogs((LeekLog) entity.getLogs());
			return ai;
		} catch (Exception e) {
			generator.exception(e, (Fight) entity.getFight(), entity.getFarmer(), file);
			((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA);
			return new EntityAI(entity, (LeekLog) entity.getLogs());
		}
	}

	private void ensureContext() {
		if (context == null) {
			// Budgets calques sur le pipeline LeekScript (cf EntityAI.build) : RAM pour les
			// LeekValue alloues cote hote, ops pour borner le travail hote des fonctions de combat.
			setMaxRAM(Math.min(50, mEntity.getRAM()) * 8_000_000);
			setMaxOperations((int) Math.min(Integer.MAX_VALUE, (long) mEntity.getCores() * 1_000_000));
			context = sandbox.createContext(languageId);
			PolyglotAPIBridge.install(context, languageId, this);
			installDeterministicRandom();
		}
	}

	/**
	 * Remplace le generateur aleatoire du langage par le RNG seede du combat, sinon les IA
	 * JS/Python utilisant le hasard ne seraient pas reproductibles (les combats se rejouent
	 * cote client depuis le log : le serveur doit etre deterministe pour une seed donnee).
	 */
	private void installDeterministicRandom() {
		if ("js".equals(languageId)) {
			context.getBindings(languageId).putMember("__lw_random", (ProxyExecutable) args -> getRandom().getDouble());
			context.eval(languageId, "Math.random = function() { return __lw_random(); };");
		}
		// Python (random.seed / random.random) : a brancher en phase 2.
	}

	@Override
	public Object runIA(Session session) throws LeekRunException {
		ensureContext();
		// Budget de statements par tour : le contexte est reutilise entre tours (etat statique
		// guest persistant) mais le statement limit GraalVM est cumulatif sur la vie du contexte.
		context.resetLimits();
		try {
			Value value;
			if (!initialized) {
				// 1er tour : on evalue tout le source une fois (definit classes + fonction turn()).
				// staticInit style LeekScript : les statics de classe persistent entre les tours.
				Value top = context.eval(languageId, source);
				Value fn = context.getBindings(languageId).getMember(TURN_FUNCTION);
				entry = (fn != null && fn.canExecute()) ? fn : null;
				initialized = true;
				// turn() definie -> le top-level n'etait que du setup, on joue le 1er tour ;
				// sinon le source "plat" EST la logique de tour, on renvoie son resultat.
				value = entry != null ? entry.execute() : top;
			} else if (entry != null) {
				value = entry.execute(); // tours suivants : on rejoue turn()
			} else {
				// Source plat (sans turn()) : re-evalue chaque tour. Limitation JS : un let/const/class
				// au top-level leve "already declared" au 2e tour (le contexte est reutilise).
				// Pour une IA multi-tours, definir une fonction turn() (cf doc de classe).
				value = context.eval(languageId, source);
			}
			// Peut lever LeekRunException (marshalling du retour : ops/profondeur) : propagee telle quelle.
			return TypeMarshaller.toJava(value, this);
		} catch (PolyglotException e) {
			LeekRunException mapped = mapException(e);
			if (!initialized) closeContext(); // echec de setup (tour 1) : repartir sur un contexte neuf
			throw mapped;
		} catch (RuntimeException e) {
			// Filet : erreur hote inattendue pendant le marshalling du retour (hors eval).
			if (!initialized) closeContext();
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
		// Le prochain ensureContext reconstruira un contexte neuf : on re-evaluera le source
		// (entry pointait vers l'ancien contexte ferme).
		initialized = false;
		entry = null;
	}

	/** A appeler en fin de combat pour liberer le contexte (sinon ferme par PolyglotSandbox.close). */
	public void dispose() {
		closeContext();
	}
}
