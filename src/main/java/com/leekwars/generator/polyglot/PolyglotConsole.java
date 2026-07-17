package com.leekwars.generator.polyglot;

import java.util.concurrent.ScheduledFuture;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

/**
 * REPL polyglot (JavaScript / TypeScript / Python) pour la console interactive du site.
 *
 * <p>Pendant polyglot de {@link LeekScriptConsole} cote LeekScript : chaque instance garde un
 * {@link Context} GraalVM VIVANT pour toute la duree de la session console d'un client, si bien que
 * l'etat persiste entre les lignes (une variable definie ligne 1 est visible ligne 2), exactement
 * comme le {@code Session} LeekScript persiste ses variables.
 *
 * <p>Difference avec {@link PolyglotEntityAI} (combat) : PAS de contexte de combat (ni {@code me}, ni
 * API de jeu, ni gardes de determinisme), et la sortie {@code console.log}/{@code print} est reroutee
 * vers un {@link LogSink} (affichee dans la console) plutot que jetee. Le resultat d'une ligne est
 * rendu dans la NOTATION NATIVE du langage (tableau JS {@code [1, 2, 3]}, {@code repr()} Python).
 *
 * <p>Isolation / anti-DoS : le contexte est cree via {@link PolyglotSandbox#createContext} (policy
 * ISOLATED, cap RAM et statement limit par contexte) ; chaque {@code execute} est en plus borne par un
 * watchdog wall-clock ({@link #EXECUTE_TIMEOUT_MS}) qui interrompt une boucle infinie.
 */
public class PolyglotConsole implements AutoCloseable {

	/** Budget wall-clock d'une ligne de console : coupe une boucle infinie sans tuer le serveur. */
	private static final long EXECUTE_TIMEOUT_MS = 5_000;

	/** Reroutage des sorties guest (console.log / print) vers l'affichage de la console. */
	public interface LogSink {
		/** @param level 1 = log, 2 = warn, 3 = error (memes niveaux que le LeekLog cote client). */
		void log(int level, String message);
	}

	/** Erreur d'execution ou de compilation d'une ligne : porte un message deja formate. */
	public static final class ConsoleException extends Exception {
		private static final long serialVersionUID = 1L;
		public final long ops;
		public ConsoleException(String message, long ops) {
			super(message);
			this.ops = ops;
		}
	}

	/** Resultat d'une ligne : {@code display} est la notation native, ou null si la ligne ne rend rien. */
	public static final class Result {
		public final String display;
		public final long ops;
		public Result(String display, long ops) {
			this.display = display;
			this.ops = ops;
		}
	}

	private final String language;   // "js" | "ts" | "python" (tel que demande par le client)
	private final String languageId; // "js" | "python" (moteur GraalVM ; ts transpile en js)
	private final boolean typescript;
	private final PolyglotSandbox sandbox;
	private final Context context;
	private final Value counter; // compteur de statements deterministe, ou null (retombe sur 0 op)
	private int sourceCounter = 0;

	public PolyglotConsole(String language, LogSink sink) {
		this.language = language;
		this.typescript = "ts".equals(language);
		this.languageId = "python".equals(language) ? "python" : "js";
		this.sandbox = new PolyglotSandbox(languageId);
		this.context = sandbox.createContext(languageId);
		this.counter = PolyglotSandbox.statementCounterBinding(context);
		installConsole(sink);
	}

	/** Branche les fonctions hote de log et installe le reroutage console.log / print + l'inspecteur natif. */
	private void installConsole(LogSink sink) {
		Value bindings = context.getBindings(languageId);
		bindings.putMember("__lw_log", (ProxyExecutable) args -> { sink.log(1, joinArg(args)); return null; });
		bindings.putMember("__lw_warn", (ProxyExecutable) args -> { sink.log(2, joinArg(args)); return null; });
		bindings.putMember("__lw_err", (ProxyExecutable) args -> { sink.log(3, joinArg(args)); return null; });
		if ("js".equals(languageId)) {
			context.eval("js", JS_SETUP);
		} else {
			context.eval("python", PY_SETUP);
		}
	}

