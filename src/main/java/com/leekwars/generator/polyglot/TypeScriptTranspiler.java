package com.leekwars.generator.polyglot;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.zip.GZIPInputStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import com.leekwars.generator.polyglot.PolyglotEntityAI.SyntaxProblem;

/**
 * Transpile le TypeScript en JavaScript via le compilateur officiel ({@code ts.transpileModule})
 * execute dans un contexte GraalVM js de CONFIANCE (c'est NOTRE code, pas celui du joueur).
 *
 * GraalVM n'a pas de moteur TypeScript : on efface les types au build, puis l'artefact JS repart
 * dans le pipeline polyglot sandboxe (memes gardes determinisme / DoS / bridge API). La transpilation
 * etant un transform pur source-&gt;source (pas d'acces FS ni hote), aucune nouvelle surface de securite.
 *
 * Le compilateur (~8.7 Mo, bundle gzip en resource) est charge UNE fois (~2 s a froid) puis reutilise
 * (~25 ms / transpilation). Un seul contexte partage, acces serialise : la transpilation a lieu au
 * BUILD de l'IA (une fois par IA et par combat), jamais sur le chemin chaud des tours.
 */
public final class TypeScriptTranspiler {

	/** Resultat d'une transpilation : le JS emis, la sourcemap (peut etre null) et les diagnostics TS. */
	public static final class Result {
		public final String js;
		public final String sourceMap;
		public final List<SyntaxProblem> diagnostics;
		Result(String js, String sourceMap, List<SyntaxProblem> diagnostics) {
			this.js = js;
			this.sourceMap = sourceMap;
			this.diagnostics = diagnostics;
		}
		/** true si aucun diagnostic de syntaxe (l'IA peut etre executee). */
		public boolean ok() {
			return diagnostics.isEmpty();
		}
		/** Premier diagnostic, ou null. Pratique pour remonter UNE erreur a l'editeur (comme LeekScript). */
		public SyntaxProblem firstDiagnostic() {
			return diagnostics.isEmpty() ? null : diagnostics.get(0);
		}
	}

	private static final Object LOCK = new Object();
	private static volatile Context tsContext;

	private TypeScriptTranspiler() {}

	/** Charge le compilateur TS dans son contexte de confiance (idempotent). A appeler au boot (prechauffage). */
	public static void prewarm() {
		context();
	}

	private static Context context() {
		Context ctx = tsContext;
		if (ctx != null) {
			return ctx;
		}
		synchronized (LOCK) {
			if (tsContext == null) {
				Context c = Context.newBuilder("js")
						.allowHostAccess(HostAccess.NONE)
						.allowIO(IOAccess.NONE)
						.out(OutputStream.nullOutputStream())
						.err(OutputStream.nullOutputStream())
						.build();
				// Shim CommonJS minimal : le bundle UMD de TS s'expose via module.exports (fallback global ts).
				c.eval("js", "var module = { exports: {} }; var exports = module.exports;");
				c.eval("js", loadCompiler());
				c.eval("js", TRANSPILE_FN);
				tsContext = c;
			}
			return tsContext;
		}
	}

	/** Budget wall-clock d'une transpilation : borne le compilateur face a un source joueur pathologique. */
	private static final long TRANSPILE_TIMEOUT_MS = 5_000;

