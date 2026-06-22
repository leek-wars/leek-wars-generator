package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotEntityAI.SyntaxProblem;
import com.leekwars.generator.polyglot.TypeScriptTranspiler;

/**
 * Valide le transpileur TS-&gt;JS (tsc embarque dans graaljs) : effacement des types, emission
 * correcte des enum, conservation des import/export (ES module pour le multi-fichiers), et
 * remontee localisee des erreurs de syntaxe (diagnostics tsc -&gt; SyntaxProblem editeur).
 */
public class TestTypeScriptTranspiler {

	@Test
	public void stripsTypesAndEmitsEnum() {
		String ts = String.join("\n",
			"const x: number = 2;",
			"enum Dir { N, S, E, W }",
			"interface P { hp: number }",
			"function dist<T extends P>(a: T, b: T): number { return Math.abs(a.hp - b.hp); }",
			"let d: Dir = Dir.N;",
			"function turn(): void { debug('t ' + x + ' ' + d + ' ' + dist({hp:1},{hp:5})); }");

		TypeScriptTranspiler.Result r = TypeScriptTranspiler.transpile(ts, "ai.ts");

		Assert.assertTrue("aucun diagnostic attendu, vu: " + r.diagnostics, r.ok());
		// Types effaces.
		Assert.assertFalse("annotation ': number' non effacee", r.js.contains(": number"));
		Assert.assertFalse("interface non effacee", r.js.contains("interface"));
		Assert.assertFalse("generic <T> non efface", r.js.contains("<T extends"));
		// enum emis sous forme executable.
		Assert.assertTrue("enum non emis", r.js.contains("Dir[Dir[\"N\"] = 0] = \"N\""));
		// Le corps de turn() survit.
		Assert.assertTrue("turn() perdu", r.js.contains("function turn()"));
	}

	@Test
	public void preservesEsModuleImports() {
		// module: ESNext -> les import/export sont conserves tels quels pour le multi-fichiers (resolus via le FS).
		String ts = "import { foo } from './bar';\nexport function turn(): void { foo(); }";
		TypeScriptTranspiler.Result r = TypeScriptTranspiler.transpile(ts, "ai.ts");
		Assert.assertTrue(r.ok());
		Assert.assertTrue("import non conserve", r.js.contains("import { foo } from './bar'"));
		Assert.assertTrue("export non conserve", r.js.contains("export function turn()"));
	}

	@Test
	public void reportsSyntaxErrorWithLocation() {
		// Erreur de syntaxe sur la 3e ligne.
		String ts = "const a = 1;\nconst b = 2;\nfunction turn() { return ( }";
		TypeScriptTranspiler.Result r = TypeScriptTranspiler.transpile(ts, "ai.ts");
		Assert.assertFalse("une erreur de syntaxe etait attendue", r.ok());
		SyntaxProblem p = r.firstDiagnostic();
		Assert.assertNotNull(p);
		Assert.assertTrue("message vide", p.message != null && !p.message.isEmpty());
		Assert.assertEquals("erreur localisee sur la 3e ligne", 3, p.startLine);
	}

	@Test
	public void typeOnlyConstructsVanish() {
		// type alias + annotation de parametre : rien a l'execution.
		String ts = "type Cell = number;\nfunction at(c: Cell): Cell { return c; }\nat(5);";
		TypeScriptTranspiler.Result r = TypeScriptTranspiler.transpile(ts, "ai.ts");
		Assert.assertTrue(r.ok());
		Assert.assertFalse(r.js.contains("type Cell"));
		Assert.assertTrue(r.js.contains("function at(c)"));
	}

	@Test
	public void validateSyntaxForFileReportsTsError() {
		// Validation orientee fichier (chemin daemon) : un .ts errone -> diagnostic localise, sans sandbox.
		SyntaxProblem p = PolyglotEntityAI.validateSyntaxForFile(
			"bot.ts", "const a: number = 1;\nfunction turn(): void { return ( }", null);
		Assert.assertNotNull("le .ts errone doit produire un diagnostic", p);
		Assert.assertEquals(2, p.startLine);
	}

	@Test
	public void validateSyntaxForFileAcceptsValidTs() {
		SyntaxProblem p = PolyglotEntityAI.validateSyntaxForFile(
			"bot.ts", "const a: number = 1;\nfunction turn(): void { debug('' + a); }", null);
		Assert.assertNull("un .ts valide ne doit produire aucun diagnostic", p);
	}

	@Test
	public void validateSyntaxForFileIgnoresNonPolyglot() {
		// Une extension non polyglot (LeekScript) n'est pas du ressort de cette validation.
		Assert.assertNull(PolyglotEntityAI.validateSyntaxForFile("bot.leek", "var x = ;", null));
	}
}
