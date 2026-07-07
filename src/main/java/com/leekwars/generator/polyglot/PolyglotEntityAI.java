package com.leekwars.generator.polyglot;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;
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

	/**
	 * Backstop wall-clock par tour : annule un tour qui depasse cette duree. Genereux (un tour
	 * legitime est borne BIEN avant par le statement limit ~20M) : ne sert qu'a couper le travail
	 * natif que le statement limit ne compte pas (ex: {@code 'x'*10**9}). Cf {@link PolyglotSandbox}.
	 *
	 * NB determinisme : une coupure wall-clock est NON deterministe (depend de la charge machine), alors
	 * que le reste du moteur est reproductible (cf les gardes de determinisme RNG/horloge). Acceptable car
	 * une IA legitime ne declenche JAMAIS ce backstop (elle est bornee bien avant par le statement limit) :
	 * la reproductibilite tient pour tous les combats non abusifs ; seul un tour pathologique (deja en
	 * train de saturer le worker) peut etre coupe a un instant dependant de la charge.
	 */
	private static final long DEFAULT_TURN_WALL_CLOCK_LIMIT_MS = 5_000;
	/**
	 * Au-dela de ce nombre de depassements wall-clock sur le combat, l'IA est neutralisee : sinon une
	 * IA malveillante paierait la limite a CHAQUE tour (x64). Une IA legitime ne depasse jamais (bornee
	 * par le statement limit), donc ce compteur ne la touche pas.
	 */
	private static final int MAX_WALL_CLOCK_TIMEOUTS = 3;

	private final String languageId;
	private final String source;
	private final PolyglotSandbox sandbox;
	private final String entryPath;            // chemin LeekScript de l'entree (null = mono-fichier)
	private final PolyglotFileSystem fileSystem; // fichiers du joueur montes (null = mono-fichier)
	private final boolean jsModule;            // entree JS chargee comme module ES
	private Context context;
	private boolean initialized;
	private Value entry;                       // fonction turn() si definie

	private long turnWallClockLimitMs = DEFAULT_TURN_WALL_CLOCK_LIMIT_MS;
	private int wallClockTimeouts;             // nb de tours coupes par le watchdog sur ce combat
	private boolean disabled;                  // IA neutralisee apres trop de depassements wall-clock

	/**
	 * Fraction du budget wall-clock du tour a laquelle le compteur d'operations synthetique atteint
	 * {@code getMaxOperations()}. A 0.5, une IA qui surveille son budget (garde
	 * {@code getOperations() > seuil}) s'arrete vers la moitie de l'echeance, laissant une marge
	 * confortable avant le backstop dur. Cf {@link #getOperations()}.
	 */
	private static final double OPS_TIME_BUDGET_FRACTION = 0.5;
	// Terme d'ops synthetique (sous isolate, faute d'instrument deterministe) : base sur le TEMPS CPU du
	// thread de combat passe dans le tour -> plus honnete que le temps mur (exclut GC / ordonnancement).
	private static final ThreadMXBean THREAD_MX = ManagementFactory.getThreadMXBean();
	private static final boolean CPU_TIME_SUPPORTED = enableCpuTime();
	private static boolean enableCpuTime() {
		if (THREAD_MX.isThreadCpuTimeSupported()) {
			THREAD_MX.setThreadCpuTimeEnabled(true);
			return true;
		}
		return false;
	}
	private long turnStartNanos;               // temps mur de debut de tour (repli si temps CPU indispo)
	private long turnThreadId;                 // id du thread de combat du tour (pour lire son temps CPU)
	private long turnStartCpuNanos = -1L;      // temps CPU du thread au debut du tour (base du terme synthetique)
	// Budget temps (ns) au bout duquel le compteur synthetique atteint getMaxOperations(). Derive de
	// turnWallClockLimitMs (recalcule dans le setter) ; pre-calcule car getOperations() est sur le hot path.
	private long opsBudgetNanos = (long) (DEFAULT_TURN_WALL_CLOCK_LIMIT_MS * 1_000_000L * OPS_TIME_BUDGET_FRACTION);
	// Compteur de statements guest DETERMINISTE : executable publie par l'instrument StatementCounter
	// de l'image isolate custom dans les polyglot bindings du contexte (execute() = lire,
	// execute(x) = reset). Refetche a chaque (re)construction de contexte par ensureContext ; null si
	// l'image ne l'embarque pas (ex Python officiel) -> fallback temps CPU (non reproductible).
	private Value statementCounter;
	// Bindings globaux du guest (cache) : sert a rafraichir le miroir __lw_real pour le getOperations
	// cote guest (cf installGuestGetOperations). null si l'override guest n'est pas actif.
	private Value guestBindings;
	// Engine en isolate processus EXTERNE (repli une-lib-par-JVM, cf PolyglotSandbox.engineFor) :
	// le guest tourne dans un AUTRE process, le temps CPU du thread de combat ne mesure plus son
	// travail -> le terme d'ops synthetique doit passer au temps MUR (sinon la garde getOperations()
	// ne monte jamais et chaque tour epuise son budget de statements, ~1,6 s/tour constate en beta).
	private boolean externalIsolate;

	/**
	 * CALIBRATION FAIRNESS INTER-LANGAGES (ops). LeekScript compte des OPERATIONS (granularite
	 * expression : {@code s += i} ~ 4 ops), l'instrument compte des STATEMENTS (granularite ligne :
	 * {@code s += i} = 1). Consequence mesuree (TestOpsCalibration) : a travail identique, JS
	 * consomme ~2x MOINS d'ops que LeekScript et Python ~5x moins -> sans correction, un joueur
	 * Python fait ~5x plus de calcul par tour a budget egal (avantage de recherche en arene classee).
	 * On multiplie le terme guest de {@link #getOperations()} par ce facteur pour rendre le compteur
	 * COMPARABLE entre langages. Compter les expressions serait plus principiel MAIS GraalPy ne tague
	 * PAS les expressions (verifie) -> le multiplicateur est le seul levier commun.
	 *
	 * <p>Facteurs = mediane des workloads combat-typiques (entiers/branches/appels ; le flottant et
	 * les strings ont un ratio different, non couvrable par une constante). AJUSTABLES (game design) :
	 * augmenter = plus severe (moins de calcul autorise). 1.0 = LeekScript (reference, non scale).
	 */
	private static double opsFactor(String languageId) {
		switch (languageId) {
			case "python": return 5.0;
			case "js":     return 2.0;
			default:       return 1.0;
		}
	}
	private final double opsFactor;

	/**
	 * CALIBRATION FAIRNESS INTER-LANGAGES (RAM). Le cap {@code min(50,RAM)*8Mo} est passe tel quel a
	 * {@code sandbox.MaxHeapMemory} (OCTETS) alors que LeekScript l'interprete en unites mRAM
	 * LOGIQUES. Consequence mesuree (TestRamCalibration) : a cap identique, JS et Python retiennent
	 * ~3.8x PLUS de donnees (tableau d'entiers) que LeekScript. On divise le cap guest par ce facteur
	 * pour rapprocher la parite. AJUSTABLE. NB : approximatif (un objet JS/Py pese plus qu'un entier
	 * packe -> le ratio varie selon la structure), donc a affiner selon les IA reelles.
	 */
	private static final double RAM_FACTOR = 3.8;

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
		this.opsFactor = opsFactor(languageId);
		this.jsModule = entryPath != null && usesEsModules(languageId, source);
		this.valid = true;
	}

	/** IA JS multi-fichiers : l'entree utilise des modules ES (import/export en debut de ligne). */
	private static boolean usesEsModules(String languageId, String source) {
		return "js".equals(languageId) && ES_MODULE.matcher(source).find();
	}

	/**
	 * Detecte le langage polyglot a partir de l'extension du fichier (null = LeekScript).
	 * Comparaison insensible a la casse : le serveur et le client classent .JS/.Py via strtolower /
	 * toLowerCase (sauter le daemon LeekScript), donc le moteur doit reconnaitre la meme chose,
	 * sinon un "Bot.JS" serait sauve comme polyglot mais compile ici comme LeekScript (erreur muette).
	 */
	public static String detectLanguage(String path) {
		if (path == null) {
			return null;
		}
		String lower = path.toLowerCase();
		// .mjs (module ES explicite) accepte au meme titre que .js.
		if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
			return "js";
		}
		// TypeScript : pas de moteur guest "ts" -> transpile en JS au build, execute par le moteur js.
		if (isTypeScript(path)) {
			return "js";
		}
		if (lower.endsWith(".py")) {
			return "python";
		}
		return null;
	}

	/** true si le fichier est du TypeScript (a transpiler en JS avant execution). Source unique : {@link #tsAlias}. */
	public static boolean isTypeScript(String path) {
		return tsAlias(path) != null;
	}

	/** Probleme de syntaxe d'une IA polyglot : message + localisation (lignes 1-based, colonnes 0-based,
	 *  pour coller a la convention des "problems" LeekScript que le client decale de +1). */
	public static final class SyntaxProblem {
		public final String message;
		public final int startLine, startColumn, endLine, endColumn;
		private SyntaxProblem(String message, int startLine, int startColumn, int endLine, int endColumn) {
			this.message = message;
			this.startLine = startLine;
			this.startColumn = startColumn;
			this.endLine = endLine;
			this.endColumn = endColumn;
		}
		static SyntaxProblem from(PolyglotException e) {
			String msg = e.getMessage() != null ? e.getMessage() : "syntax error";
			SourceSection loc = e.getSourceLocation();
			if (loc != null && loc.isAvailable()) {
				// GraalVM expose des colonnes 1-based ; on revient en 0-based (le client rajoute +1).
				return new SyntaxProblem(msg, loc.getStartLine(), Math.max(0, loc.getStartColumn() - 1),
						loc.getEndLine(), Math.max(0, loc.getEndColumn() - 1));
			}
			return new SyntaxProblem(msg, 1, 0, 1, 0); // localisation inconnue -> debut du fichier
		}
		/**
		 * Probleme localise par un offset caractere dans le source (convention des diagnostics tsc).
		 * On derive ligne (1-based) / colonne (0-based) en parcourant le source. start &lt; 0 -&gt; debut.
		 */
		static SyntaxProblem atOffset(String source, String message, int start, int length) {
			if (start < 0 || source == null) {
				return new SyntaxProblem(message, 1, 0, 1, 0);
			}
			int line = 1, col = 0;
			int max = source.length();
			for (int i = 0; i < start && i < max; i++) {
				if (source.charAt(i) == '\n') { line++; col = 0; } else { col++; }
			}
			int endLine = line, endCol = col;
			for (int i = start; i < start + length && i < max; i++) {
				if (source.charAt(i) == '\n') { endLine++; endCol = 0; } else { endCol++; }
			}
			return new SyntaxProblem(message, line, col, endLine, endCol);
		}
	}

	/**
	 * Valide la SYNTAXE d'une IA polyglot sans l'executer ({@code context.parse}). Renvoie null si OK,
	 * sinon le probleme (message + localisation). Reutilisable : le build de combat l'appelle (parite
	 * LeekScript), et le daemon d'analyse l'appelle pour remonter les erreurs a l'editeur.
	 *
	 * Une entree JS module (import/export en tete) n'est PAS parsable comme un script (l'import est
	 * illegal hors module) -> on ne valide pas (null), les erreurs remonteront au chargement du module.
	 * Le contexte sonde est cree puis ferme ET retire du suivi du sandbox (pas de retention).
	 */
	public static SyntaxProblem validateSyntax(String languageId, String source, PolyglotSandbox sandbox) {
		if (usesEsModules(languageId, source)) {
			return null;
		}
		Context probe = sandbox.createContext(languageId);
		try {
			probe.parse(languageId, source);
			return null;
		} catch (PolyglotException e) {
			return SyntaxProblem.from(e);
		} finally {
			try {
				probe.close();
			} catch (Exception ignore) {
				// best effort
			}
			sandbox.forgetContext(probe);
		}
	}

	/**
	 * Validation de syntaxe orientee FICHIER (pour les diagnostics editeur via le daemon) : route selon
	 * l'extension. TypeScript -&gt; diagnostics tsc (transpilation) ; JS/Python -&gt; parse GraalVM ;
	 * extension non polyglot -&gt; null (rien a signaler). Renvoie null si OK, sinon le 1er probleme.
	 */
	public static SyntaxProblem validateSyntaxForFile(String path, String source, PolyglotSandbox sandbox) {
		if (isTypeScript(path)) {
			return TypeScriptTranspiler.transpile(source, path).firstDiagnostic();
		}
		String lang = detectLanguage(path);
		if (lang == null) {
			return null;
		}
		return validateSyntax(lang, source, sandbox);
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
			PolyglotFileSystem fs = buildFileSystem(file, languageId);

			String source = file.getCode();
			if (isTypeScript(file.getPath())) {
				// TypeScript : on efface les types au build ; l'artefact JS repart dans le pipeline
				// polyglot identique (memes gardes). Les diagnostics tsc = erreur joueur -> IA invalide.
				TypeScriptTranspiler.Result tr = TypeScriptTranspiler.transpile(source, file.getPath());
				if (!tr.ok()) {
					SyntaxProblem p = tr.firstDiagnostic();
					((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.INVALID_AI, new String[] { p.message });
					return new EntityAI(entity, (LeekLog) entity.getLogs());
				}
				source = tr.js;
			} else {
				// Validation syntaxique au build (parite avec LeekScript). Une erreur de parse vient du code
				// joueur -> IA invalide (erreur utilisateur), jamais le chemin "erreur serveur" du catch externe.
				SyntaxProblem problem = validateSyntax(languageId, source, sandbox);
				if (problem != null) {
					((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.INVALID_AI, new String[] { problem.message });
					return new EntityAI(entity, (LeekLog) entity.getLogs());
				}
			}

			// TS : l'entree est importee via son alias .js/.mjs (graaljs charge un module par extension).
			String entryPath = isTypeScript(file.getPath()) ? tsAlias(file.getPath()) : file.getPath();
			PolyglotEntityAI ai = new PolyglotEntityAI(languageId, source, entryPath, fs, sandbox);
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
	private static PolyglotFileSystem buildFileSystem(AIFile entry, String languageId) {
		// Python : on delegue la stdlib GraalPy (sinon un FS nu la casserait). Si on ne sait pas la
		// localiser, on renonce au FS pour Python (mono-fichier, stdlib lue en interne).
		Path passthrough = null;
		if ("python".equals(languageId)) {
			passthrough = PolyglotSandbox.pythonStdlibRoot();
			if (passthrough == null) {
				return null;
			}
		}
		final Path pass = passthrough;

		// Enumere les fichiers du joueur + lecture paresseuse (chemin LeekScript -> contenu).
		Set<String> realPaths = new HashSet<>();
		realPaths.add(entry.getPath());
		Function<String, String> rawRead;
		try {
			var lfs = LeekScript.getFileSystem();
			int owner = entry.getOwner();
			var root = lfs.getRoot(owner);
			for (AIFile f : lfs.listAllFiles(owner)) {
				realPaths.add(f.getPath());
			}
			rawRead = path -> {
				try {
					AIFile f = root.resolve(path);
					return f != null ? f.getCode() : null;
				} catch (Exception e) {
					return null;
				}
			};
		} catch (Exception e) {
			// Enumeration impossible (FS sans listing, owner inconnu...) : on retombe sur le seul
			// fichier d'entree (mono-fichier). Jamais une erreur serveur pour autant.
			realPaths.clear();
			realPaths.add(entry.getPath());
			rawRead = path -> {
				try {
					return entry.getPath().equals(path) ? entry.getCode() : null;
				} catch (Exception ex) {
					return null;
				}
			};
		}

		if (!isTypeScript(entry.getPath())) {
			return new PolyglotFileSystem(realPaths, rawRead, pass);
		}

		// TypeScript multi-fichiers : on transpile chaque .ts/.mts a la lecture, et on expose un alias
		// .js/.mjs pour chaque .ts/.mts (l'usage TS importe en .js/.mjs meme quand le voisin est en .ts).
		// L'import extensionless n'est pas supporte (comme Node ESM) : utiliser une extension explicite.
		final Map<String, String> aliasToReal = new HashMap<>();
		Set<String> mounted = new HashSet<>(realPaths);
		for (String p : realPaths) {
			String alias = tsAlias(p);
			if (alias != null && !realPaths.contains(alias)) {
				mounted.add(alias);
				aliasToReal.put(alias, p);
			}
		}
		final Function<String, String> baseRead = rawRead;
		// Memoisation par fichier : le module loader peut relire un meme fichier (resolution + lecture,
		// voisin importe par plusieurs fichiers), on ne le transpile qu'une fois par chargement d'IA.
		final Map<String, String> transpileCache = new HashMap<>();
		Function<String, String> tsRead = path -> {
			String real = aliasToReal.getOrDefault(path, path);
			String cached = transpileCache.get(real);
			if (cached != null) {
				return cached;
			}
			String code = baseRead.apply(real);
			if (code == null) {
				return null;
			}
			String result;
			if (isTypeScript(real)) {
				TypeScriptTranspiler.Result tr = TypeScriptTranspiler.transpile(code, real);
				// Erreur de compilation dans un fichier importe : on emet un module qui leve, pour que la
				// rejection d'import() remonte le diagnostic (sinon un outputText partiel le masquerait).
				result = tr.ok() ? tr.js
						: "throw new Error(" + jsStringLiteral(real + ": " + tr.firstDiagnostic().message) + ");";
			} else {
				result = code;
			}
			transpileCache.put(real, result);
			return result;
		};
		return new PolyglotFileSystem(mounted, tsRead, pass);
	}

	/** Encode une chaine en litteral JS entre guillemets (pour injecter un message dans un module de fallback). */
	private static String jsStringLiteral(String s) {
		StringBuilder b = new StringBuilder("\"");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"': b.append("\\\""); break;
				case '\\': b.append("\\\\"); break;
				case '\n': b.append("\\n"); break;
				case '\r': b.append("\\r"); break;
				default: b.append(c);
			}
		}
		return b.append('"').toString();
	}

	/** Alias module JS d'un fichier TS : foo.ts -&gt; foo.js, foo.mts -&gt; foo.mjs ; null si pas du TS. */
	private static String tsAlias(String path) {
		if (path == null) {
			return null;
		}
		String lower = path.toLowerCase();
		if (lower.endsWith(".mts")) {
			return path.substring(0, path.length() - 4) + ".mjs";
		}
		if (lower.endsWith(".ts")) {
			return path.substring(0, path.length() - 3) + ".js";
		}
		return null;
	}

	private void ensureContext() {
		if (context == null) {
			// Budgets calques sur le pipeline LeekScript (cf EntityAI.build) : RAM pour les
			// LeekValue alloues cote hote, ops pour borner le travail hote des fonctions de combat.
			setMaxRAM(Math.min(50, mEntity.getRAM()) * 8_000_000);
			setMaxOperations((int) Math.min(Integer.MAX_VALUE, (long) mEntity.getCores() * 1_000_000));
			// Multi-fichiers : le FS sert les fichiers du joueur (et, pour Python, delegue la stdlib
			// GraalPy en lecture seule). fileSystem peut etre null (mono-fichier, ou Python sans
			// stdlib localisable) -> contexte sans FS (stdlib lue en interne).
			// Cap RAM RETENUE du guest (par poireau, sandbox.MaxHeapMemory, en OCTETS) = budget RAM
			// de l'entite DIVISE par RAM_FACTOR : a cap brut egal le guest retient ~3.8x plus de
			// donnees que le mRAM LeekScript (TestRamCalibration) -> on divise pour la parite (cf
			// RAM_FACTOR). Plancher a 8 Mo pour ne jamais etouffer un contexte legitime.
			long guestRamCap = Math.max(8_000_000L, (long) (getMaxRAM() / RAM_FACTOR));
			context = sandbox.createContext(languageId, fileSystem, guestRamCap);
			PolyglotAPIBridge.install(context, languageId, this);
			installDeterminismGuards();
			// Compteur de statements deterministe de l'image custom, lie a CE contexte (refetche
			// apres chaque reconstruction). null si l'image ne l'embarque pas -> temps CPU.
			statementCounter = PolyglotSandbox.statementCounterBinding(context);
			externalIsolate = sandbox.isExternalIsolate(languageId);
			// Redirige getOperations() cote guest (perf : evite ~100k+ aller-retours hote/tour sur une
			// IA de recherche). Seulement avec l'instrument deterministe ; sinon le getOperations HOTE
			// (fallback temps CPU) reste en place.
			guestBindings = context.getBindings(languageId);
			if (statementCounter != null) {
				installGuestGetOperations();
			} else {
				guestBindings = null;
			}
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
		+ "L.now=function(){return F;};L.parse=D.parse;L.UTC=D.UTC;L.prototype=D.prototype;"
		// On scelle aussi D.prototype.constructor -> L : sinon (new Date()).constructor et Date.prototype.constructor
		// pointaient encore sur le Date d'origine, permettant de recuperer l'horloge reelle via .constructor.now().
		+ "Object.defineProperty(D.prototype,'constructor',{value:L,writable:false,configurable:false});"
		+ "globalThis.Date=L;})();"
		+ "if(typeof performance!=='undefined'){performance.now=function(){return 0;};}";

	/**
	 * FACTURATION DU TRAVAIL NATIF (JS). Les methodes natives de Array/String (sort, fill, repeat...)
	 * font O(N) de travail mais emettent ~0 statement -> l'instrument ne les voit pas (faille de
	 * fairness : LeekScript, lui, facture ses builtins). On les enveloppe pour facturer via
	 * {@code __lw_charge} au prorata de la taille traitee (le facteur de calibration par langage est
	 * applique cote hote dans {@link #chargeProxy}). On N'enveloppe PAS map/filter/reduce/forEach (leur
	 * callback guest est deja compte par element) ni indexOf/includes (court-circuit : facturer la
	 * longueur entiere sur-penaliserait un hit precoce). Deux cadences de facturation : longueur du
	 * RECEVEUR pour les parcours complets (sort/fill/reverse/join/copyWithin/splice), longueur du
	 * RESULTAT pour les copies (slice/concat/flat/from/repeat), facturee APRES pour rester exacte.
	 *
	 * <p>Accumulateur PRIVE (closure, hors globalThis -> le joueur ne peut pas le remettre a zero) :
	 * on n'appelle {@code __lw_charge} (traversee hote couteuse sous isolate) que tous les {@code FLUSH}
	 * unites, pas a chaque appel -> pas de taxe de frontiere sur les IA qui font beaucoup de petits
	 * appels. Un appel massif (ex fill(1e9)) franchit FLUSH d'un coup et facture immediatement.
	 * Wraps SCELLES (writable/configurable false, comme les gardes de determinisme).
	 */
	private static final String JS_CHARGE_GUARD =
		"(function(){var C=globalThis.__lw_charge;if(!C)return;"
		+ "var acc=0;var FLUSH=4096;function ch(n){acc+=n;if(acc>=FLUSH){C(acc);acc=0;}}"
		// parcours complet -> cout = longueur du receveur (facture AVANT le travail)
		+ "function wrapLen(name){var o=Array.prototype[name];if(!o)return;"
		+ "Object.defineProperty(Array.prototype,name,{value:function(){ch(this.length>>>0);return o.apply(this,arguments);},writable:false,configurable:false});}"
		+ "['sort','fill','reverse','join','copyWithin','splice'].forEach(wrapLen);"
		// copie -> cout = longueur du RESULTAT (facture APRES, exact)
		+ "function wrapRes(name){var o=Array.prototype[name];if(!o)return;"
		+ "Object.defineProperty(Array.prototype,name,{value:function(){var r=o.apply(this,arguments);ch((r&&r.length)>>>0);return r;},writable:false,configurable:false});}"
		+ "['slice','concat','flat'].forEach(wrapRes);"
		+ "var AF=Array.from;if(AF)Object.defineProperty(Array,'from',{value:function(){var r=AF.apply(Array,arguments);ch((r&&r.length)>>>0);return r;},writable:false,configurable:false});"
		+ "var RP=String.prototype.repeat;if(RP)Object.defineProperty(String.prototype,'repeat',{value:function(){var r=RP.apply(this,arguments);ch(r.length>>>0);return r;},writable:false,configurable:false});"
		+ "})();";

	/**
	 * FACTURATION DU TRAVAIL NATIF (Python). Enveloppe les builtins qui PARCOURENT tout leur argument
	 * (sum, sorted, list, min, max...) pour facturer via {@code __lw_charge} au prorata de {@code len}
	 * (le facteur de calibration est applique cote hote, cf {@link #chargeProxy}). On ne facture que la
	 * FORME iterable (1 argument positionnel dont on peut prendre len) : la forme multi-args (min(a,b))
	 * est O(1). On N'enveloppe PAS all/any (court-circuit : facturer len sur-penaliserait). Un
	 * generateur sans len n'est pas facture ici mais son corps est deja compte par l'instrument. Les
	 * operateurs ({@code 'x'*N}, {@code 10**N}) restent non factures mais sont bornes par le cap RAM +
	 * wall-clock. Accumulateur PRIVE + flush (idem JS). Contournable par reflexion (idem JS).
	 */
	private static final String PY_CHARGE_GUARD =
		"import builtins as _lwb\n"
		+ "def _lw_install():\n"
		+ "    _acc = [0]\n"
		+ "    _FLUSH = 4096\n"
		+ "    _C = __lw_charge\n"
		+ "    def _ch(n):\n"
		+ "        _acc[0] += n\n"
		+ "        if _acc[0] >= _FLUSH:\n"
		+ "            _C(_acc[0]); _acc[0] = 0\n"
		+ "    def _wrap(_orig):\n"
		+ "        def _f(*a, **k):\n"
		+ "            if len(a) == 1:\n"
		+ "                try:\n"
		+ "                    _ch(len(a[0]))\n"
		+ "                except TypeError:\n"
		+ "                    pass\n"
		+ "            return _orig(*a, **k)\n"
		+ "        return _f\n"
		+ "    for _n in ('sum','sorted','list','tuple','set','frozenset','min','max','dict'):\n"
		+ "        try:\n"
		+ "            setattr(_lwb, _n, _wrap(getattr(_lwb, _n)))\n"
		+ "        except Exception:\n"
		+ "            pass\n"
		+ "_lw_install()\n";

	// graaljs fournit bien `console`, mais sa sortie part dans le nullOutputStream du sandbox (jetee) :
	// une IA qui fait console.log ne verrait donc RIEN. On reroute console.* vers debug() (le log de
	// combat, visible dans le rapport, avec ses propres limites anti-spam) -> les IA JS/TS peuvent
	// deboguer avec le console.log familier. debug() (LeekScript) reste aussi disponible directement.
	private static final String JS_CONSOLE_SETUP =
		"(function(){"
		// try/catch autour de debug() : il peut lever (ex: contexte de combat incomplet) ; on ne veut
		// JAMAIS que console.log fasse echouer l'IA. Visible quand debug marche, silencieux sinon.
		+ "var L=function(){try{debug(Array.prototype.map.call(arguments,String).join(' '));}catch(e){}};"
		+ "try{globalThis.console={log:L,info:L,debug:L,warn:L,error:L};}"
		+ "catch(e){try{console.log=L;console.info=L;console.debug=L;console.warn=L;console.error=L;}catch(e2){}}"
		+ "})();";

	// API de combat orientee objet (me, Entity, Cell, Fight...) : couche guest au-dessus de l'API plate,
	// chargee une fois depuis les resources et evaluee dans chaque contexte JS apres le bridge. Style LS5.
	private static final String JS_OBJECT_API = loadResource("/polyglot/objects.js");
	private static final String PY_OBJECT_API = loadResource("/polyglot/objects.py");

	private static String loadResource(String path) {
		try (InputStream in = PolyglotEntityAI.class.getResourceAsStream(path)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * OPTIM getOperations COTE GUEST. Une IA de recherche appelle {@code getOperations()} des dizaines
	 * de milliers de fois par tour pour borner son alpha-beta ; en tant que fonction HOTE bridgee, chaque
	 * appel = un aller-retour hote + une lecture du compteur de statements dans l'isolate (~2,4 us,
	 * profilé a ~360 ms/tour sur le port Quantum). On redirige donc {@code getOperations} vers une
	 * implementation GUEST : {@code __lw_real + __lw_counter() * facteur}, ou {@code __lw_counter} est le
	 * compteur de statements lu LOCALEMENT (il vit dans l'isolate, cote guest -> pas d'aller-retour) et
	 * {@code __lw_real} est un miroir des ops HOTE (mOperations), rafraichi par l'hote apres chaque appel
	 * de fonction de combat / facturation (cf {@link #syncRealToGuest}). Deterministe et identique a
	 * l'implementation hote (meme formule) -> replays preserves. Actif seulement si l'instrument est la.
	 */
	private static final String JS_GETOPS_OVERRIDE =
		"(function(){var f=$F;var c=globalThis.__lw_counter;"
		+ "var g=function(){return (globalThis.__lw_real||0)+c()*f;};"
		+ "globalThis.getOperations=g;globalThis.getOperation=g;})();";
	private static final String PY_GETOPS_OVERRIDE =
		"import builtins as _lwb2\n"
		+ "def getOperations():\n"
		+ "    return __lw_real + __lw_counter() * $F\n"
		+ "_lwb2.getOperations = getOperations\n";

	/** Fetch les bindings guest + redirige getOperations cote guest (si l'instrument deterministe est la). */
	private void installGuestGetOperations() {
		try {
			guestBindings.putMember("__lw_counter", statementCounter);
			guestBindings.putMember("__lw_real", super.getOperations());
			String src = ("js".equals(languageId) ? JS_GETOPS_OVERRIDE : PY_GETOPS_OVERRIDE)
					.replace("$F", String.valueOf(opsFactor));
			context.eval(languageId, src);
		} catch (Exception e) {
			// Best effort : si l'override echoue, le getOperations HOTE (bridge) reste en place.
			guestBindings = null;
			Log.w("PolyglotEntityAI", "getOperations guest-side indisponible (" + languageId + ") : " + e.getMessage());
		}
	}

	/**
	 * Rafraichit le miroir guest {@code __lw_real} des operations HOTE. Appele apres chaque appel de
	 * fonction de combat (cf PolyglotAPIBridge) et de facturation, et en debut de tour (remise a 0) :
	 * mOperations ne change QUE dans {@code ops()} (fonctions de combat + charge), donc entre deux tels
	 * appels le miroir est exact -> les milliers de getOperations() intermediaires lisent une valeur juste.
	 */
	void syncRealToGuest() {
		if (guestBindings != null && statementCounter != null) {
			try { guestBindings.putMember("__lw_real", super.getOperations()); } catch (Exception ignore) {}
		}
	}

	/**
	 * Fonction hote {@code __lw_charge(n)} exposee au guest : facture {@code n} operations sur le
	 * compteur reel (via {@link leekscript.runner.AI#ops(int)}, qui jette {@code TOO_MUCH_OPERATIONS}
	 * au dela du budget). Sert au comptage du travail NATIF/builtin que l'instrument (granularite
	 * statement) ne voit pas : les preludes JS_CHARGE_GUARD / PY_CHARGE_GUARD enveloppent les builtins
	 * couteux (sort, fill, sum, sorted, list...) pour facturer au prorata de la taille d'entree, comme
	 * LeekScript facture ses propres builtins. Deterministe (la taille l'est). L'exception d'ops est
	 * emballee en RuntimeException -> deballee par runIA via {@link #unwrapLeekRunException}.
	 */
	private ProxyExecutable chargeProxy() {
		return (Value... args) -> {
			if (args.length > 0) {
				long n;
				try { n = args[0].fitsInLong() ? args[0].asLong() : 0L; } catch (Exception e) { n = 0L; }
				if (n > 0) {
					// Le travail natif est du travail GUEST : on le scale par opsFactor comme les
					// statements (cf getOperations), sinon un builtin resterait opsFactor x moins cher
					// qu'une boucle explicite equivalente.
					long scaled = (long) Math.min((double) n * opsFactor, Integer.MAX_VALUE);
					if (scaled > 0) {
						try {
							ops((int) scaled);
							syncRealToGuest(); // la charge a bouge mOperations -> rafraichir le miroir guest
						} catch (LeekRunException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			return 0L;
		};
	}

	/**
	 * Neutralise les sources de non-determinisme atteignables par le guest, sinon les IA JS/Python
	 * ne seraient pas reproductibles a partir de la seed du combat (re-simulation / verification) :
	 * generateur aleatoire seede et fige, et horloge murale fixe.
	 */
	private void installDeterminismGuards() {
		if ("js".equals(languageId)) {
			context.getBindings(languageId).putMember("__lw_random", (ProxyExecutable) args -> getRandom().getDouble());
			context.getBindings(languageId).putMember("__lw_charge", chargeProxy());
			context.eval(languageId, JS_DETERMINISM_GUARD);
			context.eval(languageId, JS_CHARGE_GUARD);
			context.eval(languageId, JS_CONSOLE_SETUP);
			if (JS_OBJECT_API != null) {
				context.eval(languageId, JS_OBJECT_API);
			}
		} else if ("python".equals(languageId)) {
			// Plage bornee a l'int : getLong caste en int et un (max-min+1) qui overflow renvoie 0.
			long seed = getRandom().getLong(0, Integer.MAX_VALUE - 1);
			context.getBindings(languageId).putMember("__lw_charge", chargeProxy());
			context.eval(languageId, pythonDeterminismGuard(seed));
			context.eval(languageId, PY_CHARGE_GUARD);
			if (PY_OBJECT_API != null) {
				context.eval(languageId, PY_OBJECT_API);
			}
		}
	}

	/**
	 * Garde Python : seede random, ET re-route toutes les sources d'entropie OS (os.urandom,
	 * SystemRandom, uuid4, random.seed() sans argument) vers un PRNG seede, et fige l'horloge.
	 * Sans cela un simple {@code import os; os.urandom(1)} contournerait silencieusement la seed.
	 */
	private static String pythonDeterminismGuard(long seed) {
		return
			// Multi-fichiers : le montage /ai est ajoute a sys.path ICI (l'option python.PythonPath
			// est TRUSTED-only, rejetee par la policy ISOLATED), en FIN de path pour que la stdlib
			// garde la PRIORITE sur les fichiers du joueur. Sinon une IA nommant un fichier
			// random.py / os.py masquerait la stdlib (et le RNG seede).
			"import sys\n"
			+ "if '/ai' in sys.path:\n    sys.path.remove('/ai')\n"
			+ "sys.path.append('/ai')\n"
			+ "import os, random, uuid, time, datetime\n"
			+ "_lw_r = random.Random(" + seed + ")\n"
			+ "os.urandom = lambda n: bytes(_lw_r.getrandbits(8) for _ in range(n))\n"
			+ "random.SystemRandom = random.Random\n"
			+ "_lw_seed = random.seed\n"
			+ "def _lw_seed_guard(a=None, *ar, **kw):\n    return _lw_seed(" + seed + " if a is None else a, *ar, **kw)\n"
			+ "random.seed = _lw_seed_guard\n"
			+ "random.seed(" + seed + ")\n"
			+ "uuid.uuid4 = lambda: uuid.UUID(int=_lw_r.getrandbits(128))\n"
			+ "time.time = lambda: 0.0\ntime.monotonic = lambda: 0.0\ntime.perf_counter = lambda: 0.0\n"
			+ "time.time_ns = lambda: 0\ntime.monotonic_ns = lambda: 0\ntime.perf_counter_ns = lambda: 0\n"
			+ "time.gmtime = lambda secs=None: time.struct_time((2020, 1, 1, 0, 0, 0, 2, 1, 0))\n"
			+ "time.localtime = lambda secs=None: time.struct_time((2020, 1, 1, 0, 0, 0, 2, 1, 0))\n"
			+ "try:\n"
			+ "    datetime.datetime.now = classmethod(lambda cls, tz=None: datetime.datetime(2020, 1, 1))\n"
			+ "    datetime.datetime.today = classmethod(lambda cls: datetime.datetime(2020, 1, 1))\n"
			+ "    datetime.datetime.utcnow = classmethod(lambda cls: datetime.datetime(2020, 1, 1))\n"
			+ "except TypeError:\n"
			// Image avec accelerateur natif _datetime : la classe est IMMUABLE ("cannot set 'now'
			// attribute of immutable type") -> on la REMPLACE par une sous-classe figee, et on
			// redirige le module _datetime vers le module patche (sinon `import _datetime`
			// re-exposerait l'horloge reelle).
			+ "    _lw_dt = datetime.datetime\n"
			+ "    class _LWDateTime(_lw_dt):\n"
			+ "        @classmethod\n"
			+ "        def now(cls, tz=None):\n            return _lw_dt(2020, 1, 1)\n"
			+ "        @classmethod\n"
			+ "        def today(cls):\n            return _lw_dt(2020, 1, 1)\n"
			+ "        @classmethod\n"
			+ "        def utcnow(cls):\n            return _lw_dt(2020, 1, 1)\n"
			+ "    datetime.datetime = _LWDateTime\n"
			+ "    sys.modules['_datetime'] = sys.modules['datetime']\n";
	}

	@Override
	public Object runIA(Session session) throws LeekRunException {
		if (disabled) {
			return null; // IA neutralisee (trop de depassements wall-clock) : n'agit plus, ne consomme plus.
		}
		markTurnStart(); // bases (temps mur + temps CPU) du terme d'ops synthetique, cf getOperations
		ensureContext();
		resetStatementCounter(); // compteur de statements guest remis a zero a chaque tour (terme deterministe)
		syncRealToGuest(); // remet a 0 le miroir __lw_real (mOperations vient d etre reset par runTurn)
		// Budget de statements par tour : le contexte est reutilise entre tours (etat statique
		// guest persistant) mais le statement limit GraalVM est cumulatif sur la vie du contexte.
		context.resetLimits();

		// Garde-fou wall-clock : un tour qui depasse l'echeance (typiquement du travail natif que le
		// statement limit ne compte pas, ex: 'x'*10**9) est coupe. Le watchdog et le tour se disputent un
		// UNIQUE CAS, resolu dans le finally (donc pour TOUTE sortie : succes, erreurs, ET Error non
		// capturee comme StackOverflow). Invariants :
		//   - interruptAsync n'est declenche QUE si le watchdog gagne le CAS ;
		//   - si le watchdog gagne, le tour PERD le CAS dans le finally et passe par onWallClockTimeout(),
		//     qui FERME le contexte -> le tour suivant en reconstruit un neuf. Donc l'interruption asynchrone
		//     (qui vise l'ancien `running`) ne peut JAMAIS toucher le contexte d'un tour suivant ;
		//   - si le tour gagne, le watchdog ne declenche jamais rien (son CAS echoue) et on l'annule.
		final Context running = context;
		final AtomicBoolean settled = new AtomicBoolean(false);
		final ScheduledFuture<?> watchdog = PolyglotSandbox.scheduleDeadline(() -> {
			if (settled.compareAndSet(false, true)) {
				PolyglotSandbox.interruptAsync(running);
			}
		}, turnWallClockLimitMs);
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
			// Peut lever LeekRunException (marshalling du retour : ops/profondeur) : resolue par le finally.
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
		} finally {
			// Resolution UNIQUE de la course, pour TOUTE sortie du tour : succes, exception verifiee
			// (LeekRunException du marshalling/chargement, non capturee ci-dessus), erreur hote, ET
			// java.lang.Error (StackOverflow...) hors taxonomie des catch. Le tour gagne -> on annule le
			// watchdog. Le WATCHDOG gagne -> il a interrompu `running` ; onWallClockTimeout() FERME le
			// contexte (le tour suivant en reconstruit un neuf, donc l'interruption ne touche aucun tour
			// suivant) et SUBSTITUE un depassement a l'issue du tour. Le throw depuis le finally n'est PAS
			// re-capturable par les catch ci-dessus -> pas de double comptage.
			if (!winRace(settled, watchdog)) {
				throw onWallClockTimeout();
			}
		}
	}

	/**
	 * Resout la course tour/watchdog en fin de tour (appelee depuis le finally de {@link #runIA}). Renvoie
	 * true si LE TOUR gagne (l'echeance n'a pas encore expire) : on annule alors le watchdog (il ne
	 * declenchera rien). Renvoie false si le WATCHDOG a deja gagne (echeance expiree, interruption en
	 * cours) -&gt; l'appelant doit traiter un depassement.
	 */
	private static boolean winRace(AtomicBoolean settled, ScheduledFuture<?> watchdog) {
		if (settled.compareAndSet(false, true)) {
			watchdog.cancel(false);
			return true;
		}
		return false;
	}

	/**
	 * Tour coupe par le watchdog wall-clock. On ferme le contexte (repart neuf au prochain tour) et,
	 * apres trop de depassements sur le combat, on neutralise l'IA pour borner le cout total.
	 */
	private LeekRunException onWallClockTimeout() {
		closeContext();
		if (++wallClockTimeouts >= MAX_WALL_CLOCK_TIMEOUTS) {
			disabled = true;
			Log.w("PolyglotEntityAI", "IA polyglot (" + languageId + ") neutralisee apres " + wallClockTimeouts
				+ " depassements wall-clock (" + turnWallClockLimitMs + " ms/tour) : probable travail natif non borne par le statement limit");
		}
		return new LeekRunException(Error.TOO_MUCH_OPERATIONS);
	}

	/**
	 * Execute une fonction guest (callback de summon, cf {@link com.leekwars.generator.fight.entity.BulbAI})
	 * avec LES MEMES gardes par-tour que {@link #runIA} : reset du compteur de statements + des limites du
	 * contexte, backstop wall-clock, et mapping des exceptions guest en {@link LeekRunException}
	 * (closeContext defensif). Le bulbe ne passe PAS par runIA ; sans ces gardes, le callback contournerait
	 * le statement limit (cumulatif sur le contexte de l'invocateur -&gt; epuiserait un tour ULTERIEUR de
	 * l'invocateur) et le backstop wall-clock (un travail natif lourd bloquerait le worker). Appelee par le
	 * wrapper de {@link TypeMarshaller#wrapGuestFunction} via {@code mAIFunction.run(mOwnerAI, ...)} : a ce
	 * moment {@code mEntity} pointe deja sur le bulbe (BulbAI.runIA), donc {@code getEntity()} (et {@code me}
	 * une fois rendu dynamique) renvoie le bulbe.
	 *
	 * @param fn        la fonction guest a rejouer (liee au contexte de l'invocateur).
	 * @param guestArgs arguments deja marshalles Java -&gt; guest (null/vide pour le callback de summon).
	 * @return la valeur de retour marshallee guest -&gt; Java, ou {@code null} si l'IA est neutralisee, le
	 *         contexte ferme, ou le callback n'est plus executable (degradation gracieuse : le bulbe cesse
	 *         d'agir, le combat continue).
	 */
	Object runGuestCallback(Value fn, Object[] guestArgs) throws LeekRunException {
		if (disabled || context == null || fn == null || !fn.canExecute()) {
			return null;
		}
		markTurnStart();
		resetStatementCounter();
		syncRealToGuest();
		// Contexte reutilise entre tours (statement limit cumulatif) : on remet le budget a zero pour le
		// tour du bulbe, exactement comme runIA pour un tour normal.
		context.resetLimits();

		final Context running = context;
		final AtomicBoolean settled = new AtomicBoolean(false);
		final ScheduledFuture<?> watchdog = PolyglotSandbox.scheduleDeadline(() -> {
			if (settled.compareAndSet(false, true)) {
				PolyglotSandbox.interruptAsync(running);
			}
		}, turnWallClockLimitMs);
		try {
			// HostAccess.NONE ne gene PAS un appel host -> guest d'une Value executable (il ne gate que
			// l'acces du guest aux objets hote) ; cf entry.execute() de runIA, meme contexte.
			Value value = (guestArgs == null || guestArgs.length == 0) ? fn.execute() : fn.execute(guestArgs);
			return TypeMarshaller.toJava(value, this);
		} catch (PolyglotException e) {
			throw mapException(e);
		} catch (RuntimeException e) {
			LeekRunException unwrapped = unwrapLeekRunException(e);
			throw unwrapped != null ? unwrapped : new LeekRunException(Error.AI_INTERRUPTED, new String[] { String.valueOf(e.getMessage()) });
		} finally {
			if (!winRace(settled, watchdog)) {
				throw onWallClockTimeout();
			}
		}
	}

	/** Regle le backstop wall-clock par tour (defaut {@value #DEFAULT_TURN_WALL_CLOCK_LIMIT_MS} ms). */
	public void setTurnWallClockLimitMs(long ms) {
		this.turnWallClockLimitMs = ms;
		this.opsBudgetNanos = (long) (ms * 1_000_000L * OPS_TIME_BUDGET_FRACTION);
	}

	/**
	 * Compteur d'operations expose au guest, augmente d'un terme SYNTHETIQUE base sur le temps ecoule
	 * dans le tour.
	 *
	 * <p>En LeekScript natif, chaque statement incremente le compteur : une IA de recherche (alpha-beta,
	 * etc.) borne son exploration avec {@code getOperations() > seuil}. Sous le runtime polyglot, le
	 * calcul pur guest (boucles JS/Python) n'incremente PAS {@code mOperations} (seul le travail hote des
	 * fonctions de combat appelle {@code ops()}). Sans correctif, la garde de l'IA ne se declenche jamais,
	 * l'exploration sature les 5 s du backstop wall-clock et l'IA finit neutralisee sans avoir agi.
	 *
	 * <p>On surface donc un budget proportionnel au TEMPS : il atteint {@link #getMaxOperations()} a
	 * {@link #OPS_TIME_BUDGET_FRACTION} du budget wall-clock du tour. La garde de l'IA se declenche ainsi
	 * naturellement, bien avant le backstop dur, et l'IA rend sa meilleure action. L'enforcement reel du
	 * budget (jet de {@code TOO_MUCH_OPERATIONS} dans {@code ops()}) reste base sur {@code mOperations}
	 * brut : ce terme synthetique ne sert qu'a la LECTURE par le guest.
	 *
	 * <p>NB determinisme : comme le backstop wall-clock, ce terme depend de la charge machine ; une IO de
	 * recherche polyglot n'est donc pas bit-reproductible entre machines. Acceptable pour faire tourner /
	 * terminer des combats ; a affiner (compteur de statements guest) pour des combats classes.
	 */
	/**
	 * Remet a zero le compteur de statements deterministe (execute(x) = reset, cf StatementCounter).
	 * Si le contexte est mort (annule/ferme), on abandonne le compteur pour ce contexte : repli sur le
	 * terme temps CPU (ensureContext refetchera un binding neuf avec le prochain contexte).
	 */
	private void resetStatementCounter() {
		if (statementCounter == null) {
			return;
		}
		try {
			statementCounter.execute(0);
		} catch (Exception e) {
			statementCounter = null;
		}
	}

	/** Pose les bases (temps mur + temps CPU du thread de combat) du terme d'ops synthetique, en debut de tour. */
	private void markTurnStart() {
		turnStartNanos = System.nanoTime();
		turnThreadId = Thread.currentThread().threadId();
		turnStartCpuNanos = CPU_TIME_SUPPORTED ? THREAD_MX.getThreadCpuTime(turnThreadId) : -1L;
	}

	@Override
	public long getOperations() {
		long real = super.getOperations();
		// Terme DETERMINISTE : nombre de statements guest executes ce tour, compte par l'instrument
		// embarque dans l'image isolate custom et lu a travers la frontiere (~3 us). Reproductible
		// (meme code + memes entrees -> meme compte), donc les IA de recherche qui se bornent sur
		// getOperations() sont bit-reproductibles (replays fiables, arene classee).
		if (statementCounter != null) {
			try {
				// Terme guest * facteur de calibration par langage (cf opsFactor) : rend getOperations()
				// comparable a LeekScript pour la fairness arene.
				return real + (long) (statementCounter.execute().asLong() * opsFactor);
			} catch (Exception e) {
				// Contexte annule/ferme en cours de tour : repli temps CPU pour le reste du tour.
				statementCounter = null;
			}
		}
		// Fallback sous isolate : terme proportionnel au TEMPS CPU du thread de combat passe dans ce tour
		// -> plus honnete que le temps mur (exclut pauses GC / ordonnancement). Repli sur le temps mur si
		// le temps CPU est indisponible. NON reproductible (depend de la charge machine).
		if (opsBudgetNanos <= 0) {
			return real;
		}
		long elapsed = -1L;
		// En isolate externe, le guest ne tourne PAS sur ce thread : temps CPU inutilisable (reste
		// proche de 0 pendant que le guest calcule dans son process) -> temps mur directement.
		if (!externalIsolate && CPU_TIME_SUPPORTED && turnStartCpuNanos >= 0) {
			long nowCpu = THREAD_MX.getThreadCpuTime(turnThreadId);
			if (nowCpu >= 0) {
				elapsed = nowCpu - turnStartCpuNanos;
			}
		}
		if (elapsed < 0) { // temps CPU indispo -> repli sur le temps mur
			if (turnStartNanos == 0) {
				return real;
			}
			elapsed = System.nanoTime() - turnStartNanos;
		}
		if (elapsed < 0) {
			elapsed = 0;
		}
		long synthetic = (long) ((double) getMaxOperations() * elapsed / opsBudgetNanos);
		return real + synthetic;
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
			// Retirer du suivi du sandbox : sinon chaque contexte reconstruit (timeout, limite atteinte
			// chaque tour...) s'accumulerait dans la liste pour tout le combat (retention memoire).
			sandbox.forgetContext(context);
			context = null;
			statementCounter = null; // binding lie au contexte ferme ; refetche par ensureContext
			guestBindings = null;
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
