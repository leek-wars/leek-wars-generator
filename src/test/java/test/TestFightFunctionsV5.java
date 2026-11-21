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
import com.leekwars.generator.weapons.Weapons;

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
		return compile(version, strict, true, code);
	}

	private EntityAI compile(int version, boolean strict, boolean operations, String code) throws Exception {
		long id = COUNTER.incrementAndGet();
		var file = new AIFile("<v5_" + id + ">", code, System.currentTimeMillis(), version, OWNER, strict);
		file.setJavaClass("AI_V5_" + id);
		file.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
		file.setId((int) id);
		lastFile = file;
		var ai = (EntityAI) file.compile(new Options(version, strict, false, operations, null, true));
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

	// ---- getWeaponName(null) et sœurs : seul l'appel sans arme désigne l'arme tenue ----

	/** leek1 tient le magnum et a aussi le pistolet. */
	private static final String HOLD_MAGNUM = "setWeapon(WEAPON_MAGNUM); var n = null; var e = getNearestEnemy(); ";

	private String evalHolding(int version, String expression) throws Exception {
		leek1.addWeapon(Weapons.getWeapon("pistol"));
		leek1.addWeapon(Weapons.getWeapon("magnum"));
		var ai = compile(version, false, HOLD_MAGNUM + "return " + expression + ";");
		return ai.export(ai.runIA());
	}

	private static final String WEAPON_IDS = "[getWeaponName(-1), getWeaponName(n), getWeaponName(),"
		+ " getWeaponCost(-1), getWeaponMinRange(-1), getWeaponMaxRange(-1), getWeaponMaxUses(-1), getWeaponLaunchType(-1),"
		+ " isInlineWeapon(-1), weaponNeedLos(-1), getWeaponEffects(-1) != null, getWeaponPassiveEffects(-1) != null,"
		+ " getWeaponName(WEAPON_PISTOL), getWeaponCost()]";

	@Test
	public void minusOneIsTheHeldWeaponInV4() throws Exception {
		// null vaut 0 une fois converti en entier : une arme inconnue, déjà en v4
		Assert.assertEquals("[\"magnum\", \"\", \"magnum\", 5, 1, 8, 2, 7, false, true, true, true, \"pistol\", 5]", evalHolding(4, WEAPON_IDS));
	}

	@Test
	public void minusOneIsAnUnknownWeaponInV5() throws Exception {
		Assert.assertEquals("[\"\", \"\", \"magnum\", null, null, null, null, null, false, false, false, false, \"pistol\", 5]", evalHolding(5, WEAPON_IDS));
	}

	private final String nullTargets() {
		return "[getWeaponTargets(getCell(e), n), getWeaponTargets(WEAPON_PISTOL, n), getWeaponEffectiveArea(getCell(e), n),"
			+ " getWeaponEffectiveArea(getCell(e), n, getCell()), getCellToUseWeapon(e, n) != null, getCellToUseWeapon(e, n, []) != null,"
			+ " getCellToUseWeaponOnCell(getCell(e), n) != null, getCellsToUseWeapon(e, n) != null, getCellsToUseWeaponOnCell(getCell(e), n, []) != null,"
			+ " getWeaponTargets(getCell(e)), getWeaponTargets(WEAPON_MAGNUM, getCell(e))]";
	}

	@Test
	public void nullTargetUsesTheHeldWeaponInV4() throws Exception {
		// 1 contre 1 sur une carte vide, l'ennemi à portée du magnum
		fight.getState().getMap().clear();
		moveNextTo();
		var area = "[" + leek2.getCell().getId() + "]";
		var target = "[" + leek2.getFId() + "]";
		// getCellsToUseWeaponOnCell déclare sa cible integer : null y vaut 0, et la cellule passe pour l'arme (inconnue)
		Assert.assertEquals("[" + target + ", [], " + area + ", " + area + ", true, true, true, true, false, " + target + ", " + target + "]", evalHolding(4, nullTargets()));
	}

	@Test
	public void nullTargetIsAnUnknownTargetInV5() throws Exception {
		fight.getState().getMap().clear();
		moveNextTo();
		var target = "[" + leek2.getFId() + "]";
		Assert.assertEquals("[null, null, null, null, false, false, false, false, false, " + target + ", " + target + "]", evalHolding(5, nullTargets()));
		Assert.assertEquals("[false, false, true, true]", evalHolding(5,
			"[canUseWeapon(e, n), canUseWeaponOnCell(getCell(e), n), canUseWeapon(e), canUseWeapon(WEAPON_MAGNUM, e)]"));
	}

	/** leek2 à trois cases de leek1, sur la même ligne. */
	private void moveNextTo() {
		var map = fight.getState().getMap();
		var from = leek1.getCell();
		var to = map.getCell(from.getX() + 3, from.getY());
		if (to == null || !to.available(map)) to = map.getCell(from.getX() - 3, from.getY());
		map.moveEntity(leek2, to);
	}

	// ---- getCellToUse* rend une case de getCellsToUse* ----

	/** leek1 emmuré par quatre obstacles, leek2 loin : aucune case de lancement n'est atteignable. */
	private void wallIn() {
		var map = fight.getState().getMap();
		map.clear();
		var from = leek1.getCell();
		for (var c : map.getCellsAround(from)) if (c != null) c.setWalkable(false);
		var far = map.getCell(from.getX() + 12, from.getY());
		if (far == null) far = map.getCell(from.getX() - 12, from.getY());
		map.moveEntity(leek2, far);
	}

	private static final String CELL_FAMILY = "[getCellToUseChip(CHIP_SPARK, e), getCellsToUseChip(CHIP_SPARK, e),"
		+ " getCellToUseChipOnCell(CHIP_SPARK, getCell(e)), getCellsToUseChipOnCell(CHIP_SPARK, getCell(e)),"
		+ " getCellToUseWeapon(e), getCellsToUseWeapon(e), getCellToUseWeaponOnCell(getCell(e)), getCellsToUseWeaponOnCell(getCell(e)),"
		+ " getCellToUseWeapon(WEAPON_MAGNUM, e, []), getCellsToUseWeapon(WEAPON_MAGNUM, e, [])]";

	private String evalCells(int version, String expression) throws Exception {
		leek1.addWeapon(Weapons.getWeapon("magnum"));
		leek1.addChip(com.leekwars.generator.chips.Chips.getChip("spark"));
		var ai = compile(version, false, HOLD_MAGNUM + "return " + expression + ";");
		return ai.export(ai.runIA());
	}

	@Test
	public void walledInSingularAndPluralDisagreeInV4() throws Exception {
		wallIn();
		var result = evalCells(4, "[getCellToUseChip(CHIP_SPARK, e), count(getCellsToUseChip(CHIP_SPARK, e)) > 0,"
			+ " getCellToUseChipOnCell(CHIP_SPARK, getCell(e)), count(getCellsToUseChipOnCell(CHIP_SPARK, getCell(e))) > 0,"
			+ " getCellToUseWeapon(e), count(getCellsToUseWeapon(e)) > 0, getCellToUseWeaponOnCell(getCell(e)), count(getCellsToUseWeaponOnCell(getCell(e))) > 0]");
		Assert.assertEquals("[-1, true, -1, true, -1, true, -1, true]", result);
	}

	@Test
	public void walledInPluralIsEmptyInV5() throws Exception {
		wallIn();
		Assert.assertEquals("[null, [], null, [], null, [], null, [], null, []]", evalCells(5, CELL_FAMILY));
	}

	@Test
	public void reachableCellsAreUnchangedInV5() throws Exception {
		// Carte vide : toutes les cases de lancement sont atteignables, rien ne change
		fight.getState().getMap().clear();
		Assert.assertEquals(evalCells(4, CELL_FAMILY), evalCells(5, CELL_FAMILY));
	}

	/**
	 * Cases du pluriel que le singulier contredit, sur toutes les cases de la carte du combat
	 * prises comme cible, pour la puce et pour l'arme : singulier hors du pluriel, ou « aucune »
	 * alors que le pluriel n'est pas vide (ou l'inverse). Rend aussi si les deux cas (pluriel vide,
	 * pluriel non vide) ont été vus.
	 */
	private String singularOutsidePlural(int version) throws Exception {
		leek1.addWeapon(Weapons.getWeapon("magnum"));
		leek1.addChip(com.leekwars.generator.chips.Chips.getChip("spark"));
		var map = fight.getState().getMap();
		// Des poches de 3 x 3 cases emmurées (ni chemin ni vue au travers) un peu partout : des
		// cibles dont aucune case de lancement n'est atteignable
		for (int id = 20; id < map.getNbCell(); id += 41) {
			var center = map.getCell(id);
			if (center == null || !center.available(map)) continue;
			if (com.leekwars.generator.maps.Pathfinding.getCaseDistance(center, leek1.getCell()) < 6) continue;
			if (com.leekwars.generator.maps.Pathfinding.getCaseDistance(center, leek2.getCell()) < 6) continue;
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -2; dy <= 2; dy++) {
					if (Math.max(Math.abs(dx), Math.abs(dy)) != 2) continue;
					var c = map.getCell(center.getX() + dx, center.getY() + dy);
					if (c != null && c.available(map)) c.setWalkable(false);
				}
			}
		}
		var v5 = version >= 5;
		var none = v5 ? "null" : "-1";
		var count = v5 ? "p.count()" : "count(p)";
		var push = v5 ? "bad.push(c)" : "push(bad, c)";
		var in = v5 ? "p.inArray(s)" : "inArray(p, s)";
		var code = HOLD_MAGNUM + "var bad = []; var empty = 0; var some = 0;\n"
			+ "for (var c = 0; c < " + map.getNbCell() + "; c++) {\n"
			+ "  for (var k = 0; k < 2; k++) {\n"
			+ "    var s = k == 0 ? getCellToUseChipOnCell(CHIP_SPARK, c) : getCellToUseWeaponOnCell(c);\n"
			+ "    var p = k == 0 ? getCellsToUseChipOnCell(CHIP_SPARK, c) : getCellsToUseWeaponOnCell(c);\n"
			+ "    if (p == null || " + count + " == 0) { empty++; if (s != " + none + ") " + push + "; }\n"
			+ "    else { some++; if (s == " + none + " || !" + in + ") " + push + "; }\n"
			+ "  }\n"
			+ "}\n"
			+ "return [bad, empty > 0, some > 0];";
		var ai = compile(version, false, false, code);
		return ai.export(ai.runIA());
	}

	@Test
	public void singularContradictsPluralInV4() throws Exception {
		var v4 = singularOutsidePlural(4);
		Assert.assertFalse(v4, v4.startsWith("[[]"));
	}

	@Test
	public void singularIsAlwaysInPluralInV5() throws Exception {
		Assert.assertEquals("[[], true, true]", singularOutsidePlural(5));
	}

	@Test
	public void v4CallsTheOriginalCellMethods() throws Exception {
		var code = "return [getCellsToUseChip(CHIP_SPARK, 1), getCellToUseChipOnCell(CHIP_SPARK, 1), getCellsToUseWeapon(1)];";
		Assert.assertFalse(javaOf(4, code).contains("_v5"));
		var v5 = javaOf(5, code);
		Assert.assertTrue(v5, v5.contains("getCellsToUseChip_v5(") && v5.contains("getCellsToUseWeapon_v5("));
	}
}
