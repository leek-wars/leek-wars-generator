package test;

import static org.junit.Assume.assumeTrue;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.util.Json;

import leekscript.compiler.AIFile;
import leekscript.runner.LeekRunException;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * BANC : temps réel des fonctions de combat rapporté aux opérations qu'elles facturent.
 *
 * <p>Une IA qui boucle sur une fonction sous-facturée atteint son plafond d'opérations après des
 * secondes de calcul : la génération du combat s'allonge d'autant, sans que le joueur le paie. Ce
 * banc pose une carte et des positions réelles (4 contre 4, un camp replié dans un coin, l'autre au
 * contact : aucun poireau du coin ne peut bouger) et mesure depuis du LeekScript compilé :
 * <ul>
 * <li>{@link #benchFightCosts} : chaque fonction en boucle, en nanosecondes par opération facturée,
 *     rapportées à une boucle arithmétique ;</li>
 * <li>{@link #benchInfiniteLoops} : des boucles sans fin dont le coût dépend d'un état qui grossit,
 *     d'un échec ou d'un bord de carte, en millisecondes réelles avant le plafond d'opérations d'un
 *     poireau à 7 cœurs. Au-delà de 100 ms, la boucle est sous-facturée.</li>
 * </ul>
 * Diagnostic, sans seuil : les deux tableaux se lisent. Seule la boucle de fuite d'un poireau acculé
 * est gardée par un test ({@link #corneredFleeLoopReachesItsOperationLimitQuickly}). Les tests
 * tournent avec les options de la JVM du worker (build.gradle), sans quoi une exception implicite
 * rattrapée sur un chemin chaud ne se verrait pas.
 *
 * <pre>FIGHT_BENCH=1 gradle --offline :cleanTest :test --tests "test.BenchFightCosts"</pre>
 * FIGHT_BENCH_ONLY=nom,nom restreint les cas.
 */
public class BenchFightCosts extends FightTestBase {

	private static final String OBSTACLES = "{\"9\":84,\"19\":84,\"60\":84,\"61\":83,\"118\":83,\"127\":83,\"190\":83,\"196\":68,\"232\":68,"
		+ "\"236\":76,\"269\":83,\"270\":69,\"274\":77,\"299\":84,\"312\":70,\"324\":83,\"344\":72,\"346\":79,\"367\":84,\"376\":69,\"403\":83,"
		+ "\"406\":84,\"408\":71,\"410\":73,\"412\":68,\"424\":84,\"448\":68,\"457\":83,\"476\":84,\"482\":78,\"514\":83,\"603\":83}";
	/** Cases de départ, par camp : le premier, acculé dans le coin, est celui qu'on mesure. */
	private static final int[][] TEAMS = { { 35, 70, 0, 1 }, { 53, 88, 18, 109 } };

	/** Plafond d'opérations des boucles sans fin : celui d'un poireau à 7 cœurs. */
	private static final int LOOP_OPERATIONS = 7_000_000;
	/** Plafond des cas à nombre d'itérations fixé : jamais atteint. */
	private static final int NO_LIMIT = 2_000_000_000;
	/** Seuil du test de garde : la boucle de fuite d'un poireau acculé, à son plafond d'opérations. */
	private static final long FLEE_LOOP_MAX_MS = 1000;

	/** Fonction appelée `iterations` fois : `setup` hors boucle, `expression` à chaque itération. */
	private record Case(String name, String setup, String expression, int iterations) {
		String program() {
			return "var e = getNearestEnemy();\nvar far = getFarthestEnemy();\nvar c = getCell();\n" + setup
				+ "\nvar s = 0;\nfor (var i = 0; i < " + iterations + "; i++) {\n\tvar r = " + expression + ";\n\tif (r !== null) s += r;\n}\nreturn s;";
		}
	}

	private record Loop(String name, String program) {}

	/** Un passage d'une IA : temps réel, opérations, RAM, comment il s'est terminé et ce qu'il a rendu. */
	private record Pass(long nanos, long ops, long ram, String end, Object result) {}

	private static final Case REFERENCE = new Case("reference", "", "i", 2000000);

	private static final Case[] CASES = {
		new Case("moveAwayFrom", "", "moveAwayFrom(e)", 300),
		new Case("moveAwayFromCell", "", "moveAwayFromCell(getCell(e))", 300),
		new Case("moveAwayFromEntities", "var es = getAliveEnemies();", "moveAwayFromEntities(es)", 300),
		new Case("moveAwayFromCells", "var cs = [53, 88, 18, 109];", "moveAwayFromCells(cs)", 300),
		new Case("moveAwayFromLine", "", "moveAwayFromLine(53, 88)", 300),
		new Case("moveToward", "", "moveToward(e)", 300),
		new Case("moveToward_far", "", "moveToward(far)", 300),
		new Case("moveTowardCell", "", "moveTowardCell(612)", 300),
		new Case("moveTowardCell_obstacle", "", "moveTowardCell(60)", 300),
		new Case("moveTowardEntities", "var es = getAliveEnemies();", "moveTowardEntities(es)", 300),
		new Case("moveTowardCells", "var cs = [612, 600, 300];", "moveTowardCells(cs)", 300),
		new Case("moveTowardLine", "", "moveTowardLine(300, 306)", 300),
		new Case("getPathLength", "", "getPathLength(c, 612)", 3000),
		new Case("getPathLength_blocked", "", "getPathLength(c, 0)", 3000),
		new Case("getPath", "", "count(getPath(c, 612))", 3000),
		new Case("getPath_ignored", "var ig = [53, 88, 18, 109, 0, 1, 70];", "count(getPath(c, 612, ig))", 3000),
		new Case("lineOfSight", "", "(lineOfSight(c, 612) ? 1 : 0)", 100000),
		new Case("lineOfSight_ignored", "var ig = getAliveEnemies();", "(lineOfSight(c, 612, ig) ? 1 : 0)", 100000),
		new Case("getCellDistance", "", "getCellDistance(c, 612)", 500000),
		new Case("getDistance", "", "getDistance(c, 612)", 500000),
		new Case("isOnSameLine", "", "(isOnSameLine(c, 612) ? 1 : 0)", 500000),
		new Case("getNearestEnemy", "", "getNearestEnemy()", 200000),
		new Case("getFarthestEnemy", "", "getFarthestEnemy()", 200000),
		new Case("getNearestAlly", "", "getNearestAlly()", 200000),
		new Case("getFarthestAlly", "", "getFarthestAlly()", 200000),
		new Case("getNearestEnemyTo", "", "getNearestEnemyTo(far)", 200000),
		new Case("getNearestEnemyToCell", "", "getNearestEnemyToCell(300)", 200000),
		new Case("getNearestAllyTo", "", "getNearestAllyTo(far)", 200000),
		new Case("getNearestAllyToCell", "", "getNearestAllyToCell(300)", 200000),
		new Case("getAliveEnemies", "", "count(getAliveEnemies())", 200000),
		new Case("getAliveAllies", "", "count(getAliveAllies())", 200000),
		new Case("getEnemies", "", "count(getEnemies())", 200000),
		new Case("getAllies", "", "count(getAllies())", 200000),
		new Case("getDeadEnemies", "", "count(getDeadEnemies())", 200000),
		new Case("getAliveEnemiesCount", "", "getAliveEnemiesCount()", 200000),
		new Case("getEnemiesLife", "", "getEnemiesLife()", 200000),
		new Case("getObstacles", "", "count(getObstacles())", 20000),
		new Case("getCell", "", "getCell(e)", 500000),
		new Case("getLife", "", "getLife(e)", 500000),
		new Case("getEntityOnCell", "", "(getEntityOnCell(53) === null ? 0 : 1)", 500000),
		new Case("getCellContent", "", "getCellContent(53)", 500000),
		new Case("isEmptyCell", "", "(isEmptyCell(300) ? 1 : 0)", 500000),
		new Case("isObstacle", "", "(isObstacle(60) ? 1 : 0)", 500000),
		new Case("getCellFromXY", "", "getCellFromXY(3, 4)", 500000),
		new Case("getCellX", "", "getCellX(300)", 500000),
		new Case("getEffects", "", "count(getEffects(e))", 200000),
		new Case("getLaunchedEffects", "", "count(getLaunchedEffects(e))", 200000),
		new Case("getPassiveEffects", "", "count(getPassiveEffects(e))", 200000),
		new Case("getWeapons", "", "count(getWeapons())", 200000),
		new Case("getChips", "", "count(getChips())", 200000),
		new Case("getMessages", "", "count(getMessages())", 200000),
		new Case("getName", "", "length(getName(e))", 200000),
		new Case("getEntityTurnOrder", "", "getEntityTurnOrder(e)", 200000),
		new Case("getSummons", "", "count(getSummons())", 200000),
		new Case("getAllChips", "", "count(getAllChips())", 20000),
		new Case("getAllWeapons", "", "count(getAllWeapons())", 20000),
		new Case("getAllEffects", "", "count(getAllEffects())", 20000),
		new Case("getWeaponEffects", "", "count(getWeaponEffects(WEAPON_PISTOL))", 200000),
		new Case("getChipEffects", "", "count(getChipEffects(CHIP_SPARK))", 200000),
		new Case("getWeaponArea", "", "getWeaponArea(WEAPON_GRENADE_LAUNCHER)", 200000),
		new Case("getWeaponName", "", "length(getWeaponName(WEAPON_PISTOL))", 200000),
		new Case("canUseWeapon", "", "(canUseWeapon(WEAPON_PISTOL, e) ? 1 : 0)", 200000),
		new Case("canUseWeaponOnCell", "", "(canUseWeaponOnCell(WEAPON_GRENADE_LAUNCHER, 300) ? 1 : 0)", 200000),
		new Case("canUseChip", "", "(canUseChip(CHIP_SPARK, e) ? 1 : 0)", 200000),
		new Case("canUseChipOnCell", "", "(canUseChipOnCell(CHIP_SPARK, 300) ? 1 : 0)", 200000),
		new Case("getCellToUseWeapon", "", "getCellToUseWeapon(WEAPON_PISTOL, far)", 300),
		new Case("getCellToUseWeaponOnCell", "", "getCellToUseWeaponOnCell(WEAPON_GRENADE_LAUNCHER, 300)", 300),
		new Case("getCellToUseChip", "", "getCellToUseChip(CHIP_SPARK, far)", 300),
		new Case("getCellToUseChipOnCell", "", "getCellToUseChipOnCell(CHIP_SPARK, 300)", 300),
		new Case("getCellsToUseWeapon", "", "count(getCellsToUseWeapon(WEAPON_PISTOL, far))", 300),
		new Case("getCellsToUseWeaponOnCell", "", "count(getCellsToUseWeaponOnCell(WEAPON_GRENADE_LAUNCHER, 300))", 300),
		new Case("getCellsToUseChip", "", "count(getCellsToUseChip(CHIP_SPARK, far))", 300),
		new Case("getCellsToUseChipOnCell", "", "count(getCellsToUseChipOnCell(CHIP_SPARK, 300))", 300),
		new Case("getCellsToUseWeapon_ignored", "var ig = [53, 88, 18, 109, 0, 1, 70];", "count(getCellsToUseWeapon(WEAPON_PISTOL, far, ig))", 300),
		new Case("getWeaponTargets", "", "count(getWeaponTargets(WEAPON_GRENADE_LAUNCHER, 53))", 100000),
		new Case("getChipTargets", "", "count(getChipTargets(CHIP_METEORITE, 53))", 100000),
		new Case("getWeaponEffectiveArea", "", "count(getWeaponEffectiveArea(WEAPON_GRENADE_LAUNCHER, 53))", 100000),
		new Case("getChipEffectiveArea", "", "count(getChipEffectiveArea(CHIP_METEORITE, 53))", 100000),
		new Case("getWeaponEffectiveArea_from", "", "count(getWeaponEffectiveArea(WEAPON_FLAME_THROWER, 53, 35))", 100000),
		new Case("useWeapon_none", "", "useWeapon(e)", 500),
		new Case("useChip_none", "", "useChip(CHIP_SPARK, e)", 500),
		new Case("setWeapon_none", "", "(setWeapon(WEAPON_PISTOL) ? 1 : 0)", 500),
		new Case("summon_none", "", "summon(CHIP_PUNY_BULB, 36, function() {})", 500),
		new Case("say", "", "(say('a') ? 1 : 0)", 500),
		new Case("debug", "", "(debug('abcdefghij') === null ? 1 : 0)", 100000),
		new Case("debugC", "", "(debugC('abcdefghij', 255) === null ? 1 : 0)", 100000),
		new Case("mark", "var cs = [300, 301, 302, 303];", "(mark(cs, 255, 1) ? 1 : 0)", 100000),
		new Case("markText", "", "(markText(300, 'ab', 255, 1) ? 1 : 0)", 100000),
		new Case("clearMarks", "", "(clearMarks() === null ? 1 : 0)", 100000),
		new Case("sendAll", "", "(sendAll(MESSAGE_CUSTOM, i) === null ? 1 : 0)", 100000),
		new Case("sendTo", "", "(sendTo(2, MESSAGE_CUSTOM, i) ? 1 : 0)", 100000),
		new Case("getRegisters", "", "count(getRegisters())", 100000),
		new Case("setRegister", "", "(setRegister('a', 'b') ? 1 : 0)", 100000),
		new Case("getRegister", "", "(getRegister('a') === null ? 1 : 0)", 100000),
		new Case("getMapType", "", "getMapType()", 500000),
		new Case("getBulbChips", "", "count(getBulbChips(CHIP_PUNY_BULB))", 100000),
		new Case("getWeaponPassiveEffects", "", "count(getWeaponPassiveEffects(WEAPON_PISTOL))", 100000),
		new Case("getItemUses", "", "getItemUses(CHIP_SPARK)", 200000),
		new Case("getCooldown", "", "getCooldown(CHIP_SPARK)", 200000),
		new Case("getOperations", "", "getOperations()", 500000),
	};

	private static final Loop[] LOOPS = {
		new Loop("arith", "var s = 0; while (true) { s += 1 }"),
		new Loop("string +=", "var s = ''; while (true) { s += 'a' }"),
		new Loop("string + int", "var s = ''; var i = 0; while (true) { s = s + i++ }"),
		new Loop("push", "var a = []; while (true) { push(a, 1) }"),
		new Loop("array + [x]", "var a = []; while (true) { a = a + [1] }"),
		new Loop("a += [x]", "var a = []; while (true) { a += [1] }"),
		new Loop("unshift", "var a = []; while (true) { unshift(a, 1) }"),
		new Loop("insert 0", "var a = []; while (true) { insert(a, 1, 0) }"),
		new Loop("map grow", "var m = [:]; var i = 0; while (true) { m[i++] = 1 }"),
		new Loop("set grow", "var m = <>; var i = 0; while (true) { setPut(m, i++) }"),
		new Loop("inArray grow", "var a = []; var i = 0; while (true) { if (!inArray(a, i)) push(a, i++) }"),
		new Loop("search grow", "var a = []; var i = 0; while (true) { if (search(a, i) === null) push(a, i++) }"),
		new Loop("arrayConcat", "var a = [1]; while (true) { a = arrayConcat(a, [1]) }"),
		new Loop("clone grow", "var a = []; while (true) { push(a, 1); var b = clone(a) }"),
		new Loop("string(a) grow", "var a = []; while (true) { push(a, 1); var b = string(a) }"),
		new Loop("count string", "var s = ''; while (true) { s += 'a'; var n = length(s) }"),
		new Loop("jsonEncode grow", "var a = []; while (true) { push(a, 1); var b = jsonEncode(a) }"),
		new Loop("join grow", "var a = []; while (true) { push(a, 'a'); var b = join(a, ',') }"),
		new Loop("sort grow", "var a = []; while (true) { push(a, 1); sort(a) }"),
		new Loop("arrayMax grow", "var a = []; while (true) { push(a, 1); var b = arrayMax(a) }"),
		new Loop("sum grow", "var a = []; while (true) { push(a, 1); var b = sum(a) }"),
		new Loop("reverse grow", "var a = []; while (true) { push(a, 1); reverse(a) }"),
		new Loop("shuffle grow", "var a = []; while (true) { push(a, 1); shuffle(a) }"),
		new Loop("removeElement grow", "var a = []; while (true) { push(a, 1); removeElement(a, 2) }"),
		new Loop("arrayFilter grow", "var a = []; while (true) { push(a, 1); var b = arrayFilter(a, function(x) { return x > 1 }) }"),
		new Loop("arrayMap grow", "var a = []; while (true) { push(a, 1); var b = arrayMap(a, function(x) { return x }) }"),
		new Loop("debug array grow", "var a = []; while (true) { push(a, 1); debug(a) }"),
		new Loop("sendAll+getMessages", "while (true) { sendAll(MESSAGE_CUSTOM, 1); var m = getMessages() }"),
		new Loop("moveAwayFrom", "var e = getNearestEnemy(); while (true) { moveAwayFrom(e) }"),
		new Loop("number bad", "while (true) { number('abc') }"),
		new Loop("number bad long", "var s = 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz'; while (true) { number(s) }"),
		new Loop("jsonDecode bad", "while (true) { jsonDecode('{') }"),
		new Loop("string * int", "var s = 'abc'; var x = 0; while (true) { x = s * 2 }"),
		new Loop("string < int", "var s = 'abc'; var x = 0; while (true) { x = s < 2 }"),
		new Loop("getCellFromXY out", "while (true) { getCellFromXY(100, 100) }"),
		new Loop("getChipEffectiveArea corner", "while (true) { getChipEffectiveArea(CHIP_METEORITE, 0) }"),
		new Loop("getWeaponEffectiveArea corner", "while (true) { getWeaponEffectiveArea(WEAPON_GRENADE_LAUNCHER, 0) }"),
		new Loop("getCellsToUseChipOnCell corner", "while (true) { getCellsToUseChipOnCell(CHIP_SPARK, 0) }"),
		new Loop("lineOfSight corner", "while (true) { lineOfSight(0, 17) }"),
		new Loop("setRegister grow", "var i = 0; while (true) { setRegister('k' + i++, 'v') }"),
	};

	private final Leek[] leeks = new Leek[8];
	private final Cell[] startCells = new Cell[8];

	@Override
	protected void createLeeks() {
		for (int i = 0; i < leeks.length; i++) {
			// 2000 cœurs : le plafond d'opérations du poireau ne coupe aucune mesure
			leeks[i] = new Leek(i + 1, "L" + (i + 1), i < 4 ? 1 : 2, 300, 1500, 20, 7, 100, 100, 100, 100, 100, 100, 100, 2000, 13, 0, false, 0, 0, "", 0, "", "", "", 0);
			fight.getState().addEntity(i < 4 ? 0 : 1, leeks[i]);
		}
	}

	/** Un poireau acculé qui boucle sur moveAwayFrom atteint son plafond d'opérations en ~30 ms. */
	@Test
	public void corneredFleeLoopReachesItsOperationLimitQuickly() throws Exception {
		setupMap();
		// Le passage le plus rapide : une pause de la machine ne fait pas échouer le test
		Pass pass = measure("var e = getNearestEnemy(); while (getMP() > 0 && getLife(e) > 0) { moveAwayFrom(e) }", LOOP_OPERATIONS, 3, false);
		Assert.assertEquals("TOO_MUCH_OPERATIONS", pass.end);
		Assert.assertTrue("boucle de fuite : " + pass.nanos / 1_000_000 + " ms pour " + pass.ops + " opérations", pass.nanos < FLEE_LOOP_MAX_MS * 1_000_000);
	}

	@Test
	public void benchFightCosts() throws Exception {
		assumeTrue("FIGHT_BENCH=1 pour lancer le banc", "1".equals(System.getenv("FIGHT_BENCH")));
		setupMap();
		// Le passage le plus rapide : le coût de la fonction, sans le bruit
		Pass reference = measure(REFERENCE.program(), NO_LIMIT, 4, false);
		double referenceNsPerOp = reference.nanos / (double) reference.ops;
		System.out.printf("%-30s %12s %10s %12s %10s %10s%n", "fonction", "ns/appel", "ops/appel", "ns/op", "x ref", "retour");
		for (Case c : CASES) {
			if (skipped(c.name)) continue;
			try {
				Pass best = measure(c.program(), NO_LIMIT, 4, false);
				double nsPerOp = best.nanos / (double) best.ops;
				System.out.printf("%-30s %12.0f %10.0f %12.2f %10.1f %10s%n", c.name, best.nanos / (double) c.iterations, best.ops / (double) c.iterations, nsPerOp, nsPerOp / referenceNsPerOp, best.result);
			} catch (Throwable e) {
				System.out.printf("%-30s ERREUR %s%n", c.name, e);
			}
		}
	}

	@Test
	public void benchInfiniteLoops() throws Exception {
		assumeTrue("FIGHT_BENCH=1 pour lancer le banc", "1".equals(System.getenv("FIGHT_BENCH")));
		setupMap();
		System.out.printf("%-30s %10s %12s %10s %10s  %s%n", "boucle", "ms", "ops", "ns/op", "RAM", "fin");
		for (Loop loop : LOOPS) {
			if (skipped(loop.name)) continue;
			try {
				// Le passage le plus lent : ce qu'un tour peut coûter
				Pass pass = measure(loop.program, LOOP_OPERATIONS, 5, true);
				System.out.printf("%-30s %10.1f %12d %10.1f %10d  %s%n", loop.name, pass.nanos / 1e6, pass.ops, pass.nanos / (double) pass.ops, pass.ram, pass.end);
			} catch (Throwable e) {
				System.out.printf("%-30s ERREUR %s%n", loop.name, e);
			}
		}
	}

	private void setupMap() throws Exception {
		ObjectNode map = Json.createObject();
		map.put("id", 0);
		map.set("obstacles", Json.parse(OBSTACLES));
		map.set("pattern", Json.createArray());
		map.set("team1", cells(TEAMS[0]));
		map.set("team2", cells(TEAMS[1]));
		fight.getState().setCustomMap(map);
		initFightOnly();
		for (int i = 0; i < leeks.length; i++) startCells[i] = leeks[i].getCell();
	}

	private static ArrayNode cells(int[] ids) {
		ArrayNode array = Json.createArray();
		for (int id : ids) array.add(id);
		return array;
	}

	/** Cas écarté par FIGHT_BENCH_ONLY. */
	private static boolean skipped(String name) {
		String only = System.getenv("FIGHT_BENCH_ONLY");
		return only != null && !("," + only + ",").contains("," + name + ",");
	}

	/**
	 * `passes` passages de `code` par le premier poireau du coin, après un passage qui compile : le
	 * plus lent ou le plus rapide. Chacun part comme au début d'un tour (PM et PT pleins, poireau à sa
	 * case, journal vide : plein, on mesurerait son refus d'écrire) ; messages, marques et registres,
	 * eux, s'accumulent d'un cas à l'autre.
	 */
	private Pass measure(String code, int maxOperations, int passes, boolean slowest) throws Exception {
		Leek leek = leeks[0];
		attachAI(leek, newAIFile(code, leek.getId()));
		EntityAI ai = EntityAI.build(generator, (AIFile) leek.getAIFile(), leek);
		leek.setAI(ai);
		Pass kept = null;
		for (int k = 0; k <= passes; k++) {
			leek.endTurn();
			if (leek.getCell() != startCells[0]) fight.getState().getMap().moveEntity(leek, startCells[0]);
			leek.setLogs(new LeekLog(new FarmerLog(fight, 0), leek));
			ai.resetCounter();
			ai.setMaxOperations(maxOperations);
			String end = "fin normale";
			Object result = null;
			long t = System.nanoTime();
			try {
				result = ai.runIA();
			} catch (LeekRunException e) {
				end = String.valueOf(e.getError());
			}
			Pass pass = new Pass(System.nanoTime() - t, ai.getOperations(), ai.getUsedRAM(), end, result);
			if (k > 0 && (kept == null || (slowest ? pass.nanos > kept.nanos : pass.nanos < kept.nanos))) kept = pass;
		}
		return kept;
	}
}
