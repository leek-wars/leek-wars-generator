package test;

import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotFileSystem;
import com.leekwars.generator.polyglot.PolyglotSandbox;

/**
 * Multi-fichiers : les systemes de modules natifs (import/export JS, import Python) resolvent
 * leurs dependances a travers le {@link PolyglotFileSystem} (en memoire, fichiers du joueur),
 * sans aucun acces hote. Teste le mecanisme au niveau FileSystem.
 */
public class TestPolyglotModules {

	private IOAccess io(Map<String, String> files) {
		return IOAccess.newBuilder().fileSystem(new PolyglotFileSystem(files.keySet(), files::get)).build();
	}

	@Test
	public void detectLanguageIsCaseInsensitive() {
		// Le serveur/client classent l'extension via strtolower / toLowerCase ; le moteur doit
		// reconnaitre la meme chose, sinon un "Bot.JS" sauve comme polyglot serait compile en LeekScript.
		Assert.assertEquals("js", PolyglotEntityAI.detectLanguage("main.js"));
		Assert.assertEquals("js", PolyglotEntityAI.detectLanguage("Bot.JS"));
		Assert.assertEquals("python", PolyglotEntityAI.detectLanguage("main.py"));
		Assert.assertEquals("python", PolyglotEntityAI.detectLanguage("Bot.PY"));
		Assert.assertNull(PolyglotEntityAI.detectLanguage("main")); // LeekScript
		Assert.assertNull(PolyglotEntityAI.detectLanguage("notes.md"));
		Assert.assertNull(PolyglotEntityAI.detectLanguage(null));
	}

	// ---------- Validation syntaxique (diagnostics editeur) ----------

	@Test
	public void validateSyntaxAcceptsValidJs() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertNull(PolyglotEntityAI.validateSyntax("js", "var x = 1 + 2;\nfunction turn(){ return x; }", sb));
		}
	}

	@Test
	public void validateSyntaxReportsJsSyntaxError() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			PolyglotEntityAI.SyntaxProblem p = PolyglotEntityAI.validateSyntax("js", "var x = ;", sb);
			Assert.assertNotNull("une erreur de syntaxe JS doit etre rapportee", p);
			Assert.assertNotNull(p.message);
			Assert.assertTrue("ligne 1-based", p.startLine >= 1);
		}
	}

	@Test
	public void validateSyntaxAcceptsValidPython() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertNull(PolyglotEntityAI.validateSyntax("python", "def turn():\n    return 1\n", sb));
		}
	}

	@Test
	public void validateSyntaxReportsPythonSyntaxError() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			PolyglotEntityAI.SyntaxProblem p = PolyglotEntityAI.validateSyntax("python", "def turn(:\n    return 1\n", sb);
			Assert.assertNotNull("une erreur de syntaxe Python doit etre rapportee", p);
			Assert.assertNotNull(p.message);
		}
	}

	@Test
	public void validateSyntaxSkipsJsModule() {
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			// import en tete = module ES : non parsable comme script -> on ne valide pas (null).
			Assert.assertNull(PolyglotEntityAI.validateSyntax("js",
				"import { x } from './u.mjs';\nglobalThis.turn = () => x;", sb));
		}
	}

	@Test
	public void jsEsmImportResolvesSiblingFile() {
		Map<String, String> files = new HashMap<>();
		files.put("utils.mjs", "export function add(x, y) { return x + y; }\nexport const NAME = 'utils';\n");
		files.put("main.mjs", "import { add, NAME } from './utils.mjs';\nglobalThis.__r = add(40, 2) + ':' + NAME;\n");

		try (Context c = Context.newBuilder("js").allowHostAccess(HostAccess.NONE).allowIO(io(files)).build()) {
			// L'entree est chargee comme un vrai fichier du FS (import d'un chemin absolu) -> ses
			// imports relatifs resolvent contre /ai/.
			c.eval("js", "import('" + PolyglotFileSystem.mountPath("main.mjs") + "');");
			c.eval("js", "1;"); // laisse la microtask du module s'executer
			Assert.assertEquals("42:utils", c.eval("js", "globalThis.__r").asString());
		}
	}

	@Test
	public void pythonImportResolvesSiblingModule() throws Exception {
		Map<String, String> files = new HashMap<>();
		files.put("utils.py", "def add(x, y):\n    return x + y\nNAME = 'utils'\n");
		files.put("main.py", "import utils\nresult = str(utils.add(40, 2)) + ':' + utils.NAME\n");

		try (Context c = Context.newBuilder("python").allowHostAccess(HostAccess.NONE).allowIO(io(files))
				.option("python.PythonPath", PolyglotFileSystem.MOUNT).build()) {
			// Source nommee avec le chemin de montage : l'entree a une "localisation" /ai, donc
			// import utils resout /ai/utils.py via le FS (sys.path[0] = dossier de l'entree).
			Source main = Source.newBuilder("python", files.get("main.py"), PolyglotFileSystem.mountPath("main.py")).build();
			c.eval(main);
			Assert.assertEquals("42:utils", c.eval("python", "result").asString());
		}
	}

	@Test
	public void passthroughBlocksSymlinkEscape() throws Exception {
		// Le passthrough (stdlib) delegue a l'hote, mais un symlink sortant doit etre refuse :
		// la decision lexicale est reverifiee apres resolution des symlinks (requireContained).
		Path base = Files.createTempDirectory("lw_pt");
		Path secret = Files.createTempDirectory("lw_secret");
		Files.writeString(secret.resolve("flag.txt"), "ESCAPED");
		Files.createSymbolicLink(base.resolve("evil"), secret); // symlink dans le passthrough vers dehors

		PolyglotFileSystem fs = new PolyglotFileSystem(Set.of(), p -> null, base);
		Path viaSymlink = base.resolve("evil").resolve("flag.txt"); // lexicalement sous base, reellement dehors
		try {
			fs.checkAccess(viaSymlink, Set.of(AccessMode.READ));
			Assert.fail("un symlink sortant du passthrough aurait du etre refuse");
		} catch (java.nio.file.AccessDeniedException e) {
			// attendu
		}
		// un vrai fichier sous le passthrough reste lisible
		Files.writeString(base.resolve("real.txt"), "OK");
		fs.checkAccess(base.resolve("real.txt"), Set.of(AccessMode.READ)); // ne doit pas lever
	}

	@Test
	public void hostFilesystemStaysUnreachable() {
		Map<String, String> files = new HashMap<>();
		files.put("main.py", "ok = True\n");
		try (Context c = Context.newBuilder("python").allowHostAccess(HostAccess.NONE).allowIO(io(files))
				.option("python.PythonPath", PolyglotFileSystem.MOUNT).build()) {
			// Le FS ne sert que /ai/* : tout chemin hote est hors montage -> inaccessible.
			try {
				c.eval("python", "open('/etc/passwd').read()");
				Assert.fail("la lecture d'un fichier hote aurait du echouer");
			} catch (Exception e) {
				Assert.assertTrue(true);
			}
		}
	}
}
