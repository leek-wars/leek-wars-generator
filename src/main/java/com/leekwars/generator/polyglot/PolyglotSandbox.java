package com.leekwars.generator.polyglot;

import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.io.IOAccess;

/**
 * Fabrique de contextes GraalVM verrouilles pour l'execution d'IA en JS/Python.
 *
 * Un {@link Engine} est partage (idealement un par combat) pour amortir le warmup
 * Truffle entre les entites. Chaque entite recoit son propre {@link Context} (scope
 * global isole) construit via {@link #createContext(String)}.
 *
 * Securite : pas d'acces hote, pas d'IO, pas de threads, pas de natif, pas de process.
 * Les boucles pures guest sont bornees par un compteur de statements ; le travail
 * cote hote (fonctions de combat couteuses) reste borne par le comptage d'operations
 * LeekScript ({@code ai.ops(...)}), que {@link PolyglotEntityAI} laisse ACTIF (le
 * statement limit ne compte pas le temps passe dans le code hote).
 *
 * Le sandbox possede les contextes qu'il cree et les ferme tous a {@link #close()},
 * pour ne pas fuiter de {@link Context} si {@link PolyglotEntityAI#dispose()} n'est
 * pas appele.
 */
public class PolyglotSandbox implements AutoCloseable {

	/** Limite par defaut : 20M statements guest (cf. issue #3179). */
	public static final long DEFAULT_STATEMENT_LIMIT = 20_000_000L;

	private final Engine engine;
	private final ResourceLimits limits;
	private final StatementCounter.Counter statementCounter;
	private final List<Context> contexts = Collections.synchronizedList(new ArrayList<>());

	public PolyglotSandbox(String... languages) {
		this(DEFAULT_STATEMENT_LIMIT, languages);
	}

	public PolyglotSandbox(long statementLimit, String... languages) {
		String[] permitted = languages.length == 0 ? new String[] { "js" } : languages;
		this.engine = Engine.newBuilder(permitted).build();
		// Compteur de statements guest DETERMINISTE (cf StatementCounter) : le lookup ACTIVE l'instrument
		// sur cet engine. Sert a PolyglotEntityAI.getOperations() (operations reproductibles). null si
		// l'instrument est indisponible -> degradation gracieuse (pas de terme synthetique).
		StatementCounter.Counter counter = null;
		try {
			var instrument = engine.getInstruments().get(StatementCounter.ID);
			if (instrument != null) {
				counter = instrument.lookup(StatementCounter.Counter.class);
			}
		} catch (Exception ignore) {
			// best effort
		}
		this.statementCounter = counter;
		this.limits = ResourceLimits.newBuilder()
				.statementLimit(statementLimit, null)
				.build();
	}

	/** Compteur de statements guest (deterministe) de cet engine, ou null s'il est indisponible. */
	public StatementCounter.Counter getStatementCounter() {
		return statementCounter;
	}

	/** Racine de la stdlib GraalPy (decouverte une fois, mise en cache). */
	private static volatile Path pythonStdlibRoot;
	private static volatile boolean pythonStdlibProbed;

	/**
	 * Racine du python-home GraalPy (sa stdlib .py extraite dans ~/.cache), a deleguer en lecture
	 * seule pour que le multi-fichiers Python n'ecrase pas la stdlib. Decouverte par un contexte
	 * Python jetable (une fois par JVM). null si Python indisponible -&gt; multi-fichiers Python off.
	 */
	public static Path pythonStdlibRoot() {
		if (!pythonStdlibProbed) {
			synchronized (PolyglotSandbox.class) {
				if (!pythonStdlibProbed) {
					try (Context boot = Context.newBuilder("python").allowIO(IOAccess.ALL).build()) {
						pythonStdlibRoot = Path.of(boot.eval("python", "import sys; sys.prefix").asString()).toAbsolutePath().normalize();
					} catch (Exception e) {
						pythonStdlibRoot = null;
					}
					pythonStdlibProbed = true;
				}
			}
		}
		return pythonStdlibRoot;
	}

	/** Contexte isole et verrouille, sans systeme de fichiers (IA mono-fichier). */
	public Context createContext(String languageId) {
		return createContext(languageId, null);
	}

	/**
	 * Construit un contexte isole et verrouille pour le langage donne (et le suit pour la fermeture).
	 * Si {@code fileSystem} != null, il est monte (multi-fichiers) : les import/require resolvent
	 * a travers lui, en lecture seule et uniquement sur les fichiers du joueur (aucun acces hote).
	 */
	public Context createContext(String languageId, PolyglotFileSystem fileSystem) {
		Context.Builder builder = Context.newBuilder(languageId)
				.engine(engine)
				.allowHostAccess(HostAccess.NONE)
				.allowAllAccess(false)
				.allowCreateThread(false)
				.allowNativeAccess(false)
				.allowCreateProcess(false)
				.allowHostClassLoading(false)
				// La sortie guest (console.log / print) est jetee : sinon une IA pourrait spammer
				// le stdout/les logs du serveur. (Le logging joueur passera par l'API de combat.)
				.out(OutputStream.nullOutputStream())
				.err(OutputStream.nullOutputStream())
				.resourceLimits(limits);

		if (fileSystem != null) {
			builder.allowIO(IOAccess.newBuilder().fileSystem(fileSystem).build());
			if ("python".equals(languageId)) {
				builder.option("python.PythonPath", PolyglotFileSystem.MOUNT);
			}
		} else {
			builder.allowIO(IOAccess.NONE);
		}

		Context context = builder.build();
		contexts.add(context);
		return context;
	}