	private static String joinArg(Value[] args) {
		if (args.length == 1) return args[0].isString() ? args[0].asString() : String.valueOf(args[0]);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			if (i > 0) sb.append(' ');
			sb.append(args[i].isString() ? args[i].asString() : String.valueOf(args[i]));
		}
		return sb.toString();
	}

	/**
	 * Execute une ligne et renvoie son resultat (notation native). Persiste l'etat pour la ligne suivante.
	 *
	 * @throws ConsoleException erreur de transpilation (TS), de compilation ou d'execution
	 */
	public Result execute(String code) throws ConsoleException {
		String source = code;
		if (typescript) {
			TypeScriptTranspiler.Result tr = TypeScriptTranspiler.transpile(code, "console.ts");
			if (!tr.ok()) {
				throw new ConsoleException(tr.firstDiagnostic().message, 0);
			}
			source = tr.js;
		}

		long before = readCounter();
		final Context running = context;
		ScheduledFuture<?> deadline = PolyglotSandbox.scheduleDeadline(
				() -> PolyglotSandbox.interruptAsync(running), EXECUTE_TIMEOUT_MS);
		try {
			context.resetLimits();
			String display;
			if ("js".equals(languageId)) {
				// Source nommee unique : evite le cache de Source de GraalJS entre lignes (une meme
				// source re-evaluee serait mise en cache et ne re-jouerait pas ses effets).
				Value v = context.eval(Source.newBuilder("js", source, "console" + (sourceCounter++) + ".js").buildLiteral());
				display = context.getBindings("js").getMember("__lw_inspect").execute(v).asString();
			} else {
				Value v = context.getBindings("python").getMember("__lw_run").execute(source);
				display = v.isString() ? v.asString() : null;
			}
			long ops = readCounter() - before;
			return new Result(display, ops);
		} catch (PolyglotException e) {
			throw new ConsoleException(formatError(e), readCounter() - before);
		} finally {
			deadline.cancel(false);
		}
	}

	private long readCounter() {
		if (counter == null) return 0;
		try {
			return counter.execute().asLong();
		} catch (Exception e) {
			return 0;
		}
	}

	/** Message d'erreur guest lisible : le message de l'exception, sans la stack hote. */
	private static String formatError(PolyglotException e) {
		if (e.isInterrupted() || e.isCancelled()) {
			return "Execution interrupted (timeout or resource limit)";
		}
		String message = e.getMessage();
		return message != null ? message : "Error";
	}

	@Override
	public void close() {
		try {
			sandbox.close();
		} catch (Exception ignore) {
			// best effort
		}
	}

	public String getLanguage() {
		return language;
	}

	// Inspecteur natif JS (notation de tableau/objet, chaines entre guillemets au niveau d'imbrication
	// > 0 comme au top-level, a la maniere du REPL Node) + reroutage console.* vers les fonctions hote.
	// console.log affiche les chaines de premier niveau SANS guillemets (quoteTop=false), le rendu de
	// resultat les affiche AVEC (quoteTop=true), comme Node.
	private static final String JS_SETUP =
		"globalThis.__lw_inspect = function(v, quoteTop){\n"
		+ "  var seen = new Set();\n"
		+ "  function q(s){ return \"'\" + String(s).replace(/\\\\/g,'\\\\\\\\').replace(/'/g,\"\\\\'\").replace(/\\n/g,'\\\\n') + \"'\"; }\n"
		+ "  function ins(v, depth){\n"
		+ "    if (v === null) return 'null';\n"
		+ "    if (v === undefined) return 'undefined';\n"
		+ "    var t = typeof v;\n"
		+ "    if (t === 'string') return (depth === 0 && quoteTop === false) ? v : q(v);\n"
		+ "    if (t === 'number' || t === 'boolean') return String(v);\n"
		+ "    if (t === 'bigint') return String(v) + 'n';\n"
		+ "    if (t === 'symbol') return v.toString();\n"
		+ "    if (t === 'function') return v.name ? '[Function: ' + v.name + ']' : '[Function (anonymous)]';\n"
		+ "    if (Array.isArray(v)) {\n"
		+ "      if (seen.has(v)) return '[Circular]';\n"
		+ "      seen.add(v); var s = '[' + v.map(function(x){ return ins(x, depth+1); }).join(', ') + ']'; seen.delete(v); return s;\n"
		+ "    }\n"
		+ "    if (seen.has(v)) return '[Circular]';\n"
		+ "    seen.add(v);\n"
		+ "    var keys = Object.keys(v);\n"
		+ "    var body = keys.map(function(k){ return k + ': ' + ins(v[k], depth+1); }).join(', ');\n"
		+ "    seen.delete(v);\n"
		+ "    return '{' + (body ? ' ' + body + ' ' : '') + '}';\n"
		+ "  }\n"
		+ "  return ins(v, 0);\n"
		+ "};\n"
		+ "(function(){\n"
		+ "  var L = function(){ __lw_log.apply(null, Array.prototype.map.call(arguments, function(a){ return __lw_inspect(a, false); })); };\n"
		+ "  var W = function(){ __lw_warn.apply(null, Array.prototype.map.call(arguments, function(a){ return __lw_inspect(a, false); })); };\n"
		+ "  var E = function(){ __lw_err.apply(null, Array.prototype.map.call(arguments, function(a){ return __lw_inspect(a, false); })); };\n"
		+ "  globalThis.console = { log: L, info: L, debug: L, warn: W, error: E };\n"
		+ "})();\n";

	// REPL Python : compile en mode 'single' pour que les statements-expression passent par displayhook
	// (echo du repr, comme l'interpreteur interactif), execute dans un namespace persistant (__lw_g) pour
	// que l'etat survive entre lignes, et reroute print vers la fonction hote __lw_log. __lw_run renvoie le
	// repr capture (chaine) ou l'objet sentinelle __lw_NO quand la ligne ne rend rien (None ou statement).
	private static final String PY_SETUP =
		"import sys as __lw_sys, builtins as __lw_b\n"
		+ "__lw_g = {'__name__': '__console__', '__builtins__': __lw_b}\n"
		+ "__lw_NO = object()\n"
		+ "__lw_cap = [__lw_NO]\n"
		+ "def __lw_disp(v):\n"
		+ "    if v is not None:\n"
		+ "        __lw_cap[0] = repr(v)\n"
		+ "def __lw_run(src):\n"
		+ "    __lw_cap[0] = __lw_NO\n"
		+ "    old = __lw_sys.displayhook\n"
		+ "    __lw_sys.displayhook = __lw_disp\n"
		+ "    try:\n"
		+ "        exec(compile(src, '<console>', 'single'), __lw_g)\n"
		+ "    finally:\n"
		+ "        __lw_sys.displayhook = old\n"
		+ "    return __lw_cap[0]\n"
		+ "def __lw_print(*a, sep=' ', end='\\n', file=None, flush=False):\n"
		+ "    __lw_log(sep.join(str(_x) for _x in a))\n"
		+ "__lw_b.print = __lw_print\n";
}
