package test;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.util.Json;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
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
 * <li>{@link #benchInfiniteLoops} : des boucles sans fin, en millisecondes réelles avant le plafond
 *     d'opérations d'un poireau à 7 cœurs. Au-delà de 100 ms, la boucle est sous-facturée.</li>
 * </ul>
 *
 * <p>À lancer aussi avec les options de la JVM du worker : sans {@code OmitStackTraceInFastThrow},
 * une exception implicite rattrapée (indice hors tableau, null) coûte des microsecondes au lieu de
 * nanosecondes, et le banc ne voit rien avec les options par défaut.
 *
 * <pre>JAVA_TOOL_OPTIONS=-XX:-OmitStackTraceInFastThrow FIGHT_BENCH=1 gradle --offline :cleanTest :test --tests "test.BenchFightCosts"</pre>
 */
public class BenchFightCosts extends FightTestBase {

	private static final String OBSTACLES = "{\"9\":84,\"19\":84,\"60\":84,\"61\":83,\"118\":83,\"127\":83,\"190\":83,\"196\":68,\"232\":68,"
		+ "\"236\":76,\"269\":83,\"270\":69,\"274\":77,\"299\":84,\"312\":70,\"324\":83,\"344\":72,\"346\":79,\"367\":84,\"376\":69,\"403\":83,"
		+ "\"406\":84,\"408\":71,\"410\":73,\"412\":68,\"424\":84,\"448\":68,\"457\":83,\"476\":84,\"482\":78,\"514\":83,\"603\":83}";
	private static final int[] TEAM1 = { 35, 70, 0, 1 };
	private static final int[] TEAM2 = { 53, 88, 18, 109 };

	private final Leek[] leeks = new Leek[8];

	@Override
	protected void createLeeks() {
		for (int i = 0; i < 8; i++) {
			// 2000 cœurs : le plafond d'opérations ne coupe pas la mesure
			leeks[i] = new Leek(i + 1, "L" + (i + 1), i < 4 ? 1 : 2, 300, 1500, 20, 7, 100, 100, 100, 100, 100, 100, 100, 2000, 13, 0, false, 0, 0, "", 0, "", "", "", 0);
			fight.getState().addEntity(i < 4 ? 0 : 1, leeks[i]);
		}
	}

	private static ArrayNode cells(int[] ids) {
		ArrayNode array = Json.createArray();
		for (int id : ids) array.add(id);
		return array;
	}

	/** [nom, préparation (hors boucle), expression appelée à chaque itération, itérations]. */
	private static final String[][] CASES = {
		{"reference", "", "i", "2000000"},
		{"moveAwayFrom", "", "moveAwayFrom(e)", "300"},
		{"moveAwayFromCell", "", "moveAwayFromCell(getCell(e))", "300"},
		{"moveAwayFromEntities", "var es = getAliveEnemies();", "moveAwayFromEntities(es)", "300"},
		{"moveAwayFromCells", "var cs = [53, 88, 18, 109];", "moveAwayFromCells(cs)", "300"},
		{"moveAwayFromLine", "", "moveAwayFromLine(53, 88)", "300"},
		{"moveToward", "", "moveToward(e)", "300"},
		{"moveToward_far", "", "moveToward(far)", "300"},
		{"moveTowardCell", "", "moveTowardCell(612)", "300"},
		{"moveTowardCell_obstacle", "", "moveTowardCell(60)", "300"},
		{"moveTowardEntities", "var es = getAliveEnemies();", "moveTowardEntities(es)", "300"},
		{"moveTowardCells", "var cs = [612, 600, 300];", "moveTowardCells(cs)", "300"},
		{"moveTowardLine", "", "moveTowardLine(300, 306)", "300"},
		{"getPathLength", "", "getPathLength(c, 612)", "3000"},
		{"getPathLength_blocked", "", "getPathLength(c, 0)", "3000"},
		{"getPath", "", "count(getPath(c, 612))", "3000"},
		{"getPath_ignored", "var ig = [53, 88, 18, 109, 0, 1, 70];", "count(getPath(c, 612, ig))", "3000"},
		{"lineOfSight", "", "(lineOfSight(c, 612) ? 1 : 0)", "100000"},
		{"lineOfSight_ignored", "var ig = getAliveEnemies();", "(lineOfSight(c, 612, ig) ? 1 : 0)", "100000"},
		{"getCellDistance", "", "getCellDistance(c, 612)", "500000"},
		{"getDistance", "", "getDistance(c, 612)", "500000"},
		{"isOnSameLine", "", "(isOnSameLine(c, 612) ? 1 : 0)", "500000"},
		{"getNearestEnemy", "", "getNearestEnemy()", "200000"},
		{"getFarthestEnemy", "", "getFarthestEnemy()", "200000"},
		{"getNearestAlly", "", "getNearestAlly()", "200000"},
		{"getFarthestAlly", "", "getFarthestAlly()", "200000"},
		{"getNearestEnemyTo", "", "getNearestEnemyTo(far)", "200000"},
		{"getNearestEnemyToCell", "", "getNearestEnemyToCell(300)", "200000"},
		{"getNearestAllyTo", "", "getNearestAllyTo(far)", "200000"},
		{"getNearestAllyToCell", "", "getNearestAllyToCell(300)", "200000"},
		{"getAliveEnemies", "", "count(getAliveEnemies())", "200000"},
		{"getAliveAllies", "", "count(getAliveAllies())", "200000"},
		{"getEnemies", "", "count(getEnemies())", "200000"},
		{"getAllies", "", "count(getAllies())", "200000"},
		{"getDeadEnemies", "", "count(getDeadEnemies())", "200000"},
		{"getAliveEnemiesCount", "", "getAliveEnemiesCount()", "200000"},
		{"getEnemiesLife", "", "getEnemiesLife()", "200000"},
		{"getObstacles", "", "count(getObstacles())", "20000"},
		{"getCell", "", "getCell(e)", "500000"},
		{"getLife", "", "getLife(e)", "500000"},
		{"getEntityOnCell", "", "(getEntityOnCell(53) === null ? 0 : 1)", "500000"},
		{"getCellContent", "", "getCellContent(53)", "500000"},
		{"isEmptyCell", "", "(isEmptyCell(300) ? 1 : 0)", "500000"},
		{"isObstacle", "", "(isObstacle(60) ? 1 : 0)", "500000"},
		{"getCellFromXY", "", "getCellFromXY(3, 4)", "500000"},
		{"getCellX", "", "getCellX(300)", "500000"},
		{"getEffects", "", "count(getEffects(e))", "200000"},
		{"getLaunchedEffects", "", "count(getLaunchedEffects(e))", "200000"},
		{"getPassiveEffects", "", "count(getPassiveEffects(e))", "200000"},
		{"getWeapons", "", "count(getWeapons())", "200000"},
		{"getChips", "", "count(getChips())", "200000"},
		{"getMessages", "", "count(getMessages())", "200000"},
		{"getName", "", "length(getName(e))", "200000"},
		{"getEntityTurnOrder", "", "getEntityTurnOrder(e)", "200000"},
		{"getSummons", "", "count(getSummons())", "200000"},
		{"getAllChips", "", "count(getAllChips())", "20000"},
		{"getAllWeapons", "", "count(getAllWeapons())", "20000"},
		{"getAllEffects", "", "count(getAllEffects())", "20000"},
		{"getWeaponEffects", "", "count(getWeaponEffects(WEAPON_PISTOL))", "200000"},
		{"getChipEffects", "", "count(getChipEffects(CHIP_SPARK))", "200000"},
		{"getWeaponArea", "", "getWeaponArea(WEAPON_GRENADE_LAUNCHER)", "200000"},
		{"getWeaponName", "", "length(getWeaponName(WEAPON_PISTOL))", "200000"},
		{"canUseWeapon", "", "(canUseWeapon(WEAPON_PISTOL, e) ? 1 : 0)", "200000"},
		{"canUseWeaponOnCell", "", "(canUseWeaponOnCell(WEAPON_GRENADE_LAUNCHER, 300) ? 1 : 0)", "200000"},
		{"canUseChip", "", "(canUseChip(CHIP_SPARK, e) ? 1 : 0)", "200000"},
		{"canUseChipOnCell", "", "(canUseChipOnCell(CHIP_SPARK, 300) ? 1 : 0)", "200000"},
		{"getCellToUseWeapon", "", "getCellToUseWeapon(WEAPON_PISTOL, far)", "300"},
		{"getCellToUseWeaponOnCell", "", "getCellToUseWeaponOnCell(WEAPON_GRENADE_LAUNCHER, 300)", "300"},
		{"getCellToUseChip", "", "getCellToUseChip(CHIP_SPARK, far)", "300"},
		{"getCellToUseChipOnCell", "", "getCellToUseChipOnCell(CHIP_SPARK, 300)", "300"},
		{"getCellsToUseWeapon", "", "count(getCellsToUseWeapon(WEAPON_PISTOL, far))", "300"},
		{"getCellsToUseWeaponOnCell", "", "count(getCellsToUseWeaponOnCell(WEAPON_GRENADE_LAUNCHER, 300))", "300"},
		{"getCellsToUseChip", "", "count(getCellsToUseChip(CHIP_SPARK, far))", "300"},
		{"getCellsToUseChipOnCell", "", "count(getCellsToUseChipOnCell(CHIP_SPARK, 300))", "300"},
		{"getCellsToUseWeapon_ignored", "var ig = [53, 88, 18, 109, 0, 1, 70];", "count(getCellsToUseWeapon(WEAPON_PISTOL, far, ig))", "300"},
		{"getWeaponTargets", "", "count(getWeaponTargets(WEAPON_GRENADE_LAUNCHER, 53))", "100000"},
		{"getChipTargets", "", "count(getChipTargets(CHIP_METEORITE, 53))", "100000"},
		{"getWeaponEffectiveArea", "", "count(getWeaponEffectiveArea(WEAPON_GRENADE_LAUNCHER, 53))", "100000"},
		{"getChipEffectiveArea", "", "count(getChipEffectiveArea(CHIP_METEORITE, 53))", "100000"},
		{"getWeaponEffectiveArea_from", "", "count(getWeaponEffectiveArea(WEAPON_FLAME_THROWER, 53, 35))", "100000"},
		{"useWeapon_none", "", "useWeapon(e)", "500"},
		{"useChip_none", "", "useChip(CHIP_SPARK, e)", "500"},
		{"setWeapon_none", "", "(setWeapon(WEAPON_PISTOL) ? 1 : 0)", "500"},
		{"summon_none", "", "summon(CHIP_PUNY_BULB, 36, function() {})", "500"},
		{"say", "", "(say('a') ? 1 : 0)", "500"},
		{"debug", "", "(debug('abcdefghij') === null ? 1 : 0)", "100000"},
		{"debugC", "", "(debugC('abcdefghij', 255) === null ? 1 : 0)", "100000"},
		{"mark", "var cs = [300, 301, 302, 303];", "(mark(cs, 255, 1) ? 1 : 0)", "100000"},
		{"markText", "", "(markText(300, 'ab', 255, 1) ? 1 : 0)", "100000"},
		{"clearMarks", "", "(clearMarks() === null ? 1 : 0)", "100000"},
		{"sendAll", "", "(sendAll(MESSAGE_CUSTOM, i) === null ? 1 : 0)", "100000"},
		{"sendTo", "", "(sendTo(2, MESSAGE_CUSTOM, i) ? 1 : 0)", "100000"},
		{"getRegisters", "", "count(getRegisters())", "100000"},
		{"setRegister", "", "(setRegister('a', 'b') ? 1 : 0)", "100000"},
		{"getRegister", "", "(getRegister('a') === null ? 1 : 0)", "100000"},
		{"getMapType", "", "getMapType()", "500000"},
		{"getBulbChips", "", "count(getBulbChips(CHIP_PUNY_BULB))", "100000"},
		{"getWeaponPassiveEffects", "", "count(getWeaponPassiveEffects(WEAPON_PISTOL))", "100000"},
		{"getItemUses", "", "getItemUses(CHIP_SPARK)", "200000"},
		{"getCooldown", "", "getCooldown(CHIP_SPARK)", "200000"},
		{"getOperations", "", "getOperations()", "500000"},
	};

	/** Boucles sans fin : secondes réelles avant le plafond d'opérations d'un poireau à 7 cœurs. */
	private static final String[][] LOOPS = {
		{"arith", "var s = 0; while (true) { s += 1 }"},
		{"string +=", "var s = ''; while (true) { s += 'a' }"},
		{"string + int", "var s = ''; var i = 0; while (true) { s = s + i++ }"},
		{"push", "var a = []; while (true) { push(a, 1) }"},
		{"array + [x]", "var a = []; while (true) { a = a + [1] }"},
		{"a += [x]", "var a = []; while (true) { a += [1] }"},
		{"unshift", "var a = []; while (true) { unshift(a, 1) }"},
		{"insert 0", "var a = []; while (true) { insert(a, 1, 0) }"},
		{"map grow", "var m = [:]; var i = 0; while (true) { m[i++] = 1 }"},
		{"set grow", "var m = <>; var i = 0; while (true) { setPut(m, i++) }"},
		{"inArray grow", "var a = []; var i = 0; while (true) { if (!inArray(a, i)) push(a, i++) }"},
		{"search grow", "var a = []; var i = 0; while (true) { if (search(a, i) === null) push(a, i++) }"},
		{"arrayConcat", "var a = [1]; while (true) { a = arrayConcat(a, [1]) }"},
		{"clone grow", "var a = []; while (true) { push(a, 1); var b = clone(a) }"},
		{"string(a) grow", "var a = []; while (true) { push(a, 1); var b = string(a) }"},
		{"count string", "var s = ''; while (true) { s += 'a'; var n = length(s) }"},
		{"jsonEncode grow", "var a = []; while (true) { push(a, 1); var b = jsonEncode(a) }"},
		{"join grow", "var a = []; while (true) { push(a, 'a'); var b = join(a, ',') }"},
		{"sort grow", "var a = []; while (true) { push(a, 1); sort(a) }"},
		{"arrayMax grow", "var a = []; while (true) { push(a, 1); var b = arrayMax(a) }"},
		{"sum grow", "var a = []; while (true) { push(a, 1); var b = sum(a) }"},
		{"subArray grow", "var a = []; while (true) { push(a, 1); var b = subArray(a, 0, count(a) - 1) }"},
		{"reverse grow", "var a = []; while (true) { push(a, 1); reverse(a) }"},
		{"shuffle grow", "var a = []; while (true) { push(a, 1); shuffle(a) }"},
		{"removeElement grow", "var a = []; while (true) { push(a, 1); removeElement(a, 2) }"},
		{"arrayFilter grow", "var a = []; while (true) { push(a, 1); var b = arrayFilter(a, function(x) { return x > 1 }) }"},
		{"arrayMap grow", "var a = []; while (true) { push(a, 1); var b = arrayMap(a, function(x) { return x }) }"},
		{"debug", "while (true) { debug('abcdefghijklmnopqrstuvwxyz') }"},
		{"debug array grow", "var a = []; while (true) { push(a, 1); debug(a) }"},
		{"mark", "while (true) { mark([300, 301, 302, 303], 255, 1) }"},
		{"mark big", "var a = getObstacles(); while (true) { mark(a, 255, 1) }"},
		{"markText", "while (true) { markText(300, 'abcdef', 255, 1) }"},
		{"sendAll", "while (true) { sendAll(MESSAGE_CUSTOM, [1, 2, 3]) }"},
		{"sendAll+getMessages", "while (true) { sendAll(MESSAGE_CUSTOM, 1); var m = getMessages() }"},
		{"say", "while (true) { say('abc') }"},
		{"moveAwayFrom", "var e = getNearestEnemy(); while (true) { moveAwayFrom(e) }"},
		{"moveAwayFromEntities", "var e = getAliveEnemies(); while (true) { moveAwayFromEntities(e) }"},
		{"moveAwayFromCells", "var e = [53, 88, 18, 109]; while (true) { moveAwayFromCells(e) }"},
		{"moveAwayFromLine", "while (true) { moveAwayFromLine(53, 88) }"},
		{"moveToward", "var e = getNearestEnemy(); while (true) { moveToward(e) }"},
		{"moveToward far 1", "var e = getFarthestEnemy(); while (true) { moveToward(e, 1) }"},
		{"moveTowardCell", "while (true) { moveTowardCell(612) }"},
		{"moveTowardEntities", "var e = getAliveEnemies(); while (true) { moveTowardEntities(e) }"},
		{"moveTowardCells", "var e = [612, 300]; while (true) { moveTowardCells(e) }"},
		{"moveTowardLine", "while (true) { moveTowardLine(300, 306) }"},
		{"getPathLength", "var c = getCell(); while (true) { getPathLength(c, 612) }"},
		{"getPathLength other", "while (true) { getPathLength(300, 612) }"},
		{"getPath other", "while (true) { getPath(300, 612) }"},
		{"getPath other ignored", "var ig = getObstacles(); while (true) { getPath(300, 612, ig) }"},
		{"lineOfSight", "while (true) { lineOfSight(300, 612) }"},
		{"lineOfSight ignored", "var ig = getAliveEnemies() + getAliveAllies(); while (true) { lineOfSight(35, 612, ig) }"},
		{"getCellToUseWeapon", "var e = getFarthestEnemy(); while (true) { getCellToUseWeapon(WEAPON_GRENADE_LAUNCHER, e) }"},
		{"getCellToUseChip", "var e = getFarthestEnemy(); while (true) { getCellToUseChip(CHIP_SPARK, e) }"},
		{"getCellsToUseWeapon", "var e = getFarthestEnemy(); while (true) { getCellsToUseWeapon(WEAPON_GRENADE_LAUNCHER, e) }"},
		{"getCellsToUseChip", "var e = getFarthestEnemy(); while (true) { getCellsToUseChip(CHIP_SPARK, e) }"},
		{"getCellsToUseChipOnCell", "while (true) { getCellsToUseChipOnCell(CHIP_SPARK, 300) }"},
		{"getObstacles", "while (true) { getObstacles() }"},
		{"getWeaponTargets", "while (true) { getWeaponTargets(WEAPON_GRENADE_LAUNCHER, 53) }"},
		{"getChipEffectiveArea", "while (true) { getChipEffectiveArea(CHIP_METEORITE, 53) }"},
		{"getAllChips", "while (true) { getAllChips() }"},
		{"getRegisters", "while (true) { getRegisters() }"},
		{"number bad", "while (true) { number('abc') }"},
		{"number bad long", "var s = 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz'; while (true) { number(s) }"},
		{"number ok", "while (true) { number('123') }"},
		{"jsonDecode bad", "while (true) { jsonDecode('{') }"},
		{"jsonDecode ok", "while (true) { jsonDecode('[1,2]') }"},
		{"string * int", "var s = 'abc'; var x = 0; while (true) { x = s * 2 }"},
		{"string < int", "var s = 'abc'; var x = 0; while (true) { x = s < 2 }"},
		{"getCellFromXY out", "while (true) { getCellFromXY(100, 100) }"},
		{"getChipEffectiveArea corner", "while (true) { getChipEffectiveArea(CHIP_METEORITE, 0) }"},
		{"getWeaponEffectiveArea corner", "while (true) { getWeaponEffectiveArea(WEAPON_GRENADE_LAUNCHER, 0) }"},
		{"getCellsToUseChipOnCell corner", "while (true) { getCellsToUseChipOnCell(CHIP_SPARK, 0) }"},
		{"lineOfSight corner", "while (true) { lineOfSight(0, 17) }"},
		{"setRegister grow", "var i = 0; while (true) { setRegister('k' + i++, 'v') }"},
	};

	@Test
	public void benchInfiniteLoops() throws Exception {
		assumeTrue("FIGHT_BENCH=1 pour lancer le banc", "1".equals(System.getenv("FIGHT_BENCH")));
		setupMap();
		String only = System.getenv("FIGHT_BENCH_ONLY");
		Leek leek = leeks[Integer.parseInt(System.getenv().getOrDefault("FIGHT_BENCH_LEEK", "0"))];
		var start = leek.getCell();
		System.out.printf("%-26s %10s %12s %10s %6s %10s  %s%n", "boucle", "ms", "ops", "ns/op", "GC/6", "RAM", "fin");
		for (String[] c : LOOPS) {
			if (only != null && !("," + only + ",").contains("," + c[0] + ",")) continue;
			try {
				AIFile file = new AIFile("bench_loop_" + System.nanoTime(), c[1], System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
				leek.setAIFile(file);
				leek.setLogs(new LeekLog(new com.leekwars.generator.leek.FarmerLog(fight, 0), leek));
				leek.setFight(fight);
				EntityAI ai = EntityAI.build(generator, file, leek);
				leek.setAI(ai);
				long best = Long.MAX_VALUE;
				String end = "";
				long ops = 0, gcs = 0, ram = 0;
				for (int k = 0; k < 6; k++) {
					leek.useMP(leek.getMP() - leek.getTotalMP());
					if (leek.getCell() != start) fight.getState().getMap().moveEntity(leek, start);
					ai.resetCounter();
					ai.setMaxOperations(7_000_000);
					long gcBefore = gcCount();
					long t = System.nanoTime();
					try {
						ai.runIA();
						end = "fin normale";
					} catch (leekscript.runner.LeekRunException e) {
						end = String.valueOf(e.getError());
					}
					best = Math.min(best, System.nanoTime() - t);
					ops = ai.getOperations();
					gcs += gcCount() - gcBefore;
					ram = ai.getUsedRAM();
				}
				System.out.printf("%-26s %10.1f %12d %10.1f %6d %10d  %s%n", c[0], best / 1e6, ops, best / (double) ops, gcs, ram, end);
			} catch (Throwable e) {
				System.out.printf("%-26s ERREUR %s%n", c[0], e);
			}
		}
	}

	private static long gcCount() {
		long count = 0;
		for (var gc : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) count += gc.getCollectionCount();
		return count;
	}

	private void setupMap() throws Exception {
		ObjectNode map = Json.createObject();
		map.put("id", 0);
		map.set("obstacles", Json.parse(OBSTACLES));
		map.set("pattern", Json.createArray());
		map.set("team1", cells(TEAM1));
		map.set("team2", cells(TEAM2));
		fight.getState().setCustomMap(map);
		initFightOnly();
	}

	private static String program(String[] c) {
		return "var e = getNearestEnemy();\nvar far = getFarthestEnemy();\nvar c = getCell();\n" + c[1]
			+ "\nvar s = 0;\nfor (var i = 0; i < " + c[3] + "; i++) {\n\tvar r = " + c[2] + ";\n\tif (r !== null) s += r;\n}\nreturn s;";
	}

	@Test
	public void benchFightCosts() throws Exception {
		assumeTrue("FIGHT_BENCH=1 pour lancer le banc", "1".equals(System.getenv("FIGHT_BENCH")));

		setupMap();
		String only = System.getenv("FIGHT_BENCH_ONLY");
		int who = Integer.parseInt(System.getenv().getOrDefault("FIGHT_BENCH_LEEK", "0"));
		Leek leek = leeks[who];
		var start = leek.getCell();
		double reference = 0;
		System.out.printf("%-30s %12s %10s %12s %10s %10s%n", "fonction", "ns/appel", "ops/appel", "ns/op", "x ref", "retour");
		for (String[] c : CASES) {
			boolean isReference = c[0].equals("reference");
			if (only != null && !isReference && !("," + only + ",").contains("," + c[0] + ",")) continue;
			try {
				AIFile file = new AIFile("bench_fight_" + System.nanoTime(), program(c), System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
				leek.setAIFile(file);
				leek.setLogs(new LeekLog(farmerLog, leek));
				leek.setFight(fight);
				EntityAI ai = EntityAI.build(generator, file, leek);
				leek.setAI(ai);
				long best = Long.MAX_VALUE;
				Object result = null;
				long ops = 0;
				for (int k = 0; k < 4; k++) {
					// Chaque passage repart du début d'un tour : PM pleins, poireau à sa case
					leek.useMP(leek.getMP() - leek.getTotalMP());
					if (leek.getCell() != start) fight.getState().getMap().moveEntity(leek, start);
					ai.resetCounter();
					long t = System.nanoTime();
					result = ai.runIA();
					long elapsed = System.nanoTime() - t;
					// Le premier passage compile
					if (k > 0) best = Math.min(best, elapsed);
					ops = ai.getOperations();
				}
				long n = Long.parseLong(c[3]);
				double nsPerOp = best / (double) ops;
				if (isReference) reference = nsPerOp;
				System.out.printf("%-30s %12.0f %10.0f %12.2f %10.1f %10s%n", c[0], best / (double) n, ops / (double) n, nsPerOp, nsPerOp / reference, result);
			} catch (Throwable e) {
				System.out.printf("%-30s ERREUR %s%n", c[0], e);
			}
		}
	}
}