	/**
	 * Oublie un contexte deja ferme (par {@code PolyglotEntityAI.closeContext}), pour qu'il ne soit pas
	 * retenu jusqu'a {@link #close()}. Sans ca, une IA reconstruite a chaque tour (timeout / limite)
	 * accumulerait ses contextes morts dans la liste pour tout le combat.
	 */
	public void forgetContext(Context context) {
		contexts.remove(context);
	}

	@Override
	public void close() {
		synchronized (contexts) {
			for (Context context : contexts) {
				try {
					context.close(true); // cancelIfExecuting : ne relance pas si epuise
				} catch (Exception ignore) {
					// best effort
				}
			}
			contexts.clear();
		}
		engine.close();
	}

	// ---------- Watchdog wall-clock (anti-DoS) ----------
	// Le statement limit borne les boucles pures guest, mais PAS le travail natif fait en un seul
	// statement (ex: 'x'*10**9, un gros bignum) : ce backstop wall-clock annule un tour qui depasse
	// une echeance. Infra statique partagee par tous les combats de la JVM : un timer (1 thread daemon)
	// arme/desarme les echeances ; quand une echeance expire, l'interruption (potentiellement bloquante)
	// est faite sur un pool separe pour ne jamais saturer le timer.

	/** Delai laisse a une interruption douce avant d'escalader vers une annulation dure (close). */
	private static final long INTERRUPT_GRACE_MS = 2_000;

	private static volatile ScheduledThreadPoolExecutor watchdogTimer;
	private static volatile ExecutorService watchdogWorkers;

	private static ScheduledThreadPoolExecutor watchdogTimer() {
		if (watchdogTimer == null) {
			synchronized (PolyglotSandbox.class) {
				if (watchdogTimer == null) {
					ThreadFactory tf = daemonFactory("polyglot-watchdog");
					ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, tf);
					// Cas normal (tour fini a temps) : la tache est annulee -> on la retire aussitot de
					// la file pour ne pas l'accumuler jusqu'a son echeance.
					exec.setRemoveOnCancelPolicy(true);
					watchdogTimer = exec;
				}
			}
		}
		return watchdogTimer;
	}

	private static ExecutorService watchdogWorkers() {
		if (watchdogWorkers == null) {
			synchronized (PolyglotSandbox.class) {
				if (watchdogWorkers == null) {
					watchdogWorkers = Executors.newCachedThreadPool(daemonFactory("polyglot-watchdog-worker"));
				}
			}
		}
		return watchdogWorkers;
	}

	private static ThreadFactory daemonFactory(String name) {
		return r -> {
			Thread t = new Thread(r, name);
			t.setDaemon(true);
			return t;
		};
	}

	/**
	 * Arme une echeance : apres {@code deadlineMs}, {@code onExpire} est execute sur le thread du timer.
	 * Retourne le handle a {@link ScheduledFuture#cancel(boolean) cancel} en fin de tour.
	 */
	public static ScheduledFuture<?> scheduleDeadline(Runnable onExpire, long deadlineMs) {
		return watchdogTimer().schedule(onExpire, deadlineMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * Demande l'arret d'un tour qui depasse son echeance, sur un thread separe (l'operation peut bloquer
	 * si le guest ne cede pas). On tente d'abord une interruption douce, puis on escalade vers une
	 * annulation dure (close).
	 *
	 * RESIDU CONNU : interrupt ET close dependent tous deux d'un safepoint Truffle. Un travail natif
	 * en UN statement qui n'atteint aucun safepoint (gros bignum, allocation massive) reste donc
	 * inarretable : le thread de combat (et ce thread-ci) restent bloques jusqu'a la fin de l'operation.
	 * Pas de remede en GraalVM community (ni limite de heap, ni preemption forcee) : il faudrait
	 * l'edition EE (sandbox policy) ou une isolation par process.
	 */
	public static void interruptAsync(Context context) {
		watchdogWorkers().execute(() -> {
			try {
				context.interrupt(Duration.ofMillis(INTERRUPT_GRACE_MS));
			} catch (TimeoutException te) {
				try {
					context.close(true); // derniere cartouche : annulation dure
				} catch (Exception ignore) {
					// best effort
				}
			} catch (Exception ignore) {
				// contexte deja ferme / etat invalide : best effort
			}
		});
	}
}