	/**
	 * Transpile un source TypeScript. {@code fileName} sert au nommage des diagnostics et de la sourcemap.
	 * Toujours renvoie un Result : des diagnostics non vides -&gt; {@link Result#ok()} false (IA invalide).
	 *
	 * Le source vient d'un JOUEUR (non fiable) : on borne la transpilation par un watchdog wall-clock
	 * (le contexte du compilateur n'a pas de statement limit, sinon le compilateur legitime serait coupe)
	 * et on JETTE le contexte partage en cas d'echec/timeout/crash, pour qu'il soit reconstruit au prochain
	 * appel plutot que de rester potentiellement annule pour tous les combats de la JVM.
	 */
	public static Result transpile(String source, String fileName) {
		synchronized (LOCK) {
			Context ctx = context(); // sous le verrou : reconstruit si un appel precedent l'a jete
			ScheduledFuture<?> deadline = PolyglotSandbox.scheduleDeadline(
					() -> PolyglotSandbox.interruptAsync(ctx), TRANSPILE_TIMEOUT_MS);
			try {
				Value bindings = ctx.getBindings("js");
				bindings.putMember("__lwSrc", source);
				bindings.putMember("__lwFile", fileName != null ? fileName : "ai.ts");
				Value r = ctx.eval("js", "__lwTranspile(__lwSrc, __lwFile)");

				String js = r.getMember("js").asString();
				Value mapV = r.getMember("map");
				String sourceMap = (mapV != null && !mapV.isNull()) ? mapV.asString() : null;

				List<SyntaxProblem> diagnostics = new ArrayList<>();
				Value diags = r.getMember("diagnostics");
				long n = diags.getArraySize();
				for (long i = 0; i < n; i++) {
					Value d = diags.getArrayElement(i);
					String message = d.getMember("message").asString();
					int start = d.getMember("start").asInt();
					int length = d.getMember("length").asInt();
					diagnostics.add(SyntaxProblem.atOffset(source, message, start, length));
				}
				return new Result(js, sourceMap, Collections.unmodifiableList(diagnostics));
			} catch (PolyglotException e) {
				// Timeout (interruption du watchdog), OOM guest ou crash du compilateur sur un source
				// pathologique : on jette le contexte partage (potentiellement annule) pour qu'il soit
				// reconstruit, et on remonte un diagnostic (IA invalide cote joueur) plutot que de laisser
				// fuiter une erreur serveur ou un contexte mort pour tous les combats.
				discardContext(ctx);
				String reason = (e.isInterrupted() || e.isCancelled() || e.isResourceExhausted())
						? "TypeScript transpilation timed out"
						: "TypeScript transpilation failed";
				return new Result("", null, Collections.singletonList(
						SyntaxProblem.atOffset(source, reason, -1, 0)));
			} finally {
				deadline.cancel(false);
			}
		}
	}

	/** Jette le contexte partage (ferme + oublie) apres un echec, pour forcer sa reconstruction au prochain appel. */
	private static void discardContext(Context ctx) {
		try {
			ctx.close(true); // cancelIfExecuting : un contexte annule ne doit pas relancer a la fermeture
		} catch (Exception ignore) {
			// best effort
		}
		if (tsContext == ctx) {
			tsContext = null;
		}
	}

	/** Fonction de transpilation posee une fois sur le contexte (evite de reconstruire l'objet options a chaque appel). */
	private static final String TRANSPILE_FN =
		"globalThis.__ts = (typeof ts !== 'undefined') ? ts : module.exports;\n" +
		"globalThis.__lwTranspile = function(src, fileName) {\n" +
		"  var r = __ts.transpileModule(src, {\n" +
		"    fileName: fileName,\n" +
		"    reportDiagnostics: true,\n" +
		"    compilerOptions: {\n" +
		"      target: __ts.ScriptTarget.ES2021,\n" +
		"      module: __ts.ModuleKind.ESNext,\n" +          // import/export preserves -> multi-fichiers via FS
		"      sourceMap: true,\n" +
		"      inlineSourceMap: false,\n" +
		"      experimentalDecorators: true,\n" +
		"      removeComments: false\n" +
		"    }\n" +
		"  });\n" +
		"  var diags = (r.diagnostics || []).map(function(d) {\n" +
		"    return {\n" +
		"      message: __ts.flattenDiagnosticMessageText(d.messageText, '\\n'),\n" +
		"      start: (d.start == null ? -1 : d.start),\n" +
		"      length: (d.length == null ? 0 : d.length)\n" +
		"    };\n" +
		"  });\n" +
		"  return { js: r.outputText, map: (r.sourceMapText || null), diagnostics: diags };\n" +
		"};";

	private static String loadCompiler() {
		try (InputStream raw = TypeScriptTranspiler.class.getResourceAsStream("/typescript.js.gz")) {
			if (raw == null) {
				throw new IllegalStateException("resource /typescript.js.gz manquante (compilateur TS non bundle)");
			}
			try (GZIPInputStream gz = new GZIPInputStream(raw)) {
				return new String(gz.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			throw new RuntimeException("chargement du compilateur TypeScript impossible", e);
		}
	}
}
