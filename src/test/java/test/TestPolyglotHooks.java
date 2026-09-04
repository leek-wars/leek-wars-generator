package test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotFileSystem;
import com.leekwars.generator.polyglot.PolyglotSandbox;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.FightLoadout;
import com.leekwars.generator.weapons.Weapons;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * Hooks de combat {@code beforeFight()} / {@code afterFight()} pour les IA polyglot (JS / Python),
 * a parite avec LeekScript (cf {@link TestHooks}). Verifie la detection du hook, son execution, la
 * phase (actions interdites, stats adverses masquees), l'absence d'effet de bord sur le modele
 * d'execution (top-level charge une seule fois, turn() rejouee ensuite) et le bout-en-bout via
 * {@code Fight.startFight}.
 */
public class TestPolyglotHooks extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "HookLeek");
		leek2 = defaultLeek(2, "Dummy");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private PolyglotEntityAI buildAI(PolyglotSandbox sandbox, String language, String source) {
		PolyglotEntityAI ai = new PolyglotEntityAI(language, source, sandbox);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	private PolyglotEntityAI multiFileAI(PolyglotSandbox sb, String lang, Map<String, String> files, String entryPath) {
		Path passthrough = "python".equals(lang) ? PolyglotSandbox.pythonStdlibRoot() : null;
		List<String> probe = "js".equals(lang) ? PolyglotFileSystem.JS_PROBE_EXTENSIONS : List.of();
		PolyglotFileSystem fs = new PolyglotFileSystem(files.keySet(), files::get, passthrough, probe, entryPath);
		PolyglotEntityAI ai = new PolyglotEntityAI(lang, files.get(entryPath), entryPath, fs, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai;
	}

	// ---- Detection ----

	@Test
	public void hasHookFalseWhenSourceIgnoresHooks() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sb, "js", "function turn() { return 1; }");
			Assert.assertFalse(ai.hasHook("beforeFight"));
			Assert.assertFalse(ai.hasHook("afterFight"));
		}
	}

	@Test
	public void hasHookTrueBeforeFirstTurn() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Avant le tour 1 le source n'est pas encore evalue : la detection passe par le
			// pre-filtre textuel, sans construire de contexte.
			EntityAI ai = buildAI(sb, "js", "function beforeFight() {} function turn() {}");
			Assert.assertTrue(ai.hasHook("beforeFight"));
			Assert.assertFalse(ai.hasHook("afterFight"));
		}
	}

	@Test
	public void hasHookOnlyKnowsFightHooks() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sb, "js", "function notDefined() {}");
			Assert.assertFalse(ai.hasHook("notDefined"));
		}
	}

	@Test
	public void hasHookIsExactOnceLoaded() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Le nom cite en commentaire suffit au pre-filtre, mais une fois le source evalue la
			// reponse redevient exacte : pas de fonction, pas de hook.
			EntityAI ai = buildAI(sb, "js", "function turn() { return 1; } // beforeFight non defini");
			Assert.assertTrue(ai.hasHook("beforeFight"));
			ai.runIA();
			Assert.assertFalse(ai.hasHook("beforeFight"));
		}
	}

	// ---- Execution ----

	@Test
	public void beforeFightRunsJs() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sb, "js",
				"function beforeFight() { Registers.set('hook', 'yes'); }\n"
				+ "function turn() {}\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("yes", leek1.getRegister("hook"));
			Assert.assertEquals(EntityAI.HookPhase.NONE, ai.getHookPhase());
		}
	}

	@Test
	public void beforeFightRunsPython() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("python")) {
			EntityAI ai = buildAI(sb, "python",
				"def beforeFight():\n    Registers.set('hook', 'yes')\n\ndef turn():\n    pass\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("yes", leek1.getRegister("hook"));
		}
	}

	@Test
	public void afterFightRunsJs() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sb, "js",
				"function afterFight() { Registers.set('winner', '' + Fight.winner); }\n"
				+ "function turn() {}\n");
			ai.runHook("afterFight", EntityAI.HookPhase.AFTER_FIGHT);
			Assert.assertEquals("-1", leek1.getRegister("winner")); // combat non termine
		}
	}

	@Test
	public void jsEsModuleExportedHookIsResolved() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("strategie.mjs", "export function choix() { return 'esm'; }\n");
		files.put("main.mjs",
			"import { choix } from './strategie.mjs';\n"
			+ "export function beforeFight() { Registers.set('hook', choix()); }\n"
			+ "export function turn() {}\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Dans un module ES, une fonction top-level est module-scoped : le hook n'est visible
			// que par les exports du module, comme turn().
			EntityAI ai = multiFileAI(sb, "js", files, "main.mjs");
			Assert.assertTrue(ai.hasHook("beforeFight"));
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("esm", leek1.getRegister("hook"));
		}
	}

	@Test
	public void hookDeclaredInImportedFileIsDetected() throws Exception {
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("hooks.mjs", "globalThis.beforeFight = function() { Registers.set('hook', 'importe'); };\n");
		files.put("main.mjs", "import './hooks.mjs';\nglobalThis.turn = function() {};\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Le pre-filtre scanne aussi les fichiers importables, pas seulement l'entree.
			EntityAI ai = multiFileAI(sb, "js", files, "main.mjs");
			Assert.assertTrue(ai.hasHook("beforeFight"));
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("importe", leek1.getRegister("hook"));
		}
	}

	// ---- Modele d'execution ----

	@Test
	public void topLevelRunsOnceWhenHookLoadsEntryEarly() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Le hook charge le source avant le tour 1 : le top-level ne doit pas etre rejoue au tour 1,
			// seule turn() l'est (modele d'execution inchange).
			EntityAI ai = buildAI(sb, "js",
				"globalThis.loads = (globalThis.loads || 0) + 1;\n"
				+ "function beforeFight() { Registers.set('loadsHook', '' + globalThis.loads); }\n"
				+ "function turn() { return globalThis.loads; }\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("1", leek1.getRegister("loadsHook"));
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
		}
	}

	@Test
	public void hookStateSurvivesIntoTurns() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// L'etat pose par beforeFight() vit dans le meme contexte que les tours.
			EntityAI ai = buildAI(sb, "js",
				"var plan = null;\n"
				+ "function beforeFight() { plan = 'agressif'; }\n"
				+ "function turn() { return plan; }\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("agressif", ai.runIA());
		}
	}

	// ---- IA "plate" (sans turn()) : le top-level ne doit JAMAIS tourner en phase de hook ----

	@Test
	public void flatAiIsNeverLoadedByAHook() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Le nom du hook apparait (commentaire), mais l'IA n'a pas de turn() : son top-level EST la
			// logique de tour. Le charger avant le combat lui ferait perdre son tour -> pas de hook.
			EntityAI ai = buildAI(sb, "js",
				"// pas de beforeFight ici\n"
				+ "Registers.set('runs', '' + (1 + Number(Registers.get('runs') || 0)));\n");
			Assert.assertFalse(ai.hasHook("beforeFight"));
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertNull("le top-level ne doit pas avoir tourne", leek1.getRegister("runs"));
			ai.runIA();
			Assert.assertEquals("1", leek1.getRegister("runs"));
		}
	}

	@Test
	public void flatAiPassingThePrefilterKeepsItsFirstTurn() throws Exception {
		leek1.addWeapon(Weapons.getWeapon(FightConstants.WEAPON_PISTOL.getIntValue()));
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Les deux noms sont cites sans etre definis : le pre-filtre laisse passer, mais le
			// chargement speculatif ne trouve pas de turn() -> il est JETE, et le tour 1 recharge le
			// source dans un contexte neuf, actions autorisees.
			EntityAI ai = buildAI(sb, "js",
				"// ni beforeFight ni turn, juste cites\n"
				+ "Fight.me.setWeapon(Weapon.pistol);\n");
			Assert.assertTrue(ai.hasHook("beforeFight"));
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertNull("aucune action pendant la phase de hook", leek1.getWeapon());
			ai.runIA();
			Assert.assertNotNull("le tour 1 doit faire son travail", leek1.getWeapon());
		}
	}

	@Test
	public void flatEsModuleIsReExecutedAfterADiscardedHookLoad() throws Exception {
		// Un module ES n'est evalue qu'UNE fois par contexte : si le chargement speculatif le gardait,
		// l'IA plate resterait inerte tout le combat (replayFlatTurn ne rejoue pas un module). Le rejet
		// ferme le contexte, donc le tour 1 reimporte vraiment le module.
		leek1.addWeapon(Weapons.getWeapon(FightConstants.WEAPON_PISTOL.getIntValue()));
		initFightOnly();
		Map<String, String> files = new HashMap<>();
		files.put("lib.mjs", "export var plan = 'ok'; // beforeFight, turn : cites, pas definis\n");
		files.put("main.mjs", "import { plan } from './lib.mjs';\nFight.me.setWeapon(Weapon.pistol);\n");
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			EntityAI ai = multiFileAI(sb, "js", files, "main.mjs");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertNull("aucune action pendant la phase de hook", leek1.getWeapon());
			ai.runIA();
			Assert.assertNotNull("le module doit etre reevalue au tour 1", leek1.getWeapon());
		}
	}

	@Test
	public void topLevelActionIsNotSwallowedByTheHookLoad() throws Exception {
		leek1.addWeapon(Weapons.getWeapon(FightConstants.WEAPON_PISTOL.getIntValue()));
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Regression : le code a la racine du fichier agissait pendant beforeFight, ou les actions
			// sont interdites -> "setWeapon() ne peut pas etre appelee dans un hook" ET arme perdue.
			// Le chargement speculatif est jete des qu'il tente une action : le tour 1 la rejoue.
			EntityAI ai = buildAI(sb, "js",
				"Fight.me.setWeapon(Weapon.pistol);\n"
				+ "function beforeFight() { Registers.set('hook', 'yes'); }\n"
				+ "function turn() {}\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertNull("l'arme ne doit pas etre equipee pendant le hook", leek1.getWeapon());
			ai.runIA();
			Assert.assertNotNull("setWeapon() a la racine doit s'appliquer au tour 1", leek1.getWeapon());
		}
	}

	@Test
	public void pureSetupTopLevelStillAllowsTheHook() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Contre-epreuve : un top-level qui n'agit pas (du pur setup) reste charge par le hook,
			// et le tour 1 ne rejoue que turn().
			EntityAI ai = buildAI(sb, "js",
				"globalThis.plan = 'ok';\n"
				+ "globalThis.loads = (globalThis.loads || 0) + 1;\n"
				+ "function beforeFight() { Registers.set('hook', globalThis.plan); }\n"
				+ "function turn() { return globalThis.loads; }\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("ok", leek1.getRegister("hook"));
			Assert.assertEquals(1L, ((Number) ai.runIA()).longValue());
		}
	}

	// ---- Phase de hook ----

	@Test
	public void combatActionIsDeniedDuringHook() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Les actions de combat sont interdites en phase de hook, y compris via le bridge polyglot.
			EntityAI ai = buildAI(sb, "js",
				"function beforeFight() { Registers.set('move', '' + Fight.me.moveToward(Fight.me.cell)); }\n"
				+ "function turn() {}\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("0", leek1.getRegister("move"));
		}
	}

	@Test
	public void beforeFightMasksOpponentStats() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			EntityAI ai = buildAI(sb, "js",
				"function beforeFight() {"
				+ "  var e = Fight.getNearestEnemy();"
				+ "  Registers.set('enemy', e == null ? 'null' : 'visible');"
				+ "  Registers.set('force', e.strength == null ? 'null' : 'visible');"
				+ "  Registers.set('self', Fight.me.strength == null ? 'null' : 'visible');"
				+ "}\n"
				+ "function turn() {}\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			// L'adversaire reste identifiable (info de lobby), mais ses stats sont masquees ;
			// les siennes propres restent lisibles. Cf spec §2.2 (symetrie de l'ordre d'execution).
			Assert.assertEquals("visible", leek1.getRegister("enemy"));
			Assert.assertEquals("null", leek1.getRegister("force"));
			Assert.assertEquals("visible", leek1.getRegister("self"));
		}
	}

	@Test
	public void setLoadoutAppliesFromHook() throws Exception {
		var stats = new HashMap<Integer, Integer>();
		stats.put(Entity.STAT_LIFE, 800);
		stats.put(Entity.STAT_STRENGTH, 250);
		leek1.addLoadout(new FightLoadout("pvp", List.of(), List.of(), List.of(), stats));
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// L'usage nominal du hook : choisir son equipement au vu de l'adversaire.
			EntityAI ai = buildAI(sb, "js",
				"function beforeFight() { Registers.set('ok', Fight.me.setLoadout('pvp') ? 'yes' : 'no'); }\n"
				+ "function turn() {}\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals("yes", leek1.getRegister("ok"));
			Assert.assertEquals(250, leek1.getStat(Entity.STAT_STRENGTH));
			Assert.assertEquals(800, leek1.getTotalLife());
		}
	}

	// ---- Erreurs ----

	@Test
	public void hookErrorDoesNotBreakTheFight() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Une exception dans le hook est rapportee comme erreur d'IA, pas propagee au combat.
			EntityAI ai = buildAI(sb, "js",
				"function beforeFight() { throw new Error('boum'); }\n"
				+ "function turn() { return 42; }\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals(EntityAI.HookPhase.NONE, ai.getHookPhase());
			Assert.assertEquals(42L, ((Number) ai.runIA()).longValue());
		}
	}

	@Test
	public void syntaxErrorInHookLoadDoesNotBreakTheFight() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Le chargement du source est avance dans le hook : s'il echoue, l'erreur reste cote joueur.
			EntityAI ai = buildAI(sb, "js", "function beforeFight() { ,,, }\n");
			ai.runHook("beforeFight", EntityAI.HookPhase.BEFORE_FIGHT);
			Assert.assertEquals(EntityAI.HookPhase.NONE, ai.getHookPhase());
		}
	}

	// ---- Bout en bout ----

	@Test
	public void hooksRunInRealFight() throws Exception {
		attachJsAI(leek1,
			"function beforeFight() { Registers.set('before', 'ran'); }\n"
			+ "function afterFight() { Registers.set('after', '' + Fight.winner); }\n"
			+ "function turn() { Registers.set('turn', 'ran'); }\n");
		attachAI(leek2, ""); // adversaire LeekScript inerte
		runFight();

		Assert.assertTrue("leek1 doit utiliser une IA polyglot", leek1.getAI() instanceof PolyglotEntityAI);
		String registers = registerStore.get(leek1.getId());
		Assert.assertNotNull("les registres de leek1 doivent etre persistes", registers);
		Assert.assertTrue("beforeFight() doit s'etre execute : " + registers, registers.contains("\"before\""));
		Assert.assertTrue("turn() doit s'etre execute : " + registers, registers.contains("\"turn\""));
		Assert.assertTrue("afterFight() doit s'etre execute : " + registers, registers.contains("\"after\""));
	}

	@Test
	public void aiWithoutHooksIsUntouchedByRealFight() throws Exception {
		attachJsAI(leek1, "function turn() { Registers.set('turn', 'ran'); }");
		attachAI(leek2, "");
		runFight();

		String registers = registerStore.get(leek1.getId());
		Assert.assertNotNull(registers);
		Assert.assertTrue("turn() doit s'etre execute : " + registers, registers.contains("\"turn\""));
	}

	@Test
	public void typescriptRootLevelActionSurvivesTheHookInRealFight() throws Exception {
		// Le bug tel que rapporte : une IA TypeScript avec du code A LA RACINE du fichier voyait ses
		// actions refusees ("setWeapon() ne peut pas etre appelee dans un hook") alors que le joueur
		// n'est pas dans un hook. Bout en bout, l'arme doit bien etre equipee.
		leek1.addWeapon(Weapons.getWeapon(FightConstants.WEAPON_PISTOL.getIntValue()));
		attachTsAI(leek1,
			"function beforeFight(): void { Registers.set('before', 'ran'); }\n"
			+ "function turn(): void {}\n"
			+ "Fight.me.setWeapon(Weapon.pistol);\n");
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue("leek1 doit utiliser une IA polyglot", leek1.getAI() instanceof PolyglotEntityAI);
		Assert.assertNotNull("setWeapon() a la racine doit s'appliquer", leek1.getWeapon());
		// Corollaire assume : un top-level qui agit interdit le hook (l'appeler demanderait de faire
		// tourner ce top-level avant le combat).
		String registers = registerStore.get(leek1.getId());
		Assert.assertTrue("beforeFight() ne doit pas avoir tourne : " + registers,
			registers == null || !registers.contains("\"before\""));
	}

	/** Attache une IA TypeScript a un poireau (transpilee au build, puis executee par le moteur js). */
	private void attachTsAI(Leek leek, String code) {
		AIFile file = new AIFile("polyglot_hooks_" + System.nanoTime() + ".ts", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	/** Attache une IA JS a un poireau via un AIFile dont le chemin se termine par .js. */
	private void attachJsAI(Leek leek, String code) {
		AIFile file = new AIFile("polyglot_hooks_" + System.nanoTime() + ".js", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}
}
