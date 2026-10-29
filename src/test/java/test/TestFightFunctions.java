package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.attack.DamageType;
import com.leekwars.generator.component.Components;
import com.leekwars.generator.entity.Bulb;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.FightConstants;
import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.AILog;
import leekscript.LSException;
import leekscript.compiler.AIFile;
import leekscript.common.Error;
import leekscript.compiler.LeekScript;
import leekscript.compiler.Options;
import leekscript.runner.AI;
import leekscript.runner.LeekConstants;
import leekscript.runner.values.LegacyArrayLeekValue;

public class TestFightFunctions {

	private Generator generator;
	private Fight mFight;
	private Leek mLeek1;
	private Leek mLeek2;
	private AI ai;

	// Test demandant de modifier la carte et les positions des joueurs

	@Before
	public void setUp() throws Exception {

		generator = new Generator();
		mFight = new Fight(generator);
		mLeek1 = new Leek(1, "Test", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		mLeek2 = new Leek(2, "Bob", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);

		mFight.getState().addEntity(0, mLeek1);
		mFight.getState().addEntity(1, mLeek2);

		mFight.initFight();
		mFight.setStatisticsManager(new LocalTrophyManager()); // Un vrai déplacement en a besoin
		ai = new DefaultUserAI();
	}

	// Test de retour sur des structures du leekscript

	@Test
	public void nullPointerTest() throws Exception {

		Fight fight = new Fight(generator);
		Leek leek1 = new Leek(1, "Test");
		Leek leek2 = new Leek(2, "Bob");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);

		fight.initFight();

		Map map = fight.getState().getMap();
		map.clear();
		// map.getCell(203).setPlayer(leek2);
		// map.getCell(306).setPlayer(leek1);

		leek2.removeLife(leek2.getLife(), 0, leek1, DamageType.DIRECT, null, null);

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test moveToward
		codes.add("moveToward(1)");
		values.add(0);

		// Test moveToward
		codes.add("moveTowardCell(getCell(1))");
		values.add(0);

		// Test getPath
		codes.add("count(getPath(getCell(), getCell(1)))");
		values.add(17);

		// Test moveToward
		codes.add("getPath(33333, 5555)");
		values.add(null);

		// Test getPath
		codes.add("getPathLength(33333, 5555)");
		values.add(null);

		// Test moveAwayFrom
		codes.add("moveAwayFrom(3333)");
		values.add(0);

		// Test moveAwayFrom
		codes.add("moveAwayFrom(1)");
		values.add(0);

		// Test moveAwayFrom
		codes.add("moveAwayFrom(0)");
		values.add(0);

		// Test moveAwayFromCell
		codes.add("moveAwayFromCell(0)");
		values.add(0);

		// Test moveAwayFromCell
		codes.add("moveAwayFromCell(0)");
		values.add(0);

		// Test getDistance
		codes.add("getDistance(-1, -1)");
		values.add(-1);

		// Test getCellDistance
		codes.add("getCellDistance(-1, -1)");
		values.add(-1);

		// Test moveTowardCells
		codes.add("moveTowardCells([-1, -4])");
		values.add(0);

		// Test moveTowardCells
		codes.add("moveTowardLeeks([1])");
		values.add(0);

		// Test moveAwayFromCells
		codes.add("moveAwayFromCells([1])");
		values.add(0);

		// Test moveAwayFromLeeks
		codes.add("moveAwayFromLeeks([1])");
		values.add(0);

		// Test getCellsToUseWeapon
		codes.add("getCellsToUseWeapon(1)");
		values.add(null);

		// Test getCellToUseWeapon
		codes.add("getCellToUseWeapon(1)");
		values.add(-1);

		// Test moveAwayFromLine
		codes.add("moveAwayFromLine(-1,-1)");
		values.add(0);

		// Test getCellContent
		codes.add("getCellContent(-1)");
		values.add(-1);

		// Test getCellX
		codes.add("getCellX(-1)");
		values.add(null);

		// Test getCellY
		codes.add("getCellY(-1)");
		values.add(null);

		// Test getCellY
		codes.add("getCellY(getCell(1))");
		values.add(null);

		// Test getCellY
		codes.add("getCellContent(getCell(1))");
		values.add(null);

		// Test getEffects
		codes.add("count(getEffects(getCell(1)))");
		values.add(0);

		// Test getEffects
		codes.add("count(getWeaponTargets(WEAPON_PISTOL, null))");
		values.add(null);

//		assertTrue(testAI(leek1, codes, values));
	}

