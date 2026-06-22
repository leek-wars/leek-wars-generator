package com.leekwars.generator.polyglot;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
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

	/**
	 * Transpile un source TypeScript. {@code fileName} sert au nommage des diagnostics et de la sourcemap.
	 * Toujours renvoie un Result : si des diagnostics existent, {@link Result#ok()} est false (IA invalide).
	 */
	public static Result transpile(String source, String fileName) {
		Context ctx = context();
		synchronized (LOCK) {
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
