package test;

import java.util.ArrayList;
import java.util.List;

import leekscript.LSException;
import leekscript.runner.AI;
import leekscript.runner.values.LegacyArrayLeekValue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.leek.Leek;

public class TestLeekFunctions {

	private Generator generator;
	private Fight mFight;
	private Leek mLeek1;
	private Leek mLeek2;
	private AI ai;

	@Before
	public void setUp() throws Exception {

		generator = new Generator();
		mFight = new Fight(generator);

		mLeek1 = new Leek(1, "Test", 0, 10, 500, 6, 7, 150, 151, 10, 56, 9, 0, 0, 0, false, 0, 0, "Noname", 0, "", "", "", 0);
		mLeek2 = new Leek(2, "Bob", 0, 10, 510, 4, 6, 152, 154, 11, 46, 8, 0, 0, 0, false, 0, 0, "Noname", 0, "", "", "", 0);

		mFight.addEntity(0, mLeek1);
		mFight.addEntity(1, mLeek2);

		mFight.initFight();
		ai = new DefaultUserAI();
	}

	@Test
	public void getNameTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getName
		codes.add("getName()");
		values.add("Test");

		// Test getName(enemy)
		codes.add("getName(getNearestEnemy())");
		values.add("Bob");

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	@Test
	public void getCellTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getName
		codes.add("getCell()");
		values.add(mLeek1.getCell().getId());

		// Test getName(enemy)
		codes.add("getCell(getNearestEnemy())");
		values.add(mLeek2.getCell().getId());

