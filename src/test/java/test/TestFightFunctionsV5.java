package test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.AIFile;
import leekscript.compiler.Options;

/**
 * Fonctions de combat dont le comportement change en LeekScript 5 (déclarées `since(5, …)`) :
 * chaque test fige la v4 et vérifie la v5.
 */
public class TestFightFunctionsV5 {

	private static final AtomicLong COUNTER = new AtomicLong(20_000_000);
	// Le propriétaire 1 a accès à la v5 tant qu'elle n'est pas publique (cf Generator)
	private static final int OWNER = 1;

	private Fight fight;
	private Leek leek1;
	private Leek leek2;
	private AIFile lastFile;

	@Before
	public void setUp() throws Exception {
		var generator = new Generator();
		fight = new Fight(generator);
		leek1 = new Leek(1, "Test", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "Bob", 0, 10, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, 0, 0, 0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
		fight.initFight();
		fight.setStatisticsManager(new LocalTrophyManager());
	}

	private EntityAI compile(int version, boolean strict, String code) throws Exception {
		long id = COUNTER.incrementAndGet();
		var file = new AIFile("<v5_" + id + ">", code, System.currentTimeMillis(), version, OWNER, strict);
		file.setJavaClass("AI_V5_" + id);
		file.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
		file.setId((int) id);
		lastFile = file;
		var ai = (EntityAI) file.compile(new Options(version, strict, false, true, null, true));
		Assert.assertEquals(version, ai.getVersion());
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(new FarmerLog(fight, 0), leek1));
		ai.setFight(fight);
		return ai;
	}

	/** Résultat exporté de `code` (une expression) joué par leek1, en mode non strict. */
	private String eval(int version, String expression) throws Exception {
		var ai = compile(version, false, "return " + expression + ";");
		return ai.export(ai.runIA());
	}

	/** Avertissements et erreurs de l'analyse stricte. */
	private List<String> strictErrors(int version, String code) throws Exception {
		compile(version, true, code);
		var errors = new ArrayList<String>();
		for (var e : lastFile.getErrors()) errors.add(e.level + " " + e.error);
		return errors;
	}

	private String javaOf(int version, String code) throws Exception {
		compile(version, false, code);
		return lastFile.getCompiledCode().getJavaCode();
	}

	// ---- -1 → null pour « aucun » ----

	@Test
	public void noneIsMinusOneInV4() throws Exception {
		// 1 contre 1, sans arme ni invocation : aucun allié, aucune arme, aucun invocateur
		Assert.assertEquals("[-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]", eval(4,
			"[getNearestAlly(), getFarthestAlly(), getEntityOnCell(getCellFromXY(0, 0)), getSummoner(), getSummoner(getEntity()),"
			+ " getMobType(), getChestType(), getBulbType(), getPlantType(), getPlantTrigger(),"
			+ " getWeaponMinRange(), getWeaponMaxRange(), getWeaponCost(), getWeaponMaxUses()]"));
		Assert.assertEquals("[-1, -1, -1, -1]", eval(4,
			"[getCellToUseWeapon(" + leek2.getFId() + "), getCellToUseWeaponOnCell(getCell(" + leek2.getFId() + ")),"
			+ " getCellToUseChip(CHIP_SPARK, 99), getCellToUseChipOnCell(CHIP_SPARK, 99999)]"));
		// Une entité existe toujours : son id
		Assert.assertEquals("[" + leek2.getFId() + ", " + leek2.getFId() + ", " + leek1.getFId() + "]",
			eval(4, "[getNearestEnemy(), getFarthestEnemy(), getEntityOnCell(getCell())]"));
	}

	@Test
	public void noneIsNullInV5() throws Exception {
		Assert.assertEquals("[null, null, null, null, null, null, null, null, null, null, null, null, null, null]", eval(5,
			"[getNearestAlly(), getFarthestAlly(), getEntityOnCell(getCellFromXY(0, 0)), getSummoner(), getSummoner(getEntity()),"
			+ " getMobType(), getChestType(), getBulbType(), getPlantType(), getPlantTrigger(),"
			+ " getWeaponMinRange(), getWeaponMaxRange(), getWeaponCost(), getWeaponMaxUses()]"));
		Assert.assertEquals("[null, null, null, null]", eval(5,
			"[getCellToUseWeapon(" + leek2.getFId() + "), getCellToUseWeaponOnCell(getCell(" + leek2.getFId() + ")),"
			+ " getCellToUseChip(CHIP_SPARK, 99), getCellToUseChipOnCell(CHIP_SPARK, 99999)]"));
		Assert.assertEquals("[" + leek2.getFId() + ", " + leek2.getFId() + ", " + leek1.getFId() + "]",
			eval(5, "[getNearestEnemy(), getFarthestEnemy(), getEntityOnCell(getCell())]"));
		// Une entité inconnue reste null, comme avant
		Assert.assertEquals("[null, null]", eval(5, "[getSummoner(12345), getMobType(12345)]"));
		// Même résultat par une fonction en valeur (wrapper générique)
		Assert.assertEquals("[null, null]", eval(5, "[[getNearestAlly][0](), [getWeaponCost][0]()]"));
	}

	@Test
	public void noneTypeIsNullableInV5Only() throws Exception {
		var code = "integer a = getNearestAlly(); integer c = getCellToUseChip(CHIP_SPARK, getNearestEnemy()); return [a, c];";
		Assert.assertEquals(List.of(), strictErrors(4, code));
		var v5 = strictErrors(5, code);
		Assert.assertTrue(v5.toString(), v5.contains("WARNING DANGEROUS_CONVERSION_VARIABLE"));
		Assert.assertEquals(List.of(), strictErrors(5, "integer? a = getNearestAlly(); integer? w = getWeaponCost(); return [a, w];"));
	}

	@Test
	public void v4CallsTheOriginalMethods() throws Exception {
		var code = "return [getNearestAlly(), getEntityOnCell(1), getWeaponCost(), getSummoner()];";
		var v4 = javaOf(4, code);
		Assert.assertTrue(v4, v4.contains("FightClass.getNearestAlly(") && v4.contains("WeaponClass.getWeaponCost("));
		Assert.assertFalse(v4, v4.contains("_v5"));
		var v5 = javaOf(5, code);
		Assert.assertTrue(v5, v5.contains("FightClass.getNearestAlly_v5(") && v5.contains("FieldClass.getEntityOnCell_v5(")
			&& v5.contains("WeaponClass.getWeaponCost_v5(") && v5.contains("EntityClass.getSummoner_v5("));
	}
}
