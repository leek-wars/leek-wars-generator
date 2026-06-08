package com.leekwars.generator.polyglot;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
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
 * Modele d'execution multi-tours (le contexte est reutilise tout le combat) :
 *   - IA avec etat : definir une fonction {@code turn()}. Le source est evalue une fois (classes,
 *     champs static = memoire persistante), puis {@code turn()} est rejouee chaque tour.
 *   - IA simple : ecrire le code directement (sans {@code turn()}). Il est rejoue chaque tour.
 *     En JS le corps est rejoue dans une IIFE (variables top-level fraiches chaque tour, pas de
 *     persistance ; pas de {@code return} au top-level). En Python le source est re-evalue tel quel
 *     dans le contexte reutilise : les variables de module PERSISTENT entre les tours. turn() reste
 *     recommandee pour un etat explicite (attributs de classe).
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

	/** Nom de la fonction d'entree (optionnelle) appelee a chaque tour, pour un etat persistant. */
	private static final String TURN_FUNCTION = "turn";
	/** Detecte une IA JS multi-fichiers (modules ES) : import/export en debut de ligne. */
	private static final Pattern ES_MODULE = Pattern.compile("(?m)^\\s*(import\\s[^(]|export[\\s{*])");

	private final String languageId;
	private final String source;
	private final PolyglotSandbox sandbox;
	private final String entryPath;            // chemin LeekScript de l'entree (null = mono-fichier)
	private final PolyglotFileSystem fileSystem; // fichiers du joueur montes (null = mono-fichier)
	private final boolean jsModule;            // entree JS chargee comme module ES
	private Context context;
	private boolean initialized;
	private Value entry;                       // fonction turn() si definie

	public PolyglotEntityAI(String languageId, String source, PolyglotSandbox sandbox) {
		this(languageId, source, null, null, sandbox);
	}

	public PolyglotEntityAI(String languageId, String source, String entryPath, PolyglotFileSystem fileSystem, PolyglotSandbox sandbox) {
		super(0, LeekScript.LATEST_VERSION);
		this.languageId = languageId;
		this.source = source;
		this.entryPath = entryPath;
		this.fileSystem = fileSystem;
		this.sandbox = sandbox;
		this.jsModule = entryPath != null && usesEsModules(languageId, source);
		this.valid = true;
	}

	/** IA JS multi-fichiers : l'entree utilise des modules ES (import/export en debut de ligne). */
	private static boolean usesEsModules(String languageId, String source) {
		return "js".equals(languageId) && ES_MODULE.matcher(source).find();
	}

	/** Detecte le langage polyglot a partir de l'extension du fichier (null = LeekScript). */
	public static String detectLanguage(String path) {
		if (path == null) {
			return null;
		}
		if (path.endsWith(".js")) {
			return "js";
		}
		if (path.endsWith(".py")) {
			return "python";
		}
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

			// Multi-fichiers : on monte les fichiers du joueur (enumeres via la FileSystem LeekScript)
			// pour que les import/require de l'IA resolvent leurs voisins, en lecture seule.
			PolyglotFileSystem fs = buildFileSystem(file);

			// Validation syntaxique au build (parite avec LeekScript). On la saute pour une entree
			// JS module : son source contient des `import` (illegaux hors module) qui feraient
			// echouer un parse de script ; les erreurs remonteront au 1er tour (chargement du module).
			boolean isJsModule = usesEsModules(languageId, file.getCode());
			if (!isJsModule) {
				try (Context probe = sandbox.createContext(languageId)) {
					probe.parse(languageId, file.getCode());
				} catch (PolyglotException e) {
					// Toute erreur de parse vient du code joueur (syntaxe...) -> IA invalide (erreur
					// utilisateur), jamais le chemin "erreur serveur" du catch externe.
					((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.INVALID_AI, new String[] { e.getMessage() });
					return new EntityAI(entity, (LeekLog) entity.getLogs());
				}
			}

			PolyglotEntityAI ai = new PolyglotEntityAI(languageId, file.getCode(), file.getPath(), fs, sandbox);
			ai.setEntity(entity);
			ai.setLogs((LeekLog) entity.getLogs());
			return ai;
		} catch (Exception e) {
			generator.exception(e, (Fight) entity.getFight(), entity.getFarmer(), file);
			((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA);
			return new EntityAI(entity, (LeekLog) entity.getLogs());
		}
	}

	/**
	 * Monte les fichiers du joueur (proprietaire de l'IA) en lecture seule, pour le multi-fichiers.
	 * Enumeration via la FileSystem LeekScript (la meme capacite que les includes LeekScript) ;
	 * si elle n'enumere pas, on retombe sur le seul fichier d'entree (mono-fichier).
	 */
	private static PolyglotFileSystem buildFileSystem(AIFile entry) {
		Set<String> paths = new HashSet<>();
		paths.add(entry.getPath());
		try {
			var lfs = LeekScript.getFileSystem();
			int owner = entry.getOwner();
			var root = lfs.getRoot(owner);
			for (AIFile f : lfs.listAllFiles(owner)) {
				paths.add(f.getPath());
			}
			return new PolyglotFileSystem(paths, path -> {
				try {
					AIFile f = root.resolve(path);
					return f != null ? f.getCode() : null;
				} catch (Exception e) {
					return null;
				}
			});
		} catch (Exception e) {
			// Enumeration impossible (FS sans listing, owner inconnu...) : on retombe sur le seul
			// fichier d'entree (mono-fichier). Jamais une erreur serveur pour autant.
			return new PolyglotFileSystem(Set.of(entry.getPath()), path -> {
				try {
					return entry.getPath().equals(path) ? entry.getCode() : null;
				} catch (Exception ex) {
					return null;
				}
			});
		}
	}

	private void ensureContext() {
		if (context == null) {
			// Budgets calques sur le pipeline LeekScript (cf EntityAI.build) : RAM pour les
			// LeekValue alloues cote hote, ops pour borner le travail hote des fonctions de combat.
			setMaxRAM(Math.min(50, mEntity.getRAM()) * 8_000_000);
			setMaxOperations((int) Math.min(Integer.MAX_VALUE, (long) mEntity.getCores() * 1_000_000));
			// Multi-fichiers : on monte le FS pour JS (modules ES). Pour Python, un FS custom nu
			// casse la localisation de la stdlib GraalPy (extraite dans ~/.cache, hors du FS) ; le
			// multi-fichiers Python demandera un FS composant stdlib + fichiers joueur (a venir).
			PolyglotFileSystem fs = "js".equals(languageId) ? fileSystem : null;
			context = sandbox.createContext(languageId, fs);
			PolyglotAPIBridge.install(context, languageId, this);
			installDeterminismGuards();
		}
	}

	/** Chargement de l'entree au 1er tour, selon le mode (module ES / script). */
	private Value loadEntryFirstTurn() throws LeekRunException {
		if (jsModule) {
			// Module ES charge via le FS (import d'un chemin absolu) -> ses imports relatifs
			// resolvent contre /ai. L'IA expose sa logique via globalThis.turn.
			// import() est asynchrone : on capture sa rejection (erreur de syntaxe/exec dans un
			// fichier importe) dans une variable, sinon elle serait silencieusement avalee.
			context.eval(languageId,
				"globalThis.__lw_loadError = null;"
				+ "import('" + PolyglotFileSystem.mountPath(entryPath) + "')"
				+ ".catch(function(e){ globalThis.__lw_loadError = '' + (e && e.stack ? e.stack : e); });");
			context.eval(languageId, "void 0;"); // draine les microtasks (eval du module + le .catch)
			Value loadError = context.getBindings(languageId).getMember("__lw_loadError");
			if (loadError != null && !loadError.isNull()) {
				throw new LeekRunException(Error.AI_INTERRUPTED, new String[] { loadError.asString() });
			}
			return null; // un module ne renvoie pas de valeur
		}
		return context.eval(languageId, source); // JS script / Python : eval direct
	}

	/** Tours suivants d'une IA plate (sans turn()). */
	private Value replayFlatTurn() {
		if (jsModule) {
			return null; // module en cache : deja execute (turn() requise pour agir a chaque tour)
		}
		if ("js".equals(languageId)) {
			// IIFE anonyme (scope frais) : evite la collision "already declared" d'une re-eval top-level.
			return context.eval(languageId, "(function(){" + source + "\n})()");
		}
		return context.eval(languageId, source); // Python (et autres) : re-eval brut
	}

	// RNG seede + gele (non reassignable) + horloge murale fixe. Sinon une IA pourrait reintroduire
	// du non-determinisme (Math.random reassigne, new Date(), Date.now), cassant la reproductibilite.
	private static final String JS_DETERMINISM_GUARD =
		"Object.defineProperty(Math,'random',{value:function(){return __lw_random();},writable:false,configurable:false});"
		+ "(function(){var F=0;var D=Date;var L=function(){if(arguments.length===0)return new D(F);return new D(...arguments);};"
		+ "L.now=function(){return F;};L.parse=D.parse;L.UTC=D.UTC;L.prototype=D.prototype;globalThis.Date=L;})();"
		+ "if(typeof performance!=='undefined'){performance.now=function(){return 0;};}";

	/**
	 * Neutralise les sources de non-determinisme atteignables par le guest, sinon les IA JS/Python
	 * ne seraient pas reproductibles a partir de la seed du combat (re-simulation / verification) :
	 * generateur aleatoire seede et fige, et horloge murale fixe.
	 */
	private void installDeterminismGuards() {
		if ("js".equals(languageId)) {
			context.getBindings(languageId).putMember("__lw_random", (ProxyExecutable) args -> getRandom().getDouble());
			context.eval(languageId, JS_DETERMINISM_GUARD);
		} else if ("python".equals(languageId)) {
			// Plage bornee a l'int : getLong caste en int et un (max-min+1) qui overflow renvoie 0.
			long seed = getRandom().getLong(0, Integer.MAX_VALUE - 1);
			context.eval(languageId, pythonDeterminismGuard(seed));
		}
	}

	/**
	 * Garde Python : seede random, ET re-route toutes les sources d'entropie OS (os.urandom,
	 * SystemRandom, uuid4, random.seed() sans argument) vers un PRNG seede, et fige l'horloge.
	 * Sans cela un simple {@code import os; os.urandom(1)} contournerait silencieusement la seed.
	 */
	private static String pythonDeterminismGuard(long seed) {
		return "import os, random, uuid, time, datetime\n"
			+ "_lw_r = random.Random(" + seed + ")\n"
			+ "os.urandom = lambda n: bytes(_lw_r.getrandbits(8) for _ in range(n))\n"
			+ "random.SystemRandom = random.Random\n"
			+ "_lw_seed = random.seed\n"
			+ "def _lw_seed_guard(a=None, *ar, **kw):\n    return _lw_seed(" + seed + " if a is None else a, *ar, **kw)\n"
			+ "random.seed = _lw_seed_guard\n"
			+ "random.seed(" + seed + ")\n"
			+ "uuid.uuid4 = lambda: uuid.UUID(int=_lw_r.getrandbits(128))\n"
			+ "time.time = lambda: 0.0\ntime.monotonic = lambda: 0.0\ntime.perf_counter = lambda: 0.0\n"
			+ "try:\n"
			+ "    datetime.datetime.now = classmethod(lambda cls, tz=None: datetime.datetime(2020, 1, 1))\n"
			+ "    datetime.datetime.today = classmethod(lambda cls: datetime.datetime(2020, 1, 1))\n"
			+ "    datetime.datetime.utcnow = classmethod(lambda cls: datetime.datetime(2020, 1, 1))\n"
			+ "except Exception:\n    pass\n";
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
				// 1er tour : on charge l'entree une fois (definit classes / globalThis.turn / fonction turn()).
				// On marque initialized tot : meme si le chargement echoue (et leve), on ne recharge pas
				// (les tours suivants passeront par replayFlatTurn, inertes, plutot que de boucler).
				initialized = true;
				Value top = loadEntryFirstTurn();
				Value turnFn = context.getBindings(languageId).getMember(TURN_FUNCTION);
				if (turnFn != null && turnFn.canExecute()) {
					// IA avec etat : le top-level (classes + statics) n'etait que du setup execute
					// une fois ; turn() est rejouee chaque tour (les statics de classe persistent).
					entry = turnFn;
					value = entry.execute();
				} else {
					// IA "plate" (sans turn()) : le source EST la logique de tour, et vient de
					// s'executer. turn() est donc OPTIONNELLE.
					value = top;
				}
			} else if (entry != null) {
				value = entry.execute(); // IA avec etat : turn() rejouee chaque tour
			} else {
				value = replayFlatTurn(); // IA plate sans turn() : tours suivants
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
		// Erreur interne (observe avec GraalPy + statement limit sur le runtime interprete :
		// l'interruption d'une boucle remonte en erreur interne). La boucle est bornee ; on ferme
		// le contexte par securite et on signale une erreur d'IA. On loggue cote serveur pour
		// garder la visibilite sur un eventuel vrai bug GraalVM (sinon masque en erreur joueur).
		if (e.isInternalError()) {
			Log.w("PolyglotEntityAI", "Erreur interne polyglot (" + languageId + "), souvent une limite de ressource sur le runtime interprete: " + e.getMessage());
			closeContext();
			return new LeekRunException(Error.AI_INTERRUPTED, new String[] { String.valueOf(e.getMessage()) });
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
