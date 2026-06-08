package com.leekwars.generator.polyglot;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	private final List<Context> contexts = Collections.synchronizedList(new ArrayList<>());

	public PolyglotSandbox(String... languages) {
		this(DEFAULT_STATEMENT_LIMIT, languages);
	}

	public PolyglotSandbox(long statementLimit, String... languages) {
		String[] permitted = languages.length == 0 ? new String[] { "js" } : languages;
		this.engine = Engine.newBuilder(permitted).build();
		this.limits = ResourceLimits.newBuilder()
				.statementLimit(statementLimit, null)
				.build();
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
}
