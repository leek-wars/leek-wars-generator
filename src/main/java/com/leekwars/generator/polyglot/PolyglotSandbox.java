package com.leekwars.generator.polyglot;

import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import com.leekwars.generator.Log;

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

	/**
	 * Limite memoire TOTALE de l'isolate (par langage), en octets : adresse RESERVEE, committee a la
	 * demande. Doit couvrir la somme des caps par-poireau (jusqu'a ~8 entites qui RETIENNENT leur RAM
	 * entre tours) + l'overhead runtime GraalJS/GraalPy. TODO calibrer vs le nombre d'entites max.
	 */
	private static final long MAX_ISOLATE_MEMORY = 4_000_000_000L;

	/** Cap RAM par defaut par contexte (par poireau) si non fourni (probes de validation), en octets. */
	private static final long DEFAULT_MAX_HEAP_MEMORY = 100_000_000L;

	// Limites exigees par SandboxPolicy.ISOLATED, posees GENEREUSEMENT : le vrai backstop reste le
	// watchdog wall-clock de PolyglotEntityAI + le cap RAM par poireau. Regroupees ici (memes knobs
	// que les budgets ci-dessus) plutot que dispersees en litteraux dans le builder.
	private static final String MAX_CPU_TIME = "10m";
	private static final String MAX_CPU_TIME_CHECK_INTERVAL = "10ms";
	private static final String MAX_STACK_FRAMES = "50000";
	private static final String MAX_THREADS = "1";
	private static final String MAX_AST_DEPTH = "5000";
	private static final String MAX_STREAM_SIZE = "1MB";

	private final long statementLimit;
	private final String statementLimitOption; // = String.valueOf(statementLimit), reutilise par createContext
	/**
	 * Un Engine (= un isolate) PAR LANGAGE : les images isolate sont par-langage
	 * (js-isolate != python-isolate), un meme isolate ne peut pas heberger js ET python. Lazy,
	 * partage par tous les contextes du meme langage du combat (amortit le warmup).
	 */
	private final Map<String, Engine> engines = new ConcurrentHashMap<>();
	private final List<Context> contexts = Collections.synchronizedList(new ArrayList<>());

	public PolyglotSandbox(String... languages) {
		this(DEFAULT_STATEMENT_LIMIT, languages);
	}

	public PolyglotSandbox(long statementLimit, String... languages) {
		this.statementLimit = statementLimit;
		this.statementLimitOption = String.valueOf(statementLimit);
	}

	/** Formate un nombre d'octets en option memoire GraalVM ("&lt;n&gt;MB"). */
	private static String memoryOption(long bytes) {
		return Math.max(1L, bytes / 1_000_000L) + "MB";
	}

	/**
	 * Compteur de statements DETERMINISTE de l'image isolate JS custom (repo graal-isolate), publie par
	 * l'instrument {@link StatementCounter} dans les polyglot bindings de chaque contexte — le lookup de
	 * service hote ({@code getInstruments().get(ID).lookup(Counter.class)}) ne traverse PAS la frontiere
	 * isolate. Renvoie l'executable (execute() = lire, execute(x) = remise a zero), ou null si l'image
	 * ne l'embarque pas (image officielle, ex Python) -&gt; {@code getOperations()} retombe sur le terme
	 * temps CPU (non deterministe).
	 */
	public static Value statementCounterBinding(Context context) {
		try {
			Value counter = context.getPolyglotBindings().getMember(StatementCounter.BINDING_NAME);
			return counter != null && counter.canExecute() ? counter : null;
		} catch (Exception e) {
			return null;
		}
	}

	/** Langages dont l'engine a du basculer en isolate processus externe (cf engineFor). */
	private final Set<String> externalIsolates = ConcurrentHashMap.newKeySet();

	/** Vrai si l'engine de ce langage tourne en isolate processus EXTERNE (mode degrade, cf engineFor). */
	public boolean isExternalIsolate(String languageId) {
		return externalIsolates.contains(languageId);
	}

	/**
	 * Engine isolate (SandboxPolicy.ISOLATED) pour un langage, cree a la demande et partage.
	 *
	 * <p>Deux replis gracieux (echelle, jamais d'echec du combat pour un probleme d'image) :
	 * <ul>
	 * <li>option {@code lw-statement-counter} inconnue (image isolate officielle SANS notre
	 * instrument, cf repo graal-isolate) -&gt; engine sans compteur deterministe, getOperations()
	 * retombe sur le temps CPU ;</li>
	 * <li>LIMITE GraalVM : UNE seule lib isolate in-process par JVM ("A native library for
	 * engine.SpawnIsolate was already loaded") : un worker qui a deja charge l'isolate js ne peut
	 * plus charger l'isolate python (et inversement) -&gt; bascule de ce langage en isolate
	 * PROCESSUS EXTERNE ({@code engine.IsolateMode=external}) : coexistence retablie, isolation
	 * renforcee, MAIS {@code sandbox.MaxHeapMemory} par contexte n'y est PAS applique (verifie par
	 * TestPolyglotRamLimit) -&gt; la RAM n'est plus bornee que par {@code engine.MaxIsolateMemory}
	 * (par combat-langage) ; le worker reste a l'abri (process a part).</li>
	 * </ul>
	 */
	private Engine engineFor(String languageId) {
		return engines.computeIfAbsent(languageId, lang -> {
			// L'image isolate combinee (js+python) embarque l'instrument pour TOUS ses langages ;
			// si une image officielle (sans instrument) est revenue, l'echelle de replis ci-dessous
			// retire simplement l'option.
			boolean withCounter = true;
			boolean external = false;
			while (true) {
				try {
					return buildEngine(lang, external, withCounter);
				} catch (IllegalArgumentException e) {
					if (!withCounter) {
						throw e;
					}
					Log.w("PolyglotSandbox", "Instrument " + StatementCounter.ID
							+ " indisponible (image isolate officielle ?) : " + e.getMessage());
					withCounter = false;
				} catch (IllegalStateException e) {
					if (external) {
						throw e;
					}
					Log.w("PolyglotSandbox", "Isolate in-process indisponible pour " + lang
							+ " (une seule lib isolate par JVM) : bascule en isolate processus externe. "
							+ e.getMessage());
					externalIsolates.add(lang);
					external = true;
				}
			}
		});
	}

	private Engine buildEngine(String languageId, boolean externalIsolate, boolean withCounter) {
		Engine.Builder builder = Engine.newBuilder(languageId)
				.sandbox(SandboxPolicy.ISOLATED)
				.option("engine.MaxIsolateMemory", memoryOption(MAX_ISOLATE_MEMORY))
				.out(OutputStream.nullOutputStream())
				.err(OutputStream.nullOutputStream());
		if (externalIsolate) {
			builder.allowExperimentalOptions(true).option("engine.IsolateMode", "external");
		}
		if (withCounter) {
			builder.option(StatementCounter.ID, "true");
		}
		return builder.build();
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
		return createContext(languageId, null, DEFAULT_MAX_HEAP_MEMORY);
	}

	/**
	 * Construit un contexte ISOLE (mode isolate GraalVM) et verrouille pour le langage donne.
	 *
	 * <p>La RAM RETENUE du guest est bornee PAR CONTEXTE (= par poireau) via
	 * {@code sandbox.MaxHeapMemory} : un poireau qui explose sa RAM est annule sans affecter les autres
	 * du meme combat (l'isolate partage n'est pas empoisonne). Le cap est le RETENU (pas l'alloue), donc
	 * les temporaires ne penalisent pas -> honnete, comme le {@code mRAM} LeekScript. {@code maxHeapBytes}
	 * = budget RAM de l'entite (cf {@link PolyglotEntityAI#ensureContext}).
	 *
	 * <p>Note (determinisme) : l'image isolate JS custom embarque l'instrument StatementCounter (ops
	 * deterministes, lus via {@link #statementCounterBinding}) ; Python (image officielle) reste sur le
	 * terme temps CPU. Sous ISOLATED, le statement limit passe par {@code sandbox.MaxStatements} (les
	 * autres {@code sandbox.Max*} sont exiges par la policy et poses genereusement ; le vrai backstop
	 * reste le watchdog wall-clock de PolyglotEntityAI + le cap RAM).
	 *
	 * <p>Si {@code fileSystem} != null, il est monte (multi-fichiers) : les import/require resolvent a
	 * travers lui, en lecture seule et uniquement sur les fichiers du joueur (aucun acces hote).
	 */
	public Context createContext(String languageId, PolyglotFileSystem fileSystem, long maxHeapBytes) {
		Context.Builder builder = Context.newBuilder(languageId)
				.engine(engineFor(languageId))
				.sandbox(SandboxPolicy.ISOLATED)
				.allowCreateThread(false)
				.allowNativeAccess(false)
				.allowCreateProcess(false)
				.allowHostClassLoading(false)
				// La sortie guest (console.log / print) est jetee : sinon une IA pourrait spammer
				// le stdout/les logs du serveur. (Le logging joueur passera par l'API de combat.)
				.out(OutputStream.nullOutputStream())
				.err(OutputStream.nullOutputStream())
				// Cap RAM RETENUE par poireau : le vrai anti-DoS, isole par contexte.
				.option("sandbox.MaxHeapMemory", memoryOption(maxHeapBytes))
				// Ops guest (borne les boucles pures ; non deterministe sous isolate).
				.option("sandbox.MaxStatements", statementLimitOption)
				// Limites exigees par la policy ISOLATED (constantes ci-dessus).
				.option("sandbox.MaxCPUTime", MAX_CPU_TIME)
				.option("sandbox.MaxCPUTimeCheckInterval", MAX_CPU_TIME_CHECK_INTERVAL)
				.option("sandbox.MaxStackFrames", MAX_STACK_FRAMES)
				.option("sandbox.MaxThreads", MAX_THREADS)
				.option("sandbox.MaxASTDepth", MAX_AST_DEPTH)
				.option("sandbox.MaxOutputStreamSize", MAX_STREAM_SIZE)
				.option("sandbox.MaxErrorStreamSize", MAX_STREAM_SIZE);

		if (fileSystem != null) {
			// NB : PAS d'option python.PythonPath ici — elle est TRUSTED-only, la policy ISOLATED
			// la rejette. Le montage /ai est ajoute a sys.path COTE GUEST par le garde de
			// determinisme (PolyglotEntityAI.pythonDeterminismGuard), en FIN de path pour que la
			// stdlib garde la priorite sur les fichiers du joueur.
			builder.allowIO(IOAccess.newBuilder().fileSystem(fileSystem).build());
			// NB : PAS d'option js.esm-bare-specifier-relative-lookup non plus (TRUSTED-only,
			// rejetee par ISOLATED). Les specificateurs bare ('include.js' sans ./) sont ancres a
			// la racine /ai par le loader ; le repli vers le dossier de l'ENTREE est fait par
			// PolyglotFileSystem.probe (cas reel : ia-ts/test.js important son voisin).
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
		for (Engine engine : engines.values()) {
			try {
				engine.close();
			} catch (Exception ignore) {
				// best effort
			}
		}
		engines.clear();
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