	@Test
	public void cellTest() throws Exception {
		Map map = mFight.getState().getMap();
		map.clear();
		// map.getCell(203).setPlayer(mLeek2);
		// map.getCell(306).setPlayer(mLeek1);

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getCell
		codes.add("getCell()");
		values.add(mLeek1.getCell().getId());

		// Test getCellX
		codes.add("getCellX(getCell())");
		values.add(mLeek1.getCell().getX() - map.getWidth() + 1);

		// Test getCellY
		codes.add("getCellY(getCell())");
		values.add(mLeek1.getCell().getY());

		// Test getCellFromXY
		codes.add("getCellFromXY(0,0)");
		values.add(map.getCell(0 + map.getWidth() - 1, 0).getId());

		// Test getCellFromXY
		codes.add("getCellX(getCellFromXY(14,0))");
		values.add(14);
		// Test getCellFromXY
		codes.add("getCellY(getCellFromXY(0,14))");
		values.add(14);

		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	@Test
	public void getPathTest() throws Exception {
		Map map = mFight.getState().getMap();
		map.clear();
		// map.getCell(10, 0).setPlayer(mLeek2);
		// map.getCell(306).setPlayer(mLeek1);

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getPath (clear map, distance of 2)
		codes.add("count(getPath(getCellFromXY(-1,0), getCellFromXY(1,0)))");
		values.add(2);
		// Test getPath + ignore (entity cell, using array syntax for v4)
		codes.add("count(getPath(getCellFromXY(-1,0), getCellFromXY(1,0), [getCell()]))");
		values.add(2);

		Assert.assertTrue(testAI(mLeek1, codes, values));
	}


	@Test
	public void MapFunctionsTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		Cell emptycell = null;
		Cell obstaclecell = null;
		for (int i = 1; i < 250; i++) {
			Cell c = mFight.getState().getMap().getCell(i);
			if (c.available(mFight.getState().getMap()) && emptycell == null)
				emptycell = c;
			if (!c.isWalkable() && obstaclecell == null)
				obstaclecell = c;
			if (emptycell != null && obstaclecell != null)
				break;
		}

		// Test getDistance (returns Double in v4)
		codes.add("getDistance(164,200)");
		values.add(2.0);
		codes.add("getDistance(17,595)");
		values.add(34.0);

		// Test getCellDistance
		codes.add("getCellDistance(164,200)");
		values.add(2);
		codes.add("getCellDistance(17,595)");
		values.add(34);

		// Test getEntityOnCell
		codes.add("getEntityOnCell(getCell())");
		values.add(mLeek1.getFId());
		codes.add("getEntityOnCell(getCell(" + mLeek2.getFId() + "))");
		values.add(mLeek2.getFId());
		codes.add("getEntityOnCell(" + emptycell.getId() + ")");
		values.add(-1);
		codes.add("getEntityOnCell(-1)");
		values.add(-1);

		// Test getCellContent
		codes.add("getCellContent(getCell())");
		values.add(LeekConstants.CELL_PLAYER.getIntValue());
		codes.add("getCellContent(getCell(" + mLeek2.getFId() + "))");
		values.add(LeekConstants.CELL_PLAYER.getIntValue());
		codes.add("getCellContent(" + emptycell.getId() + ")");
		values.add(LeekConstants.CELL_EMPTY.getIntValue());
		codes.add("getCellContent(" + obstaclecell.getId() + ")");
		values.add(LeekConstants.CELL_OBSTACLE.getIntValue());
		codes.add("getCellContent(-1)");
		values.add(-1);

		// Test isEmptyCell
		codes.add("isEmptyCell(getCell())");
		values.add(false);
		codes.add("isEmptyCell(getCell(" + mLeek2.getFId() + "))");
		values.add(false);
		codes.add("isEmptyCell(" + emptycell.getId() + ")");
		values.add(true);
		codes.add("isEmptyCell(" + obstaclecell.getId() + ")");
		values.add(false);
		codes.add("isEmptyCell(-1)");
		values.add(false);

		// Test isObstacle
		codes.add("isObstacle(getCell())");
		values.add(false);
		codes.add("isObstacle(getCell(" + mLeek2.getFId() + "))");
		values.add(false);
		codes.add("isObstacle(" + emptycell.getId() + ")");
		values.add(false);
		codes.add("isObstacle(" + obstaclecell.getId() + ")");
		values.add(true);
		codes.add("isObstacle(-1)");
		values.add(true);

		// Test isEntity
		codes.add("isEntity(getCell())");
		values.add(true);
		codes.add("isEntity(getCell(" + mLeek2.getFId() + "))");
		values.add(true);
		codes.add("isEntity(" + emptycell.getId() + ")");
		values.add(false);
		codes.add("isEntity(" + obstaclecell.getId() + ")");
		values.add(false);
		codes.add("isEntity(-1)");
		values.add(false);

		// Test getCellX
		codes.add("getCellX(getCell())");
		values.add(mLeek1.getCell().getX() - mFight.getState().getMap().getWidth() + 1);
		codes.add("getCellX(getCell(" + mLeek2.getFId() + "))");
		values.add(mLeek2.getCell().getX() - mFight.getState().getMap().getWidth() + 1);
		codes.add("getCellX(" + emptycell.getId() + ")");
		values.add(emptycell.getX() - mFight.getState().getMap().getWidth() + 1);
		codes.add("getCellX(" + obstaclecell.getId() + ")");
		values.add(obstaclecell.getX() - mFight.getState().getMap().getWidth() + 1);
		codes.add("getCellX(-1)");
		values.add(null);

		// Test getCellY
		codes.add("getCellY(getCell())");
		values.add(mLeek1.getCell().getY());
		codes.add("getCellY(getCell(" + mLeek2.getFId() + "))");
		values.add(mLeek2.getCell().getY());
		codes.add("getCellY(" + emptycell.getId() + ")");
		values.add(emptycell.getY());
		codes.add("getCellY(" + obstaclecell.getId() + ")");
		values.add(obstaclecell.getY());
		codes.add("getCellY(-1)");
		values.add(null);

		// Test getNearestEnemy
		codes.add("getNearestEnemy()");
		values.add(mLeek2.getFId());

		// Test getFarthestEnemy
		codes.add("getFarthestEnemy()");
		values.add(mLeek2.getFId());

		// Test getTurn
		codes.add("getTurn()");
		values.add(1);

		// Test getAliveEnemies
		codes.add("string(getAliveEnemies())");
		values.add("[1]");

		// Test getAliveEnemiesCount
		codes.add("getAliveEnemiesCount()");
		values.add(1);

		// Test getDeadEnemies
		codes.add("string(getDeadEnemies())");
		values.add("[]");

		// Test getDeadEnemiesCount
		codes.add("getDeadEnemiesCount()");
		values.add(0);

		// Test getEnemies
		codes.add("string(getEnemies())");
		values.add("[1]");

		// Test getAllies
		codes.add("string(getAllies())");
		values.add("[0]");

		// Test getEnemiesCount
		codes.add("getEnemiesCount()");
		values.add(1);

		// Test getNearestAlly
		codes.add("getNearestAlly()");
		values.add(-1);

		// Test getFarthestAlly
		codes.add("getFarthestAlly()");
		values.add(-1);

		// Test getFarthestAlly
		codes.add("getFarthestAlly()");
		values.add(-1);

		// Test getAliveAllies
		codes.add("string(getAliveAllies())");
		values.add("[0]");

		// Test getDeadAllies
		codes.add("string(getDeadAllies())");
		values.add("[]");

		// Test getDeadAlliesCount
		codes.add("getDeadAlliesCount()");
		values.add(0);

		// Test getAlliesCount
		codes.add("getAlliesCount()");
		values.add(1);

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	/** getDeadAlliesCount() compte exactement les entités de getDeadAllies(). */
	@Test
	public void getDeadAlliesCountTest() throws Exception {
		Fight fight = new Fight(generator);
		Leek leek1 = new Leek(1, "Test", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		Leek ally = new Leek(3, "Ally", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		Leek enemy = new Leek(2, "Bob", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(0, ally);
		fight.getState().addEntity(1, enemy);
		fight.initFight();
		fight.setStatisticsManager(new LocalTrophyManager());

		var code = "return [getDeadAlliesCount(), count(getDeadAllies()), getDeadEnemiesCount(), getAliveAlliesCount()];";
		Assert.assertEquals("[0, 0, 0, 2]", GeneratorCompilation.runSnippet(leek1, fight, code, new Options(true)));

		ally.removeLife(ally.getLife(), 0, enemy, DamageType.DIRECT, null, null);
		Assert.assertTrue(ally.isDead());
		Assert.assertEquals("[1, 1, 0, 1]", GeneratorCompilation.runSnippet(leek1, fight, code, new Options(true)));
		// Vu de l'ennemi, l'allié mort est un ennemi mort
		Assert.assertEquals("[0, 0, 1, 1]", GeneratorCompilation.runSnippet(enemy, fight, code, new Options(true)));
	}

	/**
	 * Une IA qui définit déjà sa propre getDeadAlliesCount() compile toujours, dans toutes les
	 * versions, et c'est la sienne qui est appelée.
	 */
	@Test
	public void userDefinedGetDeadAlliesCountTest() throws Exception {
		String code = "var a = getDeadAlliesCount();"
			+ " function getDeadAlliesCount() { return 42; }"
			+ " return [a, getDeadAlliesCount(), count(getDeadAllies())];";
		for (int version = 1; version <= LeekScript.LATEST_VERSION; version++) {
			Assert.assertEquals("version " + version, "[42, 42, 0]", GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(version, false, false, true, null, true)));
		}
		Assert.assertEquals("strict", "[42, 42, 0]", GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(LeekScript.LATEST_VERSION, true, false, true, null, true)));
	}

	/** getAIPath() : chemin absolu de l'IA, seulement quand son fichier est au même éleveur que celui de l'appelant. */
	@Test
	public void getAIPathTest() throws Exception {
		// Même éleveur d'entité (comme deux tourelles, à -1) : c'est le propriétaire des fichiers qui compte
		mLeek1.setFarmer(-1);
		mLeek2.setFarmer(-1);
		mLeek1.setAIFile(new AIFile("dossier/ia", "", 1, LeekScript.LATEST_VERSION, 7, false));
		mLeek2.setAIFile(new AIFile("secret/ia", "", 1, LeekScript.LATEST_VERSION, 8, false));
		String code = "return [getAIPath(), getAIPath(getEntity()), getAIPath(" + mLeek2.getFId() + "), getAIPath(12345)];";
		String expected = "[\"/dossier/ia\", \"/dossier/ia\", null, null]";
		for (int version = 1; version <= LeekScript.LATEST_VERSION; version++) {
			Assert.assertEquals("version " + version, expected, GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(version, false, false, true, null, true)));
		}
		Assert.assertEquals("strict", expected, GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(LeekScript.LATEST_VERSION, true, false, true, null, true)));
		// Une invocation exécute l'IA de son invocateur
		var bulb = new Bulb(mLeek1, 3, "Bulbe", 1, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		mFight.getState().addEntity(0, bulb);
		Assert.assertEquals("\"/dossier/ia\"", GeneratorCompilation.runSnippet(mLeek1, mFight, "return getAIPath(" + bulb.getFId() + ");", new Options(true)));
		// Un fichier du même éleveur, sur une entité d'un autre éleveur (bot du combat de test) : lisible
		mLeek2.setFarmer(0);
		mLeek2.setAIFile(new AIFile("secret/ia", "", 1, LeekScript.LATEST_VERSION, 7, false));
		Assert.assertEquals("\"/secret/ia\"", GeneratorCompilation.runSnippet(mLeek1, mFight, "return getAIPath(" + mLeek2.getFId() + ");", new Options(true)));
	}

	/**
	 * getComponents() rend les composants équipés en ids d'objet, les mêmes que les constantes
	 * COMPONENT_*, dans toutes les versions.
	 */
	@Test
	public void getComponentsTest() throws Exception {
		int nephelium = FightConstants.COMPONENT_NEPHELIUM.getIntValue();
		int apple = FightConstants.COMPONENT_APPLE.getIntValue();
		mLeek1.setComponentTemplates(List.of(nephelium, apple));
		String code = "return [getComponents(), getComponents(getEntity()), getComponents(" + mLeek2.getFId() + "),"
			+ " getComponents(12345), COMPONENT_NEPHELIUM, COMPONENT_APPLE];";
		String expected = "[[" + nephelium + ", " + apple + "], [" + nephelium + ", " + apple + "], [], null, " + nephelium + ", " + apple + "]";
		for (int version = 1; version <= LeekScript.LATEST_VERSION; version++) {
			Assert.assertEquals("version " + version, expected, GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(version, false, false, true, null, true)));
		}
		Assert.assertEquals("strict", expected, GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(LeekScript.LATEST_VERSION, true, false, true, null, true)));
		// Vu de l'ennemi
		Assert.assertEquals("[" + nephelium + ", " + apple + "]", GeneratorCompilation.runSnippet(mLeek2, mFight, "return getComponents(" + mLeek1.getFId() + ");", new Options(true)));
	}

	/**
	 * Les constantes COMPONENT_* valent l'id d'objet du composant de même nom (celui de getInventory).
	 * Les composants plus récents que data/components.json sont vérifiés sur leurs ids en base.
	 */
	@Test
	public void componentConstantsAreItemIdsTest() {
		var newer = java.util.Map.of("COMPONENT_WALNUT", 591, "COMPONENT_COCOA", 593, "COMPONENT_AMETHYST_RING", 595,
			"COMPONENT_GYROSCOPE", 597, "COMPONENT_LEVITATOR", 599);
		int known = 0, checkedNewer = 0;
		for (FightConstants c : FightConstants.values()) {
			if (!c.name().startsWith("COMPONENT_")) continue;
			var component = Components.getComponent(c.getIntValue());
			if (component == null) {
				Assert.assertEquals(c.name(), newer.get(c.name()), (Integer) c.getIntValue());
				checkedNewer++;
				continue;
			}
			Assert.assertEquals(c.name(), "COMPONENT_" + component.getName().toUpperCase(), c.name());
			known++;
		}
		Assert.assertEquals(Components.getTemplates().size(), known);
		Assert.assertEquals(newer.size(), checkedNewer);
	}

	/**
	 * Une IA qui définit déjà une fonction, une globale ou une variable du nom d'une nouvelle
	 * fonction ou constante compile toujours, dans toutes les versions, et garde la sienne.
	 */
	@Test
	public void userDefinedComponentAndAwakeningNamesTest() throws Exception {
		String code = "var a = getComponents(); var b = getPlantTriggers(1);"
			+ " function getComponents() { return 42; } function getPlantTriggers(x) { return x + 1; }"
			+ " global COMPONENT_CORE = 7; var COMPONENT_APPLE = 8;"
			+ " return [a, b, getComponents(), getPlantTriggers(2), COMPONENT_CORE, COMPONENT_APPLE];";
		for (int version = 1; version <= LeekScript.LATEST_VERSION; version++) {
			Assert.assertEquals("version " + version, "[42, 2, 42, 3, 7, 8]", GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(version, false, false, true, null, true)));
		}
		Assert.assertEquals("strict", "[42, 2, 42, 3, 7, 8]", GeneratorCompilation.runSnippet(mLeek1, mFight, code, new Options(LeekScript.LATEST_VERSION, true, false, true, null, true)));
	}

	@Test
	public void SummonFunctionsTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getType
		codes.add("getType()");
		values.add(FightConstants.ENTITY_LEEK.getIntValue());
		codes.add("getType(" + mLeek2.getFId() + ")");
		values.add(FightConstants.ENTITY_LEEK.getIntValue());
		codes.add("getType(-1)");
		values.add(null);

		// Test isSummon
		codes.add("isSummon()");
		values.add(false);
		codes.add("isSummon(" + mLeek2.getFId() + ")");
		values.add(false);
		codes.add("getType(-1)");
		values.add(null);

		// Test getBirthTurn
		codes.add("getBirthTurn()");
		values.add(1);
		codes.add("getBirthTurn(-1)");
		values.add(null);

		// Test getSummoner
		codes.add("getSummoner()");
		values.add(-1);
		codes.add("getSummoner(" + mLeek2.getFId() + ")");
		values.add(-1);
		codes.add("getSummoner(-1)");
		values.add(null);

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	/**
	 * Topic forum 450 : une cible null (getCell d'un mort, entité absente) ne déplace pas. Elle valait
	 * la case 0 (le poireau partait vers le coin de la carte) ou l'entité 0.
	 */
	@Test
	public void moveWithNullTargetTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();
		codes.add("moveTowardCell(null)");
		values.add(0);
		codes.add("moveTowardCell(null, 3)");
		values.add(0);
		codes.add("moveAwayFromCell(null)");
		values.add(0);
		codes.add("moveToward(null)");
		values.add(0);
		codes.add("moveAwayFrom(null, 2)");
		values.add(0);
		codes.add("moveToward(null, getMP() / 2)"); // pm réel
		values.add(0);
		codes.add("[moveTowardCell][0](null)"); // fonction en valeur
		values.add(0);
		// Joué par mLeek2 : l'entité 0 est l'autre poireau, pas lui-même (aller vers soi ne bouge pas)
		Assert.assertTrue(testAI(mLeek2, codes, values));
	}

	/** Une chaîne ou un booléen se convertit en entier comme avant : le poireau se déplace. */
	@Test
	public void moveStringTargetTest() throws Exception {
		Assert.assertTrue(testAI(mLeek1, List.of("moveToward('" + mLeek2.getFId() + "') > 0"), List.<Object>of(true)));
	}

	@Test
	public void moveBooleanTargetTest() throws Exception {
		Assert.assertEquals(1, mLeek2.getFId()); // true vaut l'entité 1
		Assert.assertTrue(testAI(mLeek1, List.of("moveToward(true) > 0"), List.<Object>of(true)));
	}

	/** Un réel déplace toujours (tronqué), et getCell l'accepte aussi. */
	@Test
	public void moveRealTargetTest() throws Exception {
		long id = mLeek2.getFId();
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();
		codes.add("getCell(" + id + ".0) == getCell(" + id + ")");
		values.add(true);
		codes.add("moveTowardCell(getCell(" + id + ") / 1) > 0");
		values.add(true);
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	/** Une cible null laisse un warning dans les logs : argument de mauvais type. */
	@Test
	public void moveWithNullTargetWarnsTest() throws Exception {
		var ai = (EntityAI) LeekScript.compileSnippet("moveTowardCell(null) moveAwayFrom(null, 2)", "com.leekwars.generator.fight.entity.EntityAI", new Options(true));
		var logs = new FarmerLog(mFight, 0);
		ai.setEntity(mLeek2);
		ai.setLogs(new LeekLog(logs, mLeek2));
		ai.setFight(mFight);
		ai.runIA();
		int warnings = 0;
		for (var actionLogs : logs.toJSON()) {
			for (var log : actionLogs) {
				if (log.get(1).asInt() == AILog.SWARNING && log.get(3).asInt() == Error.WRONG_ARGUMENT_TYPE.ordinal()) warnings++;
			}
		}
		Assert.assertEquals(2, warnings);
	}

	/**
	 * 5pilow/leek-wars#3095 : la case d'arrivée occupée par une entité est retirée du chemin,
	 * sauf si elle figure dans les cases à ignorer.
	 */
	@Test
	public void getPathIgnoredEndCellTest() throws Exception {
		mFight.getState().getMap().clear();
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		String next = "getCellFromXY(getCellX(getCell()) + 1, getCellY(getCell()))";
		codes.add("getPathLength(getCell(), " + next + ")");
		values.add(1);
		codes.add("getPathLength(" + next + ", getCell())");
		values.add(0);
		codes.add("getPathLength(" + next + ", getCell(), [getCell()])");
		values.add(1);
		codes.add("count(getPath(" + next + ", getCell(), [getCell()]))");
		values.add(1);

		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	@Test
	public void getCellToUseChipOnCellInvalidTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Cellule hors carte : -1, pas la valeur passée
		codes.add("getCellToUseChipOnCell(CHIP_SPARK, 99999)");
		values.add(-1);
		// Puce inconnue : -1, pas la cellule visée
		codes.add("getCellToUseChipOnCell(99999, getCell(" + mLeek2.getFId() + "))");
		values.add(-1);
		// Appel valide : une cellule est trouvée
		codes.add("getCellToUseChipOnCell(CHIP_SPARK, getCell(" + mLeek2.getFId() + ")) != -1");
		values.add(true);

		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	@Test
	public void getObstaclesTest() throws Exception {
		// Test AI

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test nombre
		codes.add("typeOf(getObstacles())");
		values.add(LeekConstants.TYPE_ARRAY.getIntValue());

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	@Test
	public void typeOfTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		codes.add("typeOf(getEntity)");
		values.add(LeekConstants.TYPE_FUNCTION.getIntValue());

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	@Test
	public void getNearestEnemyTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getName
		codes.add("getNearestEnemy()");
		values.add(1);

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	private boolean testAI(Leek l, List<String> mCodes, List<Object> mValues) throws Exception {
		String leekscript = "return [";
		Object[] values = new Object[mValues.size()];

		for (int i = 0; i < mValues.size(); i++) {
			if (i != 0)
				leekscript += ",";
			leekscript += mCodes.get(i);
			// Convert Integer to Long to match LeekScript runtime types
			values[i] = mValues.get(i) instanceof Integer ? ((Integer) mValues.get(i)).longValue() : mValues.get(i);
		}

		leekscript += "];";
		try {
			return GeneratorCompilation.testScriptGenerator(l, mFight, leekscript, new LegacyArrayLeekValue(ai, values));
		} catch (LSException e) {
			System.err.println("Erreur :\n" + leekscript);
			System.err.println("Valeur attendue :\n" + ai.export(e.getThe()));
			System.err.println("Valeur renvoyée :\n" + ai.export(e.getRun()));
			return false;
		}
	}
}
