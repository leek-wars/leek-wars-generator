package test;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.weapons.Weapons;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

/**
 * Armes 2.50 dans l'IA Quantum (repo ia, LeekScript) : duel miroir avec pour seule arme
 * la lance du soleil (42, ligne + EFFECT_REPEL) puis le sabre du désert (41, Stérile).
 * Valide en vrai combat : compilation des nouveaux fichiers (feature_repel), entrées
 * GCTU des deux armes (sinon debugE "No generator" et l'IA ne tire jamais), et usage
 * effectif (SET_WEAPON + dégâts). Dev-only : saute si le repo ia est absent (CI).
 */
public class TestQuantumNewWeapons extends FightTestBase {

	private static final String LS_DIR = "/home/pierre/dev/leek-wars/ia";
	private static final String ENTRYPOINT = "Quantum";

	private Leek leek1;
	private Leek leek2;
	private Folder aiRoot;

	/** Leek réaliste. Farmer != 0 : getFarmerID()==0 détourne le tour vers BotMessage (easter-egg). */
	private static Leek combatLeek(int id, String name) {
		return new Leek(id, name, 1, 150, 2500, 18, 6, 450, 200, 300, 100, 100, 0, 0, 8, 64,
			0, false, 0, 0, "", 0, "", "", "", 0);
	}

	@Override
	protected void createLeeks() {
		leek1 = combatLeek(1, "Piqueur");
		leek2 = combatLeek(2, "Cible");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** weaponItem = id d'ITEM (clé du catalogue Weapons : pistolet=37, sabre=429, lance=440). */
	private void setupQuantum(int weaponItem) throws Exception {
		org.junit.Assume.assumeTrue("repo ia absent, test saute (CI)", Files.isDirectory(Path.of(LS_DIR)));

		// Cache compilé keyé par id de fichier, pas par contenu : sans ça, une IA périmée
		// serait rejouée silencieusement après édition des sources.
		generator.setCache(false);

		// Sans le registre ai_level, AI_LEVEL=lambda et l'IA retombe sur Lambda_ai().
		registerStore.put(leek1.getId(), "{\"ai_level\":\"4\"}");
		registerStore.put(leek2.getId(), "{\"ai_level\":\"4\"}");

		var weapon = Weapons.getWeapon(weaponItem);
		Assert.assertNotNull("arme (item " + weaponItem + ") absente du catalogue data/weapons.json", weapon);
		leek1.addWeapon(weapon);
		leek2.addWeapon(weapon);

		// FileSystem enraciné sur le repo ia (résout les include() multi-fichiers).
		final Folder absRoot = new Folder(0, 0, LS_DIR, null, null, null, System.currentTimeMillis());
		absRoot.setParent(absRoot);
		absRoot.setRoot(absRoot);
		var nfs = new NativeFileSystem() {
			@Override public Folder getRoot() { return absRoot; }
			@Override public Folder getRoot(int owner) { return absRoot; }
			@Override public Folder getRoot(int owner, int farmer) { return absRoot; }
		};
		java.lang.reflect.Field fsField = Folder.class.getDeclaredField("fs");
		fsField.setAccessible(true);
		fsField.set(absRoot, nfs);
		LeekScript.setFileSystem(nfs);
		this.aiRoot = absRoot;

		AIFile ai = absRoot.resolve(ENTRYPOINT);
		for (Leek leek : new Leek[] { leek1, leek2 }) {
			leek.setAIFile(ai);
			leek.setLogs(new LeekLog(farmerLog, leek));
			leek.setFight(fight);
			leek.setBirthTurn(1);
		}

		// Map custom sans obstacle avec spawns imposés : sur la map générée (id 0),
		// setInitialCell est ignoré (Map.java) et les poireaux partent au hasard.
		// 342 = 306 + 2×18 : alignés en diagonale, à portée de lance dès l'engagement.
		var map = com.leekwars.generator.util.Json.createObject();
		map.put("id", 1);
		map.set("obstacles", com.leekwars.generator.util.Json.createObject());
		map.set("pattern", com.leekwars.generator.util.Json.createArray());
		var team1 = com.leekwars.generator.util.Json.createArray(); team1.add(306);
		var team2 = com.leekwars.generator.util.Json.createArray(); team2.add(342);
		map.set("team1", team1);
		map.set("team2", team2);
		fight.getState().setCustomMap(map);
		leek1.setInitialCell(306);
		leek2.setInitialCell(342);

		fight.setMaxTurns(20);
		fight.getState().seed(42);
	}

	private String runAndCheck(int weaponTemplate) throws Exception {
		runFight();

		String logs = farmerLog.toJSON().toString();
		String actions = fight.getState().getActions().toJSON().toString();

		// Un log de debug intéressant à l'œil nu
		int[] shown = {0};
		for (String line : logs.split("\\\\n|\",\"|\\],\\[")) {
			if (shown[0] < 25 && (line.contains("Best :") || line.contains("No generator")
				|| line.contains("not supported") || line.contains("Aucune action") || line.contains("failed!"))) {
				System.out.println("[new-weapons] LOG| " + line);
				shown[0]++;
			}
		}
		System.out.println("[new-weapons] tours=" + fight.getState().getOrder().getTurn()
			+ " | " + leek1.getName() + " vie=" + leek1.getLife() + " cell=" + leek1.getCell()
			+ " | " + leek2.getName() + " vie=" + leek2.getLife() + " cell=" + leek2.getCell());

		// Table GCTU : une entrée manquante rend l'arme muette, en silence côté combat.
		Assert.assertFalse("[GCTU] No generator dans les logs : entrée de table manquante",
			logs.contains("No generator"));
		// Effet sans positivité : debugE "Effect X not supported!" à l'init des items.
		Assert.assertFalse("Effet sans positivité déclarée (EFFECT_POSITIVITY)",
			logs.contains("not supported!"));
		// L'arme a bien été dégainée et utilisée (SET_WEAPON template puis dégâts).
		Assert.assertTrue("SET_WEAPON " + weaponTemplate + " absent des actions",
			actions.contains("[13," + weaponTemplate + "]"));
		Assert.assertTrue("aucun dégât : l'arme n'a jamais touché",
			leek1.getLife() < 2500 || leek2.getLife() < 2500);
		return actions;
	}

	/** Test unitaire LS : scénarios synthétiques, pipeline hit_map → actions →
	 * state.use, sans l'exploration complète. Entrée minimaliste (la suite
	 * test/test.leek entière, 190 fichiers, fait échouer le javac du harnais). */
	@Test
	public void unitTests() throws Exception {
		setupQuantum(440);
		// Entrée = vrai fichier du repo ia, résolu via le FS : un AIFile créé à la main
		// n'a pas de dossier et fait NPE les include() à la compilation Java.
		AIFile entry = aiRoot.resolve("test/harness_new_weapons.leek");
		// Compilation directe d'abord : dans le combat, l'exception javac est avalée
		// (pas d'errorManager en test) et ne laisse qu'un code 62 muet dans les logs.
		entry.setJavaClass("AI_TSS_probe");
		entry.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
		entry.compile(new leekscript.compiler.Options(entry.getVersion(), entry.isStrict(), false, true, null, true));
		leek1.setAIFile(entry);
		// leek2 inerte : pas besoin du Quantum complet en face
		leek2.setAIFile(new AIFile("<dummy>", "", 0, LeekScript.LATEST_VERSION, leek2.getId(), false));
		fight.setMaxTurns(1);
		runFight();

		String logs = farmerLog.toJSON().toString();
		String[] lines = logs.split("\\\\n|\",\"|\\],\\[");
		boolean ok = logs.contains("[TSS] OK use");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			// En cas de plantage silencieux (compile/runtime), tout montrer (borné)
			if (line.contains("[TSS]") || (!ok && i < 40)) {
				System.out.println("[unit] " + line);
			}
		}
		Assert.assertTrue("le run n'est pas allé au bout", logs.contains("[TSS] terminé"));
		Assert.assertFalse("échecs LS, voir [TSS] FAIL ci-dessus", logs.contains("[TSS] FAIL"));
		Assert.assertTrue("aucune action utilisée", ok);
	}

