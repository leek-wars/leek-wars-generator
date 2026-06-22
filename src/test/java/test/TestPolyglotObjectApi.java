package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;
import com.leekwars.generator.state.Entity;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * API de combat ORIENTÉE OBJET (tranche 1 : me / Entity / Cell / Fight) pour les IA polyglot.
 * Couche guest au-dessus de l'API plate. On vérifie les lectures de propriétés (me.life, me.cell.x),
 * l'accès aux entités (Fight.getNearestEnemy() -> Entity), et un vrai combat où deux IA en API objet
 * se rapprochent (me.moveToward(enemy), me.cell.distance(enemy)).
 */
public class TestPolyglotObjectApi extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	// Stats de combat (PV modérés pour finir dans la limite de tours) + équipement complet :
	// permet de tester une vraie attaque écrite 100 % en API objet (me.useWeapon...).
	private static Leek combatLeek(int id, String name) {
		return new Leek(id, name, 0, 150, 1200, 18, 6, 450, 200, 300, 100, 100, 0, 0, 8, 64,
			0, false, 0, 0, "", 0, "", "", "", 0);
	}

	private void equipEverything(Leek leek) {
		for (com.leekwars.generator.FightConstants c : com.leekwars.generator.FightConstants.values()) {
			String n = c.name();
			try {
				if (n.startsWith("CHIP_")) {
					var chip = com.leekwars.generator.chips.Chips.getChip(c.getIntValue());
					if (chip != null) leek.addChip(chip);
				} else if (n.startsWith("WEAPON_")) {
					var w = com.leekwars.generator.weapons.Weapons.getWeapon(c.getIntValue());
					if (w != null) leek.addWeapon(w);
				}
			} catch (Exception ignore) {}
		}
	}

	@Override
	protected void createLeeks() {
		leek1 = combatLeek(1, "Obj1");
		leek2 = combatLeek(2, "Obj2");
		equipEverything(leek1);
		equipEverything(leek2);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private Object eval(PolyglotSandbox sb, String src) throws Exception {
		PolyglotEntityAI ai = new PolyglotEntityAI("js", src, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai.runIA();
	}

	@Test
	public void entityPropertiesReadState() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			Assert.assertEquals((long) leek1.getLife(), ((Number) eval(sb, "me.life;")).longValue());
			Assert.assertEquals((long) leek1.getStat(Entity.STAT_STRENGTH), ((Number) eval(sb, "me.strength;")).longValue());
			// me.cell.x == getCellX(getCell()) ; me est l'IA courante.
			Object cx = eval(sb, "me.cell.x;");
			Assert.assertNotNull(cx);
			// Fight.getNearestEnemy() renvoie une Entity dont l'id est le fid de leek2.
			Assert.assertEquals((long) leek2.getFId(), ((Number) eval(sb, "Fight.getNearestEnemy().id;")).longValue());
			// distance de me a l'ennemi via l'objet == via l'API plate.
			Object dObj = eval(sb, "me.cell.distance(Fight.getNearestEnemy());");
			Object dFlat = eval(sb, "getCellDistance(getCell(), getCell(getNearestEnemy()));");
			Assert.assertEquals(((Number) dFlat).longValue(), ((Number) dObj).longValue());
		}
	}

	private Object evalPy(PolyglotSandbox sb, String expr) throws Exception {
		PolyglotEntityAI ai = new PolyglotEntityAI("python", "def turn():\n    return " + expr + "\n", sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai.runIA();
	}

	@Test
	public void pythonObjectPropertiesReadState() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals((long) leek1.getLife(), ((Number) evalPy(sb, "me.life")).longValue());
			Assert.assertEquals((long) leek1.getStat(Entity.STAT_STRENGTH), ((Number) evalPy(sb, "me.strength")).longValue());
			Assert.assertEquals((long) leek2.getFId(), ((Number) evalPy(sb, "Fight.getNearestEnemy().id")).longValue());
			Object dObj = evalPy(sb, "me.cell.distance(Fight.getNearestEnemy())");
			Object dFlat = evalPy(sb, "getCellDistance(getCell(), getCell(getNearestEnemy()))");
			Assert.assertEquals(((Number) dFlat).longValue(), ((Number) dObj).longValue());
			// Tranche 2 : objet Weapon en Python.
			Assert.assertEquals(((Number) evalPy(sb, "getWeaponCost(WEAPON_PISTOL)")).longValue(),
				((Number) evalPy(sb, "Weapon(WEAPON_PISTOL).cost")).longValue());
		}
	}

	@Test
	public void weaponChipFieldObjects() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Weapon : stats statiques (pas besoin d'equiper l'arme), coherentes avec l'API plate.
			Assert.assertEquals(((Number) eval(sb, "getWeaponCost(WEAPON_PISTOL);")).longValue(),
				((Number) eval(sb, "new Weapon(WEAPON_PISTOL).cost;")).longValue());
			Assert.assertEquals(((Number) eval(sb, "getWeaponMaxRange(WEAPON_PISTOL);")).longValue(),
				((Number) eval(sb, "new Weapon(WEAPON_PISTOL).maxRange;")).longValue());
			Assert.assertEquals(eval(sb, "getWeaponName(WEAPON_PISTOL);"),
				eval(sb, "new Weapon(WEAPON_PISTOL).name;"));
			// Chip : stats statiques.
			Assert.assertEquals(((Number) eval(sb, "getChipCost(CHIP_LIGHTNING);")).longValue(),
				((Number) eval(sb, "new Chip(CHIP_LIGHTNING).cost;")).longValue());
			// Field : cellFromXY renvoie une Cell coherente.
			Object cellObjX = eval(sb, "var c = Field.cellFromXY(0, 0); c == null ? -1 : c.id;");
			Object cellFlat = eval(sb, "var c = getCellFromXY(0, 0); c == null ? -1 : c;");
			Assert.assertEquals(((Number) cellFlat).longValue(), ((Number) cellObjX).longValue());
			Assert.assertEquals(((Number) eval(sb, "getMapType();")).longValue(),
				((Number) eval(sb, "Field.mapType;")).longValue());
			// me.weapon / me.weapons / me.chips : ne doivent pas lever (tableaux d'objets ou null).
			Assert.assertEquals(true, eval(sb, "Array.isArray(me.weapons) && Array.isArray(me.chips);"));
		}
	}

	@Test
	public void effectsStatesSummonsRegisters() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Registers : round-trip via le namespace objet (au-dessus de set/getRegister).
			Assert.assertEquals("hello", eval(sb, "Registers.set('k', 'hello'); Registers.get('k');"));
			// effects / states / summons : tableaux (ne lèvent pas).
			Assert.assertEquals(true, eval(sb,
				"Array.isArray(me.effects) && Array.isArray(me.states) && Array.isArray(me.summons);"));
			// summoned : booléen cohérent avec l'API plate.
			Assert.assertEquals(eval(sb, "isSummon(getEntity());"), eval(sb, "me.summoned;"));
		}
	}

	@Test
	public void debugMarkingDoesNotThrow() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Debug.mark accepte une Cell, un id, ou un tableau de Cells ; renvoie un bool.
			Assert.assertEquals(true, eval(sb,
				"Debug.mark(me.cell); Debug.mark([me.cell, Fight.getNearestEnemy().cell], 0xff0000, 2);"
				+ "Debug.markText(me.cell, 'ici'); Debug.clearMarks(); true;"));
		}
	}

	private void attachJsAI(Leek leek, String code) {
		AIFile file = new AIFile("obj_" + System.nanoTime() + ".js", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void objectApiMoverClosesDistanceInRealFight() throws Exception {
		String ai =
			"function turn() {"
			+ "  var enemy = Fight.getNearestEnemy();"
			+ "  if (enemy == null) return;"
			+ "  if (getRegister('start') == null) setRegister('start', '' + me.cell.distance(enemy));"
			+ "  me.moveToward(enemy);"
			+ "  setRegister('end', '' + me.cell.distance(enemy));"
			+ "}";
		attachJsAI(leek1, ai);
		attachJsAI(leek2, ai);
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		int s1 = Integer.parseInt(leek1.getRegister("start"));
		int e1 = Integer.parseInt(leek1.getRegister("end"));
		int s2 = Integer.parseInt(leek2.getRegister("start"));
		int e2 = Integer.parseInt(leek2.getRegister("end"));
		System.out.println("[objet] leek1 distance " + s1 + " -> " + e1 + " | leek2 " + s2 + " -> " + e2);
		Assert.assertTrue("leek1 (API objet) doit s'etre rapproche : " + s1 + " -> " + e1, e1 < s1);
		Assert.assertTrue("leek2 (API objet) doit s'etre rapproche : " + s2 + " -> " + e2, e2 < s2);
	}

	/**
	 * Validation de bout en bout : une IA écrite ENTIÈREMENT en API objet (aucune fonction plate)
	 * équipe une arme, se rapproche et tire — l'ennemi doit perdre des PV. Prouve que l'API objet
	 * suffit à écrire une vraie IA de combat (Fight.getNearestEnemy / me.weapons / me.setWeapon /
	 * me.moveToward / me.canUseWeapon / me.useWeapon).
	 */
	@Test
	public void fullObjectApiAttackerDealsDamage() throws Exception {
		String ai =
			"function turn() {"
			+ "  var e = Fight.getNearestEnemy();"
			+ "  if (e == null) return;"
			+ "  var ws = me.weapons;"
			+ "  if (ws.length > 0) me.setWeapon(ws[0]);"
			+ "  me.moveToward(e);"
			+ "  while (me.canUseWeapon(e)) { if (me.useWeapon(e) <= 0) break; }"
			+ "}";
		attachJsAI(leek1, ai);
		attachJsAI(leek2, ai);
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		int l1 = leek1.getLife();
		int l2 = leek2.getLife();
		System.out.println("[objet-attaque] leek1 vie=" + l1 + "/1200 | leek2 vie=" + l2 + "/1200");
		// Deux attaquants identiques en API objet : au moins un a infligé des dégâts.
		Assert.assertTrue("une attaque 100% API objet doit infliger des degats (l1=" + l1 + " l2=" + l2 + ")",
			l1 < 1200 || l2 < 1200);
	}
}
