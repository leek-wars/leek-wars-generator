package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;

import leekscript.runner.LeekRunException;

/**
 * Tests d'attaque (audit securite) : confirme que le sandbox bloque l'evasion hote ET que le
 * determinisme tient face a une IA hostile (l'invariant central : un combat doit se rejouer a
 * l'identique pour une seed donnee). Les IA ici sont adversariales.
 */
public class TestPolyglotSecurity extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Sec1");
		leek2 = defaultLeek(2, "Sec2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private PolyglotEntityAI ai(PolyglotSandbox sb, String lang, String src, long seed) {
		PolyglotEntityAI ai = new PolyglotEntityAI(lang, src, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		ai.getRandom().seed(seed);
		return ai;
	}

	private void assertThrows(PolyglotEntityAI ai) throws Exception {
		try {
			ai.runIA();
			Assert.fail("l'attaque aurait du echouer (acces refuse)");
		} catch (LeekRunException e) {
			// attendu : l'acces interdit remonte en erreur d'IA
		}
	}

	// ---------- Evasion hote : doit etre bloquee ----------

	@Test
	public void jsHostClassLookupUnavailable() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			// Java n'est pas expose (controle par allowAllAccess(false)).
			Assert.assertEquals("undefined", ai(sb, "js", "typeof Java", 1).runIA());
			assertThrows(ai(sb, "js", "Java.type('java.lang.System').exit(1)", 1));
		}
	}

	@Test
	public void pythonProcessAndFsBlocked() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			assertThrows(ai(sb, "python", "import subprocess\nsubprocess.run(['echo', 'pwned'])\n", 1));
			assertThrows(ai(sb, "python", "open('/etc/passwd').read()", 1));
			assertThrows(ai(sb, "python", "import os\nos.system('echo pwned')\n", 1));
		}
	}

	// ---------- Determinisme : doit tenir face a une IA hostile ----------

	@Test
	public void jsMathRandomIsFrozenAndSeeded() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			// Tentative de reecraser Math.random : doit etre ignoree (gele), et rester seede.
			String src = "function turn(){ try { Math.random = function(){ return 0.999; }; } catch(e){} return Math.random(); }";
			double ra = ((Number) ai(sb, "js", src, 5).runIA()).doubleValue();
			double rb = ((Number) ai(sb, "js", src, 5).runIA()).doubleValue();
			double rc = ((Number) ai(sb, "js", src, 6).runIA()).doubleValue();
			Assert.assertEquals("meme seed -> meme tirage", ra, rb, 0.0);
			Assert.assertNotEquals("la reassignation de Math.random doit etre ignoree (gele)", 0.999, ra, 0.0);
			Assert.assertNotEquals("seed differente -> tirage different", ra, rc, 0.0);
		}
	}

	@Test
	public void jsWallClockIsFixed() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals(0L, ((Number) ai(sb, "js", "Date.now()", 1).runIA()).longValue());
			Assert.assertEquals(0L, ((Number) ai(sb, "js", "new Date().getTime()", 1).runIA()).longValue());
			// #4006 leak via .constructor : new Date().constructor / Date.prototype.constructor pointaient
			// sur le Date d'origine -> doivent maintenant renvoyer l'horloge figee, pas l'heure reelle.
			Assert.assertEquals(0L, ((Number) ai(sb, "js", "new Date().constructor.now()", 1).runIA()).longValue());
			Assert.assertEquals(0L, ((Number) ai(sb, "js", "Date.prototype.constructor.now()", 1).runIA()).longValue());
			Assert.assertEquals(0L, ((Number) ai(sb, "js", "new (new Date().constructor)().getTime()", 1).runIA()).longValue());
		}
	}

	@Test
	public void pythonOsEntropyIsDeterministic() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			// Le vecteur CRITICAL : os.urandom doit etre re-route vers un PRNG seede.
			String src = "def turn():\n    import os\n    return os.urandom(8).hex()\n";
			String ra = (String) ai(sb, "python", src, 5).runIA();
			String rb = (String) ai(sb, "python", src, 5).runIA();
			String rc = (String) ai(sb, "python", src, 6).runIA();
			Assert.assertEquals("meme seed -> meme os.urandom", ra, rb);
			Assert.assertNotEquals("seed differente -> os.urandom different", ra, rc);
		}
	}

	@Test
	public void pythonNoArgSeedStaysDeterministic() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			// random.seed() sans argument ne doit PAS re-seeder depuis l'entropie OS.
			String src = "def turn():\n    import random\n    random.seed()\n    return random.random()\n";
			Object ra = ai(sb, "python", src, 5).runIA();
			Object rb = ai(sb, "python", src, 5).runIA();
			Assert.assertEquals(((Number) ra).doubleValue(), ((Number) rb).doubleValue(), 0.0);
		}
	}

	@Test
	public void pythonWallClockIsFixed() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals(0.0, ((Number) ai(sb, "python", "import time\ntime.time()", 1).runIA()).doubleValue(), 0.0);
			Assert.assertEquals("2020-01-01 00:00:00",
				ai(sb, "python", "import datetime\nstr(datetime.datetime.now())", 1).runIA());
			// #4006 les variantes _ns / gmtime / localtime n'etaient pas figees.
			Assert.assertEquals(0L, ((Number) ai(sb, "python", "import time\ntime.time_ns()", 1).runIA()).longValue());
			Assert.assertEquals(0L, ((Number) ai(sb, "python", "import time\ntime.monotonic_ns()", 1).runIA()).longValue());
			Assert.assertEquals(2020L, ((Number) ai(sb, "python", "import time\ntime.gmtime().tm_year", 1).runIA()).longValue());
			Assert.assertEquals(2020L, ((Number) ai(sb, "python", "import time\ntime.localtime().tm_year", 1).runIA()).longValue());
		}
	}
}
