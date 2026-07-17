package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotConsole;

/**
 * REPL polyglot de la console interactive : etat persistant entre lignes, rendu en notation native,
 * reroutage console.log/print, erreurs. Pendant de la console LeekScript ({@link com.leekwars.websocket}).
 */
public class TestPolyglotConsoleRepl {

	private static final class Logs implements PolyglotConsole.LogSink {
		final List<String> lines = new ArrayList<>();
		@Override public void log(int level, String message) { lines.add(message); }
	}

	private String run(PolyglotConsole c, String code) throws Exception {
		return c.execute(code).display;
	}

	@Test
	public void jsBasics() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			Assert.assertEquals("2", run(c, "1 + 1"));
			Assert.assertEquals("[1, 2, 3]", run(c, "[1, 2, 3]"));
			Assert.assertEquals("{ a: 1, b: 'hi' }", run(c, "({a: 1, b: 'hi'})"));
			Assert.assertEquals("'hi'", run(c, "'hi'"));
			Assert.assertEquals("undefined", run(c, "undefined"));
			Assert.assertEquals("true", run(c, "1 < 2"));
		}
	}

	@Test
	public void jsStatePersistsVar() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			run(c, "var x = 5");
			Assert.assertEquals("6", run(c, "x + 1"));
		}
	}

	@Test
	public void jsStatePersistsLet() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			run(c, "let y = 3");
			Assert.assertEquals("6", run(c, "y * 2"));
		}
	}

	@Test
	public void jsStatePersistsFunction() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			run(c, "function add(a, b) { return a + b; }");
			Assert.assertEquals("7", run(c, "add(3, 4)"));
		}
	}

	@Test
	public void jsConsoleLog() throws Exception {
		Logs logs = new Logs();
		try (PolyglotConsole c = new PolyglotConsole("js", logs)) {
			run(c, "console.log('hello', 42)");
			Assert.assertEquals(1, logs.lines.size());
			Assert.assertEquals("hello 42", logs.lines.get(0));
		}
	}

	@Test
	public void jsError() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			try {
				run(c, "nope()");
				Assert.fail("should throw");
			} catch (PolyglotConsole.ConsoleException e) {
				Assert.assertNotNull(e.getMessage());
			}
		}
	}

	@Test
	public void jsOpsCounted() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			PolyglotConsole.Result r = c.execute("var s = 0; for (var i = 0; i < 100; i++) s += i; s");
			Assert.assertEquals("4950", r.display);
			Assert.assertTrue("ops should be counted", r.ops > 0);
		}
	}

	@Test
	public void pythonBasics() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("python", new Logs())) {
			Assert.assertEquals("2", run(c, "1 + 1"));
			Assert.assertEquals("[1, 2, 3]", run(c, "[1, 2, 3]"));
			Assert.assertEquals("'hi'", run(c, "'hi'"));
			Assert.assertNull("None n'affiche rien", run(c, "None"));
			Assert.assertNull("un statement n'affiche rien", run(c, "z = 1"));
		}
	}

	@Test
	public void pythonStatePersists() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("python", new Logs())) {
			run(c, "x = 5");
			Assert.assertEquals("6", run(c, "x + 1"));
			run(c, "def f(n):\n    return n * n");
			Assert.assertEquals("9", run(c, "f(3)"));
		}
	}

	@Test
	public void pythonPrint() throws Exception {
		Logs logs = new Logs();
		try (PolyglotConsole c = new PolyglotConsole("python", logs)) {
			run(c, "print('hello', 42)");
			Assert.assertEquals(1, logs.lines.size());
			Assert.assertEquals("hello 42", logs.lines.get(0));
		}
	}

	@Test
	public void typescriptTranspiles() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("ts", new Logs())) {
			Assert.assertEquals("3", run(c, "const a: number = 1; const b: number = 2; a + b"));
		}
	}

	@Test
	public void typescriptStatePersistsAcrossLines() throws Exception {
		// transpileModule ne fait pas de resolution de noms : `n + 1` ligne 2 (n vient de la ligne 1)
		// transpile sans diagnostic, et l'etat du contexte JS persiste -> le REPL TS reste continu.
		try (PolyglotConsole c = new PolyglotConsole("ts", new Logs())) {
			run(c, "const n: number = 41");
			Assert.assertEquals("42", run(c, "n + 1"));
		}
	}
}
