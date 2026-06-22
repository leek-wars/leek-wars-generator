package com.leekwars.generator.polyglot;

import java.io.InputStream;
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
	private long turnStartNanos;               // nanos de debut de tour (base du compteur d'ops synthetique)
	// Budget temps (ns) au bout duquel le compteur synthetique atteint getMaxOperations(). Derive de
	// turnWallClockLimitMs (recalcule dans le setter) ; pre-calcule car getOperations() est sur le hot path.
	private long opsBudgetNanos = (long) (DEFAULT_TURN_WALL_CLOCK_LIMIT_MS * 1_000_000L * OPS_TIME_BUDGET_FRACTION);
	// Compteur de statements guest DETERMINISTE (cf StatementCounter) : terme d'operations reproductible
	// pour getOperations(). null si l'instrument est indisponible -> fallback temps-based (non reproductible).
	private final StatementCounter.Counter statementCounter;

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
		this.statementCounter = sandbox != null ? sandbox.getStatementCounter() : null;
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
			context = sandbox.createContext(languageId, fileSystem);
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
		+ "L.now=function(){return F;};L.parse=D.parse;L.UTC=D.UTC;L.prototype=D.prototype;"
		// On scelle aussi D.prototype.constructor -> L : sinon (new Date()).constructor et Date.prototype.constructor
		// pointaient encore sur le Date d'origine, permettant de recuperer l'horloge reelle via .constructor.now().
		+ "Object.defineProperty(D.prototype,'constructor',{value:L,writable:false,configurable:false});"
		+ "globalThis.Date=L;})();"
		+ "if(typeof performance!=='undefined'){performance.now=function(){return 0;};}";

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

	private static String loadResource(String path) {
		try (InputStream in = PolyglotEntityAI.class.getResourceAsStream(path)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Neutralise les sources de non-determinisme atteignables par le guest, sinon les IA JS/Python
	 * ne seraient pas reproductibles a partir de la seed du combat (re-simulation / verification) :
	 * generateur aleatoire seede et fige, et horloge murale fixe.
	 */
	private void installDeterminismGuards() {
		if ("js".equals(languageId)) {
			context.getBindings(languageId).putMember("__lw_random", (ProxyExecutable) args -> getRandom().getDouble());
			context.eval(languageId, JS_DETERMINISM_GUARD);
			context.eval(languageId, JS_CONSOLE_SETUP);
			if (JS_OBJECT_API != null) {
				context.eval(languageId, JS_OBJECT_API);
			}
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
		return
			// Multi-fichiers : on s'assure que la stdlib a la PRIORITE sur les fichiers du joueur
			// (/ai). Sinon une IA nommant un fichier random.py / os.py masquerait la stdlib (et le
			// RNG seede). On deplace /ai en fin de sys.path AVANT d'importer la stdlib.
			"import sys\n"
			+ "if '/ai' in sys.path:\n    sys.path.remove('/ai')\n    sys.path.append('/ai')\n"
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
			+ "except Exception:\n    pass\n";
	}

	@Override
	public Object runIA(Session session) throws LeekRunException {
		if (disabled) {
			return null; // IA neutralisee (trop de depassements wall-clock) : n'agit plus, ne consomme plus.
		}
		turnStartNanos = System.nanoTime(); // base du compteur d'ops temps-based (fallback, cf getOperations)
		if (statementCounter != null) {
			statementCounter.reset(); // compteur de statements guest remis a zero a chaque tour (terme deterministe)
		}
		ensureContext();
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
	@Override
	public long getOperations() {
		long real = super.getOperations();
		// Terme DETERMINISTE : nombre de statements guest executes ce tour. Reproductible (meme code +
		// memes entrees -> meme compte), donc les IA de recherche qui se bornent sur getOperations()
		// redeviennent bit-reproductibles (replays fiables, arene classee).
		if (statementCounter != null) {
			return real + statementCounter.get();
		}
		// Fallback (instrument indisponible) : terme proportionnel au TEMPS ecoule -> NON reproductible,
		// mais evite que l'IA sature le backstop wall-clock faute de signal d'ops.
		if (turnStartNanos == 0 || opsBudgetNanos <= 0) {
			return real;
		}
		long elapsed = System.nanoTime() - turnStartNanos;
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
