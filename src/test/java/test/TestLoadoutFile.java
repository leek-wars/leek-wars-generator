package test;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.classes.LoadoutClass;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.Scenario;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.IACompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.compiler.resolver.FileSystem;

/**
 * `getLoadout("chemin.json")` : un équipement écrit en JSON dans les fichiers du joueur, lu à la
 * compilation (chemin relatif au fichier appelant, comme un include) et rendu avec ses items
 * convertis en ids (WEAPON_*, CHIP_*, template des composants).
 */
public class TestLoadoutFile extends FightTestBase {

	private static final String TANK = """
		{
			"type": "leek-wars-loadout",
			"version": 1,
			"name": "Tank \\"lourd\\"",
			"icon": "🔥",
			"weapons": ["weapon_m_laser", "pistol", 47, "inconnue"],
			"forgotten_weapons": [],
			"chips": ["chip_mutation", "flash", "rock"],
			"components": [{"slot": 2, "item": "core3", "stats": {"strength": 12}}],
			"stats": {"strength": 5000, "ram": 200, "bidon": 3}
		}""";

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, defaultLeek(2, "L2"));
	}

	private String run(Map<String, String> files, String entry) throws Exception {
		LeekScript.setFileSystem(new TestPolyglotMultiFile.MemFileSystem(0, files));
		try {
			var ai = (EntityAI) LeekScript.compileFile(entry, "com.leekwars.generator.fight.entity.EntityAI", new Options(true));
			ai.setEntity(leek1);
			leek1.setAI(ai); // les avertissements du rapport lisent la position dans l'IA de l'entité
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			return ai.export(ai.runIA());
		} finally {
			LeekScript.resetFileSystem();
		}
	}

	private Error firstError(Map<String, String> files, String entry) throws Exception {
		LeekScript.setFileSystem(new TestPolyglotMultiFile.MemFileSystem(0, files));
		try {
			var file = LeekScript.getFileSystem().getRoot().resolve(entry);
			var result = new IACompiler().analyze(file);
			// Une entrée : [niveau, fichier, ligne, colonne, ligne de fin, colonne de fin, erreur, paramètres].
			var errors = new java.util.ArrayList<Error>();
			for (var info : result.informations) errors.add(Error.values()[info.get(6).asInt()]);
			return errors.isEmpty() ? null : errors.get(0);
		} finally {
			LeekScript.resetFileSystem();
		}
	}

	@Test
	public void readsLoadoutWithIds() throws Exception {
		var result = run(Map.of(
			"t1/main.leek", "var l = getLoadout('builds/tank.json')\nreturn [l.name, l.weapons, l.chips, l.components[0].item, l.components[0].slot, l.components[0].stats.strength, l.stats.strength, l.stats.ram, l.stats.life]",
			"t1/builds/tank.json", TANK
		), "t1/main.leek");
		// m_laser = 47, pistol = 37 (doublon 47 retiré, inconnue ignorée) ; mutation 159, flash 6, rock 7 ; core3 = 292.
		Assert.assertEquals("[\"Tank \"lourd\"\", [47, 37], [159, 6, 7], 292, 2, 12, 5000, 200, 0]", result);
	}

	@Test
	public void pathRelativeToIncludedFile() throws Exception {
		var result = run(Map.of(
			"t2/main.leek", "include('lib/stuff.leek')\nreturn build().weapons",
			"t2/lib/stuff.leek", "function build() { return getLoadout('../builds/tank.json') }",
			"t2/builds/tank.json", TANK
		), "t2/main.leek");
		Assert.assertEquals("[47, 37]", result);
	}

	@Test
	public void severalLoadoutsGiveAnArray() throws Exception {
		var result = run(Map.of(
			"t3/main.leek", "var all = getLoadout('all.json')\nreturn [count(all), all[1].name, all[1].chips]",
			"t3/all.json", "{\"type\": \"leek-wars-loadouts\", \"version\": 1, \"loadouts\": [{\"name\": \"A\", \"weapons\": [], \"chips\": []}, {\"name\": \"B\", \"weapons\": [], \"chips\": [\"chip_flash\"]}]}"
		), "t3/main.leek");
		Assert.assertEquals("[2, \"B\", [6]]", result);
	}

	@Test
	public void jsonFileIsACompileDependency() throws Exception {
		// Modifier le .json doit invalider l'IA compilée, comme un fichier inclus.
		var files = Map.of(
			"t7/main.leek", "return getLoadout('builds/tank.json')",
			"t7/builds/tank.json", TANK
		);
		var mem = new TestPolyglotMultiFile.MemFileSystem(0, files);
		// Le graphe des includes résout depuis le dossier de chaque fichier listé : on les liste résolus.
		var fs = new FileSystem() {
			@Override public Iterable<AIFile> listAllFiles(int owner) {
				var list = new java.util.ArrayList<AIFile>();
				for (var path : files.keySet()) {
					try { list.add(mem.getRoot().resolve(path)); } catch (Exception e) { throw new RuntimeException(e); }
				}
				return list;
			}
			@Override public Folder getRoot() { return mem.getRoot(); }
			@Override public Folder getRoot(int owner) { return mem.getRoot(); }
			@Override public Folder getRoot(int owner, int farmer) { return mem.getRoot(); }
			@Override public AIFile getFileById(int id, int farmer) { return null; }
			@Override public Folder getFolderById(int id, int farmer) { return mem.getRoot(); }
			@Override public Folder findFolder(String name, Folder folder) { return mem.findFolder(name, folder); }
			@Override public AIFile findFile(String name, Folder folder) throws java.io.FileNotFoundException { return mem.findFile(name, folder); }
			@Override public long getAITimestamp(AIFile ai) { return 1L; }
			@Override public void loadDependencies(AIFile ai) { }
			@Override public long getFolderTimestamp(Folder folder) { return 1L; }
		};
		var main = fs.getRoot().resolve("t7/main.leek");
		var paths = fs.loadIncludedAIs(main).stream().map(f -> f.getPath()).toList();
		Assert.assertEquals(java.util.List.of("t7/builds/tank.json"), paths);
	}

	@Test
	public void legacyVersion() throws Exception {
		var result = run(Map.of(
			"t8/main.leek", "// @version:3\nvar l = getLoadout('tank.json')\nreturn [l['weapons'], l['stats']['strength']]",
			"t8/tank.json", TANK
		), "t8/main.leek");
		Assert.assertEquals("[[47, 37], 5000]", result);
	}

	@Test
	public void functionAsValueIsRefused() throws Exception {
		Assert.assertEquals(Error.FILE_PATH_EXPECTED, firstError(Map.of(
			"t9/main.leek", "var f = getLoadout\nreturn f('a.json')",
			"t9/a.json", TANK
		), "t9/main.leek"));
	}

	@Test
	public void tooLargeFileIsRefused() throws Exception {
		Assert.assertEquals(Error.FILE_TOO_LARGE, firstError(Map.of(
			"t10/main.leek", "return getLoadout('big.json')",
			"t10/big.json", "{\"name\": \"" + "é".repeat(70_000) + "\"}"
		), "t10/main.leek"));
	}

	@Test
	public void manyCallsShareOneCopy() throws Exception {
		// Le contenu est émis une fois : plusieurs appels d'un fichier de 60 000 caractères non ASCII
		// compilent sans multiplier le code généré.
		var calls = new StringBuilder("var n = 0\n");
		for (int i = 0; i < 5; i++) calls.append("n += count(getLoadout('big.json').weapons)\n");
		calls.append("return n");
		var result = run(Map.of(
			"t11/main.leek", calls.toString(),
			"t11/big.json", "{\"name\": \"" + "é".repeat(60_000) + "\", \"weapons\": [\"pistol\"]}"
		), "t11/main.leek");
		Assert.assertEquals("5", result);
	}

	@Test
	public void hugeNumbersAreIgnored() throws Exception {
		var result = run(Map.of(
			"t12/main.leek", "var l = getLoadout('n.json')\nreturn [l.weapons, l.chips, l.components[0].slot, l.components[0].stats, l.stats.strength]",
			"t12/n.json", "{\"weapons\": [5000000000, 1e300, 37], \"chips\": [\"99999999999\", \"6\"], \"components\": [{\"item\": \"core3\", \"slot\": 1e10, \"stats\": {\"tp\": 1e300, \"mp\": \"2\"}}, {\"item\": 292}], \"stats\": {\"strength\": 1e300}}"
		), "t12/main.leek");
		Assert.assertEquals("[[37], [6], 0, {mp: 2}, 1000000000]", result);
	}

	@Test
	public void invalidJsonGivesNull() throws Exception {
		var result = run(Map.of(
			"t4/main.leek", "return getLoadout('bad.json')",
			"t4/bad.json", "{ pas du json"
		), "t4/main.leek");
		Assert.assertEquals("null", result);
	}

	@Test
	public void missingFileIsACompileError() throws Exception {
		Assert.assertEquals(Error.FILE_NOT_FOUND, firstError(Map.of(
			"t5/main.leek", "return getLoadout('nope.json')"
		), "t5/main.leek"));
	}

	@Test
	public void pathMustBeALiteral() throws Exception {
		Assert.assertEquals(Error.FILE_PATH_EXPECTED, firstError(Map.of(
			"t6/main.leek", "var p = 'a.json'\nreturn getLoadout(p)",
			"t6/a.json", TANK
		), "t6/main.leek"));
	}

	@Test
	public void firstLoadoutWithoutAI() {
		// Lecture du worker pour un poireau de scénario de test : mêmes ids que getLoadout.
		var l = LoadoutClass.firstLoadout(TANK);
		Assert.assertEquals("Tank \"lourd\"", l.get("name").asString());
		Assert.assertEquals("[47,37]", l.get("weapons").toString());
		Assert.assertEquals("[159,6,7]", l.get("chips").toString());
		Assert.assertEquals(292, l.get("components").get(0).get("item").asInt());
		Assert.assertEquals(12, l.get("components").get(0).get("stats").get("strength").asInt());
		Assert.assertEquals(5000, l.get("stats").get("strength").asInt());
		Assert.assertEquals(0, l.get("stats").get("life").asInt());
	}

	@Test
	public void firstLoadoutOfSeveral() {
		Assert.assertEquals("A", LoadoutClass.firstLoadout("{\"loadouts\": [1, {\"name\": \"A\"}, {\"name\": \"B\"}]}").get("name").asString());
		Assert.assertEquals("B", LoadoutClass.firstLoadout("[\"x\", {\"name\": \"B\"}]").get("name").asString());
	}

	@Test
	public void firstLoadoutOfNothing() {
		Assert.assertNull(LoadoutClass.firstLoadout("{ pas du json"));
		Assert.assertNull(LoadoutClass.firstLoadout("[]"));
		Assert.assertNull(LoadoutClass.firstLoadout("\"tank\""));
		Assert.assertNull(LoadoutClass.firstLoadout("{\"loadouts\": [1, 2]}"));
	}

	@Test
	public void entryLoadoutNameReachesTheEntity() {
		// Nom de l'ensemble porté à l'entrée (scénario de test) : lu du JSON, rendu par toJson, et
		// posé sur l'entité pour le rapport.
		var json = (tools.jackson.databind.node.ObjectNode) leekscript.util.Json.parse(
			"{\"name\": \"T\", \"level\": 10, \"life\": 100, \"tp\": 10, \"mp\": 3, \"strength\": 0, \"farmer\": -1, \"loadout\": \"tank.json\"}");
		var info = new EntityInfo(json);
		Assert.assertEquals("tank.json", info.loadoutName);
		Assert.assertEquals("tank.json", info.toJson().get("loadout").asString());
		Assert.assertEquals("tank.json", info.createEntity(null, new Scenario(), null).getLoadoutName());
		info.loadoutName = null;
		Assert.assertNull(info.createEntity(null, new Scenario(), null).getLoadoutName());
		Assert.assertFalse(info.toJson().has("loadout"));
	}
}