		// Test AI
		Assert.assertTrue(testAI(mLeek1, codes, values));
	}

	@Test
	public void LeekFunctionsTest() throws Exception {
		ArrayList<String> codes = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();

		// Test getForce
		codes.add("getForce()");
		values.add(mLeek1.getStat(Entity.CHARAC_STRENGTH));
		codes.add("getForce(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getStat(Entity.CHARAC_STRENGTH));
		codes.add("getForce(-1)");
		values.add(null);

		// Test getAgility
		codes.add("getAgility()");
		values.add(mLeek1.getStat(Entity.CHARAC_AGILITY));
		codes.add("getAgility(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getStat(Entity.CHARAC_AGILITY));
		codes.add("getAgility(-1)");
		values.add(null);

		// Test getLife
		codes.add("getLife()");
		values.add(mLeek1.getLife());
		codes.add("getLife(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getLife());
		codes.add("getLife(-1)");
		values.add(null);

		// Test getCell
		codes.add("getCell()");
		values.add(mLeek1.getCell().getId());
		codes.add("getCell(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getCell().getId());
		codes.add("getCell(-1)");
		values.add(null);

		// Test getWeapon
		codes.add("getWeapon()");
		values.add(null);
		codes.add("getWeapon(" + mLeek2.getFId() + ")");
		values.add(null);
		codes.add("getWeapon(-1)");
		values.add(null);

		// Test getMP
		codes.add("getMP()");
		values.add(mLeek1.getMP());
		codes.add("getMP(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getMP());
		codes.add("getMP(-1)");
		values.add(null);

		// Test getTP
		codes.add("getTP()");
		values.add(mLeek1.getTP());
		codes.add("getTP(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getTP());
		codes.add("getTP(-1)");
		values.add(null);

		// Test getTotalLife
		codes.add("getTotalLife()");
		values.add(mLeek1.getStat(Entity.CHARAC_LIFE));
		codes.add("getTotalLife(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getStat(Entity.CHARAC_LIFE));
		codes.add("getTotalLife(-1)");
		values.add(null);

		// Test isEnemy
		codes.add("isEnemy(" + mLeek1.getFId() + ")");
		values.add(false);
		codes.add("isEnemy(" + mLeek2.getFId() + ")");
		values.add(true);
		codes.add("isEnemy(-1)");
		values.add(false);

		// Test isAlly
		codes.add("isAlly(" + mLeek1.getFId() + ")");
		values.add(true);
		codes.add("isAlly(" + mLeek2.getFId() + ")");
		values.add(false);
		codes.add("isAlly(-1)");
		values.add(false);

		// Test isDead
		codes.add("isDead(" + mLeek1.getFId() + ")");
		values.add(false);
		codes.add("isDead(" + mLeek2.getFId() + ")");
		values.add(false);
		codes.add("isDead(-1)");
		values.add(false);

		// Test isAlive
		codes.add("isAlive(" + mLeek1.getFId() + ")");
		values.add(true);
		codes.add("isAlive(" + mLeek2.getFId() + ")");
		values.add(true);
		codes.add("isAlive(-1)");
		values.add(false);

		// Test getLeek
		codes.add("getLeek()");
		values.add(mLeek1.getFId());

		// Test getChips
		codes.add("string(getChips())");
		values.add("[]");
		codes.add("string(getChips(" + mLeek2.getFId() + "))");
		values.add("[]");
		codes.add("getChips(-1)");
		values.add(null);

		// Test getAbsoluteShield
		codes.add("getAbsoluteShield()");
		values.add(0);
		codes.add("getAbsoluteShield(" + mLeek2.getFId() + ")");
		values.add(0);
		codes.add("getAbsoluteShield(-1)");
		values.add(null);

		// Test getAbsoluteShield
		codes.add("getRelativeShield()");
		values.add(0);
		codes.add("getRelativeShield(" + mLeek2.getFId() + ")");
		values.add(0);
		codes.add("getRelativeShield(-1)");
		values.add(null);

		// Test getLevel
		codes.add("getLevel()");
		values.add(mLeek1.getLevel());
		codes.add("getLevel(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getLevel());
		codes.add("getLevel(-1)");
		values.add(null);

		// Test getFrequency
		codes.add("getFrequency()");
		values.add(mLeek1.getStat(Entity.CHARAC_FREQUENCY));
		codes.add("getFrequency(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getStat(Entity.CHARAC_FREQUENCY));
		codes.add("getFrequency(-1)");
		values.add(null);

		// Test getLeekID
		codes.add("getLeekID()");
		values.add(mLeek1.getId());
		codes.add("getLeekID(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getId());
		codes.add("getLeekID(-1)");
		values.add(null);

		// Test getTeamName
		codes.add("getTeamName()");
		values.add(mLeek1.getTeamName());
		codes.add("getTeamName(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getTeamName());
		codes.add("getTeamName(-1)");
		values.add(null);

		// Test getFarmerName
		codes.add("getFarmerName()");
		values.add(mLeek1.getFarmerName());
		codes.add("getFarmerName(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getFarmerName());
		codes.add("getFarmerName(-1)");
		values.add(null);

		// Test getTeamID
		codes.add("getTeamID()");
		values.add(mLeek1.getTeamId());
		codes.add("getTeamID(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getTeamId());
		codes.add("getTeamID(-1)");
		values.add(null);

		// Test getFarmerID
		codes.add("getFarmerID()");
		values.add(mLeek1.getFarmer());
		codes.add("getFarmerID(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getFarmer());
		codes.add("getFarmerID(-1)");
		values.add(null);

		// Test getAIName
		codes.add("getAIName()");
		values.add(mLeek1.getAIName());
		codes.add("getAIName(" + mLeek2.getFId() + ")");
		values.add(mLeek2.getAIName());
		codes.add("getAIName(-1)");
		values.add(null);

		// Test getAIName
		// codes.add("getAIID()");
		// values.add(mLeek1.getAI().getId());
		// codes.add("getAIID(" + mLeek2.getFId() + ")");
		// values.add(mLeek2.getAI().getId());
		codes.add("getAIID(-1)");
		values.add(null);

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
			int i = e.getIndex();
			System.err.println("Erreur :\n" + mCodes.get(i));
			System.err.println("Valeur attendue :\n" + ai.string(e.getThe()));
			System.err.println("Valeur renvoyée :\n" + ai.string(e.getRun()));
			return false;
		}
	}
}