	/** Régénère la table GCTU via l'outil du repo ia (gctu_generator_generator.leek)
	 * et dump le fichier généré : sert à vérifier/mettre à jour gctu_generators.leek
	 * sans passer par le site. Le résultat est écrit dans /tmp/gctu_regen_log.json. */
	@Test
	public void gctuRegeneration() throws Exception {
		setupQuantum(440);
		AIFile entry = aiRoot.resolve("test/harness_gctu_regen.leek");
		// L'outil est calibré pour le budget du site : large budget d'ops ici
		leek1.setCores(64);
		leek1.setAIFile(entry);
		leek2.setAIFile(new AIFile("<dummy>", "", 0, LeekScript.LATEST_VERSION, leek2.getId(), false));
		fight.setMaxTurns(12);
		runFight();

		String logs = farmerLog.toJSON().toString();
		java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/gctu_regen_log.json"), logs);
		System.out.println("[gctu-regen] log " + logs.length() + " chars -> /tmp/gctu_regen_log.json"
			+ " | dump présent = " + logs.contains("GENERATED FILE"));
		Assert.assertTrue("le dump du fichier généré n'apparaît pas dans les logs",
			logs.contains("GENERATED FILE"));
	}

	/** Témoin : arme historique (pistolet, template 1) — si Quantum n'attaque pas ici,
	 * le problème est dans le harnais, pas dans l'intégration des nouvelles armes. */
	@Test
	public void pistolControlDuel() throws Exception {
		setupQuantum(37);
		runAndCheck(1);
	}

	@Test
	public void sunSpearMirrorDuel() throws Exception {
		setupQuantum(440);
		runAndCheck(42);
	}

	@Test
	public void desertSaberMirrorDuel() throws Exception {
		setupQuantum(429);
		String actions = runAndCheck(41);
		// Le sabre applique l'état Stérile (ADD_STATE 59, state 12) : l'effet doit
		// apparaître dans le log d'actions (ADD_WEAPON_EFFECT = 301, [type, itemID, ...]).
		Assert.assertTrue("aucun effet d'arme loggé : Stérile pas appliqué ?",
			actions.contains("[301,429,") || actions.contains("[301,41,"));
	}
}
