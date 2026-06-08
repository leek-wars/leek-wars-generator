package test;

import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotFileSystem;

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
