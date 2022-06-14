package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.DamageType;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;

import leekscript.LSException;
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
		mLeek1 = new Leek(1, "Test");
		mLeek2 = new Leek(2, "Bob");

		mFight.addEntity(0, mLeek1);
		mFight.addEntity(1, mLeek2);

		mFight.initFight();
		ai = new DefaultUserAI();
	}

	// Test de retour sur des structures du leekscript

	@Test
	public void nullPointerTest() throws Exception {

		Fight fight = new Fight(generator);
		Leek leek1 = new Leek(1, "Test");
		Leek leek2 = new Leek(2, "Bob");
		fight.addEntity(0, leek1);
		fight.addEntity(1, leek2);

		fight.initFight();

		Map map = fight.getMap();
		map.clear();
		map.getCell(203).setPlayer(leek2);
		map.getCell(306).setPlayer(leek1);

		leek2.removeLife(leek2.getLife(), 0, leek1, DamageType.DIRECT, null);

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
		Map map = mFight.getMap();
		map.clear();
		map.getCell(203).setPlayer(mLeek2);
		map.getCell(306).setPlayer(mLeek1);

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getCell
		codes.add("getCell()");
		values.add(306);

		// Test getCellX
		codes.add("getCellX(getCell())");
		values.add(mLeek1.getCell().getX() - map.getWidth() + 1);

		// Test getCellY
		codes.add("getCellY(getCell())");
		values.add(mLeek1.getCell().getY());

		// Test getCellFromXY
		codes.add("getCellFromXY(0,0)");
		values.add(306);

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
		Map map = mFight.getMap();
		map.clear();
		map.getCell(10, 0).setPlayer(mLeek2);
		map.getCell(306).setPlayer(mLeek1);

		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getPath
		codes.add("count(getPath(getCellFromXY(-1,0), getCellFromXY(1,0)))");
		values.add(4);
		// Test getPath + ignore
		codes.add("count(getPath(getCellFromXY(-1,0), getCellFromXY(1,0), getLeek()))");
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
			Cell c = mFight.getMap().getCell(i);
			if (c.available() && emptycell == null)
				emptycell = c;
			if (!c.isWalkable() && obstaclecell == null)
				obstaclecell = c;
			if (emptycell != null && obstaclecell != null)
				break;
		}

		// Test getDistance
		codes.add("getDistance(164,200)");
		values.add(2);
		codes.add("getDistance(17,595)");
		values.add(34);

		// Test getCellDistance
		codes.add("getCellDistance(164,200)");
		values.add(2);
		codes.add("getCellDistance(17,595)");
		values.add(34);

		// Test getLeekOnCell
		codes.add("getLeekOnCell(getCell())");
		values.add(mLeek1.getFId());
		codes.add("getLeekOnCell(getCell(" + mLeek2.getFId() + "))");
		values.add(mLeek2.getFId());
		codes.add("getLeekOnCell(" + emptycell.getId() + ")");
		values.add(-1);
		codes.add("getLeekOnCell(-1)");
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

		// Test isLeek
		codes.add("isLeek(getCell())");
		values.add(true);
		codes.add("isLeek(getCell(" + mLeek2.getFId() + "))");
		values.add(true);
		codes.add("isLeek(" + emptycell.getId() + ")");
		values.add(false);
		codes.add("isLeek(" + obstaclecell.getId() + ")");
		values.add(false);
		codes.add("isLeek(-1)");
		values.add(false);

		// Test getCellX
		codes.add("getCellX(getCell())");
		values.add(mLeek1.getCell().getX() - mFight.getMap().getWidth() + 1);
		codes.add("getCellX(getCell(" + mLeek2.getFId() + "))");
		values.add(mLeek2.getCell().getX() - mFight.getMap().getWidth() + 1);
		codes.add("getCellX(" + emptycell.getId() + ")");
		values.add(emptycell.getX() - mFight.getMap().getWidth() + 1);
		codes.add("getCellX(" + obstaclecell.getId() + ")");
		values.add(obstaclecell.getX() - mFight.getMap().getWidth() + 1);
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

		// Test getFarestEnemy
		codes.add("getFarestEnemy()");
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

		// Test getFarestAlly
		codes.add("getFarestAlly()");
		values.add(-1);

		// Test getFarestAlly
		codes.add("getFarestAlly()");
		values.add(-1);

		// Test getAliveAllies
		codes.add("string(getAliveAllies())");
		values.add("[0]");

		// Test getDeadAllies
		codes.add("string(getDeadAllies())");
		values.add("[]");

		// Test getAlliesCount
		codes.add("getAlliesCount()");
		values.add(1);

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
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

		codes.add("typeOf(getLeek)");
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
			values[i] = mValues.get(i);
		}

		leekscript += "];";
		try {
			return GeneratorCompilation.testScriptGenerator(l, mFight, leekscript, new LegacyArrayLeekValue(ai, values));
		} catch (LSException e) {
			System.err.println("Erreur :\n" + leekscript);
			System.err.println("Valeur attendue :\n" + ai.string(e.getThe()));
			System.err.println("Valeur renvoyée :\n" + ai.string(e.getRun()));
			return false;
		}
	}
}
