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
				((Number) eval(sb, "Field.type;")).longValue());
			// me.weapon / me.weapons / me.chips : ne doivent pas lever (tableaux d'objets ou null).
			Assert.assertEquals(true, eval(sb, "Array.isArray(me.weapons) && Array.isArray(me.chips);"));
		}
	}

	/**
	 * Constantes objet Weapon.pistol / Chip.fireball : membres statiques = instances poolees.
	 * Weapon.pistol.id === WEAPON_PISTOL, .cost/.name coherents avec l'API plate, et l'identite
	 * de pool (Weapon.pistol === weap(meme id)) rend la comparaison par reference fiable. (#3179)
	 */
	@Test
	public void weaponChipObjectConstants() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Weapon.pistol est une instance dont l'id vaut la globale plate WEAPON_PISTOL.
			Assert.assertEquals(((Number) eval(sb, "WEAPON_PISTOL;")).longValue(),
				((Number) eval(sb, "Weapon.pistol.id;")).longValue());
			// Membres riches accessibles directement (coherents avec l'API plate).
			Assert.assertEquals(((Number) eval(sb, "getWeaponCost(WEAPON_PISTOL);")).longValue(),
				((Number) eval(sb, "Weapon.pistol.cost;")).longValue());
			Assert.assertEquals(eval(sb, "getWeaponName(WEAPON_PISTOL);"), eval(sb, "Weapon.pistol.name;"));
			// camelCase depuis SNAKE_CASE : WEAPON_MACHINE_GUN -> Weapon.machineGun.
			Assert.assertEquals(((Number) eval(sb, "WEAPON_MACHINE_GUN;")).longValue(),
				((Number) eval(sb, "Weapon.machineGun.id;")).longValue());
			// Chip pareil.
			Assert.assertEquals(((Number) eval(sb, "CHIP_LIGHTNING;")).longValue(),
				((Number) eval(sb, "Chip.lightning.id;")).longValue());
			// IDENTITE DE POOL : deux acces a la meme constante = MEME objet (comparable par ===).
			Assert.assertEquals(true, eval(sb, "Weapon.pistol === Weapon.pistol;"));
			// ... et l'arme equipee (via weap()) est le meme singleton que la constante objet.
			Assert.assertEquals(true, eval(sb,
				"me.setWeapon(Weapon.pistol); me.weapon === Weapon.pistol;"));
			// Ergonomie : passer la constante objet a une action l'unwrap (wid) comme un id plat.
			Assert.assertEquals(eval(sb, "getWeaponName(WEAPON_PISTOL);"),
				eval(sb, "me.setWeapon(Weapon.pistol); getWeaponName(getWeapon());"));
		}
	}

	/** Constantes objet Weapon.pistol aussi en PYTHON : instance poolee, identite avec l'arme equipee. */
	@Test
	public void weaponChipObjectConstantsPython() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals(((Number) evalPy(sb, "WEAPON_PISTOL")).longValue(),
				((Number) evalPy(sb, "Weapon.pistol.id")).longValue());
			Assert.assertEquals(((Number) evalPy(sb, "getWeaponCost(WEAPON_PISTOL)")).longValue(),
				((Number) evalPy(sb, "Weapon.pistol.cost")).longValue());
			// camelCase identique au runtime JS (WEAPON_MACHINE_GUN -> machineGun, CHIP_FIRE_BALL -> fireBall).
			Assert.assertEquals(((Number) evalPy(sb, "WEAPON_MACHINE_GUN")).longValue(),
				((Number) evalPy(sb, "Weapon.machineGun.id")).longValue());
			Assert.assertEquals(((Number) evalPy(sb, "CHIP_FIRE_BALL")).longValue(),
				((Number) evalPy(sb, "Chip.fireBall.id")).longValue());
			// Identite de pool : l'arme equipee est le MEME objet que la constante (Python `is`).
			// Tuple pour sequencer l'effet (setWeapon) puis l'assertion, sans court-circuit d'un `or`.
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "(me.setWeapon(Weapon.pistol), me.weapon is Weapon.pistol)[1]"));
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

	/**
	 * Les helpers qui rendaient des ids bruts renvoient désormais des OBJETS : ciblage
	 * (me.weaponCells -> Cell[]), chemin (Field.path / cell.path -> Cell[]), cibles touchées
	 * (me.weaponTargets -> Entity[]), proximité par cellule (Fight.getNearestEnemyToCell -> Entity),
	 * contenu de case (cell.content -> number). Vérifie le TYPE des éléments. (#3179)
	 */
	@Test
	public void objectReturningHelpers() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// weaponCells : tableau (éventuellement vide) dont les éléments sont des Cell.
			Assert.assertEquals(true, eval(sb,
				"me.setWeapon(Weapon.pistol);"
				+ "var cs = me.weaponCells(Fight.getNearestEnemy());"
				+ "Array.isArray(cs) && (cs.length === 0 || cs[0] instanceof Cell);"));
			// weaponCell : Cell ou null.
			Assert.assertEquals(true, eval(sb,
				"var c = me.weaponCell(Fight.getNearestEnemy()); c === null || c instanceof Cell;"));
			// Field.path et cell.path : Cell[].
			Assert.assertEquals(true, eval(sb,
				"var p = Field.path(me.cell, Fight.getNearestEnemy().cell);"
				+ "Array.isArray(p) && (p.length === 0 || p[0] instanceof Cell);"));
			Assert.assertEquals(true, eval(sb,
				"var p = me.cell.path(Fight.getNearestEnemy()); Array.isArray(p) && (p.length === 0 || p[0] instanceof Cell);"));
			// getNearestEnemyToCell : Entity.
			Assert.assertEquals(true, eval(sb,
				"var e = Fight.getNearestEnemyToCell(me.cell); e === null || e instanceof Entity;"));
			// weaponTargets : Entity[].
			Assert.assertEquals(true, eval(sb,
				"me.setWeapon(Weapon.pistol);"
				+ "var t = me.weaponTargets(Fight.getNearestEnemy().cell);"
				+ "Array.isArray(t) && (t.length === 0 || t[0] instanceof Entity);"));
			// cell.content : number cohérent avec l'API plate.
			Assert.assertEquals(((Number) eval(sb, "getCellContent(getCell());")).longValue(),
				((Number) eval(sb, "me.cell.content;")).longValue());
		}
	}

	/** Idem en PYTHON : helpers de ciblage/chemin/cibles renvoient des objets. */
	@Test
	public void objectReturningHelpersPython() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals(Boolean.TRUE, evalPy(sb,
				"(me.setWeapon(Weapon.pistol), all(isinstance(c, Cell) for c in me.weaponCells(Fight.getNearestEnemy())))[1]"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb,
				"all(isinstance(c, Cell) for c in Field.path(me.cell, Fight.getNearestEnemy().cell))"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb,
				"all(isinstance(c, Cell) for c in me.cell.path(Fight.getNearestEnemy()))"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb,
				"(lambda e: e is None or isinstance(e, Entity))(Fight.getNearestEnemyToCell(me.cell))"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb,
				"(me.setWeapon(Weapon.pistol), all(isinstance(t, Entity) for t in me.weaponTargets(Fight.getNearestEnemy().cell)))[1]"));
		}
	}

	/**
	 * Full POO : hiérarchie Item (Weapon/Chip extends Item), conteneurs de constantes par famille
	 * (Effect.SHIELD, State.PACIFIST, Entity.Stat.STRENGTH, Fight.Type.SOLO, Item.LaunchType.LINE,
	 * Field.NEXUS, Chest.Type.WOOD...) et dispatch d'instances typées (getNearestEnemy -> Leek). Les
	 * valeurs objet doivent coïncider avec les globales plates (conservées au runtime). (#3179)
	 */
	@Test
	public void objectConstantContainersAndHierarchy() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Hiérarchie Item : une arme/puce EST un Item.
			Assert.assertEquals(true, eval(sb, "(new Weapon(WEAPON_PISTOL) instanceof Item) && (new Chip(CHIP_LIGHTNING) instanceof Item);"));
			// Conteneurs de catégories == globales plates (MAJUSCULES, valeur identique).
			Assert.assertEquals(true, eval(sb, "Effect.ABSOLUTE_SHIELD === EFFECT_ABSOLUTE_SHIELD && Effect.DAMAGE === EFFECT_DAMAGE;"));
			Assert.assertEquals(true, eval(sb, "State.UNHEALABLE === STATE_UNHEALABLE;"));
			Assert.assertEquals(true, eval(sb, "Entity.Stat.STRENGTH === STAT_STRENGTH;"));
			Assert.assertEquals(true, eval(sb, "Entity.Type.LEEK === ENTITY_LEEK;"));
			// Anti-pollution : Bulb.Type (propre) ne doit PAS hériter Entity.Type -> pas de LEEK dessus.
			Assert.assertEquals(true, eval(sb, "Bulb.Type.PUNY === BULB_PUNY && Bulb.Type.LEEK === undefined;"));
			Assert.assertEquals(true, eval(sb, "Cell.Type.EMPTY === CELL_EMPTY;"));
			Assert.assertEquals(true, eval(sb, "Item.LaunchType.LINE === LAUNCH_TYPE_LINE;"));
			Assert.assertEquals(true, eval(sb, "Item.Area.CIRCLE_1 === AREA_CIRCLE_1;"));
			Assert.assertEquals(true, eval(sb, "Field.NEXUS === MAP_NEXUS;"));
			// Sous-conteneurs de Fight.
			Assert.assertEquals(true, eval(sb, "Fight.Type.SOLO === FIGHT_TYPE_SOLO;"));
			Assert.assertEquals(true, eval(sb, "Fight.Context.GARDEN === FIGHT_CONTEXT_GARDEN;"));
			Assert.assertEquals(true, eval(sb, "Fight.Boss.FENNEL_KING === BOSS_FENNEL_KING;"));
			Assert.assertEquals(true, eval(sb, "Fight.Erosion.DAMAGE === EROSION_DAMAGE;"));
			Assert.assertEquals(true, eval(sb, "Fight.Use.SUCCESS === USE_SUCCESS;"));
			Assert.assertEquals(true, eval(sb, "Fight.Message.HEAL === MESSAGE_HEAL;"));
			// Types des sous-classes d'entité.
			Assert.assertEquals(true, eval(sb, "Chest.Type.WOOD === CHEST_WOOD && Bulb.Type.PUNY === BULB_PUNY && Mob.Type.GRAAL === MOB_GRAAL;"));
			// Feature.type est une catégorie Effect (le "croisement" assumé).
			Assert.assertEquals(true, eval(sb, "var f = new Weapon(WEAPON_PISTOL).features; f.length === 0 || typeof f[0].type === 'number';"));
			// Dispatch : dans un combat leek vs leek, l'ennemi est une instance Leek (donc Entity).
			Assert.assertEquals(true, eval(sb, "var e = Fight.getNearestEnemy(); (e instanceof Leek) && (e instanceof Entity);"));
		}
	}

	/** Full POO côté PYTHON : hiérarchie Item, conteneurs de constantes, dispatch d'instances typées. */
	@Test
	public void objectConstantContainersPython() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance(Weapon(WEAPON_PISTOL), Item) and isinstance(Chip(CHIP_LIGHTNING), Item)"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "Effect.DAMAGE == EFFECT_DAMAGE and State.UNHEALABLE == STATE_UNHEALABLE"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "Entity.Stat.STRENGTH == STAT_STRENGTH and Entity.Type.LEEK == ENTITY_LEEK"));
			// Anti-pollution : Bulb.Type propre, ne contient pas LEEK (héritée d'Entity.Type).
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "Bulb.Type.PUNY == BULB_PUNY and not hasattr(Bulb.Type, 'LEEK')"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "Fight.Type.SOLO == FIGHT_TYPE_SOLO and Item.LaunchType.LINE == LAUNCH_TYPE_LINE and Field.NEXUS == MAP_NEXUS"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "Chest.Type.WOOD == CHEST_WOOD and Bulb.Type.PUNY == BULB_PUNY and Mob.Type.GRAAL == MOB_GRAAL"));
			// Dispatch : l'ennemi (leek) est une instance Leek (donc Entity).
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance(Fight.getNearestEnemy(), Leek) and isinstance(Fight.getNearestEnemy(), Entity)"));
		}
	}

	/**
	 * Caractéristiques DÉCLARÉES d'une arme/puce -> objets Feature ([type, minValue, maxValue, turns,
	 * targets, modifiers]) via la PROPERTY `features` (ex-méthode effects()). Statique (pas de combat),
	 * cohérent avec l'API plate getWeaponEffects/getChipEffects. JS + Python. (#3179)
	 */
	@Test
	public void featuresWrapWeaponAndChip() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			// JS : Weapon.features[i] est une Feature dont type/minValue/maxValue == tableau brut.
			Object js = eval(sb,
				"(function(){"
				+ "  var raw = getWeaponEffects(WEAPON_PISTOL);"
				+ "  var o = new Weapon(WEAPON_PISTOL).features;"  // property, pas d'appel
				+ "  if (o.length !== raw.length) return 'len';"
				+ "  if (o.length === 0) return 'empty';"
				+ "  if (!(o[0] instanceof Feature)) return 'class';"
				+ "  if (o[0].type !== raw[0][0]) return 'type';"
				+ "  if (o[0].minValue !== raw[0][1]) return 'min';"
				+ "  if (o[0].maxValue !== raw[0][2]) return 'max';"
				+ "  return 'ok:' + o.length;"
				+ "})();");
			Assert.assertTrue("Feature JS incohérent: " + js, String.valueOf(js).startsWith("ok:"));
			// Chip : même structure, property features.
			Assert.assertEquals(((Number) eval(sb, "getChipEffects(CHIP_LIGHTNING).length;")).longValue(),
				((Number) eval(sb, "new Chip(CHIP_LIGHTNING).features.length;")).longValue());
			// Python : Weapon(WEAPON_PISTOL).features[0].type == getWeaponEffects(WEAPON_PISTOL)[0][0].
			Assert.assertEquals(
				((Number) evalPy(sb, "getWeaponEffects(WEAPON_PISTOL)[0][0]")).longValue(),
				((Number) evalPy(sb, "Weapon(WEAPON_PISTOL).features[0].type")).longValue());
		}
	}

	/**
	 * Effets ACTIFS sur une entité -> objets Effect ([type, value, caster, turns, critical, item,
	 * target, modifiers]). Dans un vrai combat, l'IA s'auto-buffe (CHIP_PROTEIN sur elle-même) puis lit
	 * me.effects : le 1er effet doit être un Effect dont le caster est l'entité elle-même.
	 */
	@Test
	public void activeEffectsWrapAfterSelfBuff() throws Exception {
		String ai =
			"function turn() {"
			+ "  if (!getRegister('done')) {"
			+ "    useChip(CHIP_PROTEIN, getEntity());"
			+ "    var es = me.effects;"
			+ "    setRegister('count', '' + es.length);"
			+ "    if (es.length > 0) {"
			+ "      setRegister('isEffect', es[0] instanceof Effect ? '1' : '0');"
			+ "      setRegister('etype', '' + es[0].type);"
			+ "      setRegister('ecaster', '' + (es[0].caster == null ? -1 : es[0].caster.id));"
			+ "    }"
			+ "    setRegister('done', '1');"
			+ "  }"
			+ "  var e = Fight.getNearestEnemy();"
			+ "  if (e == null) return;"
			+ "  var ws = me.weapons; if (ws.length > 0) me.setWeapon(ws[0]);"
			+ "  me.moveToward(e);"
			+ "  while (me.canUseWeapon(e)) { if (me.useWeapon(e) <= 0) break; }"
			+ "}";
		attachJsAI(leek1, ai);
		attachJsAI(leek2, ai);
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		int count = Integer.parseInt(leek1.getRegister("count"));
		System.out.println("[objet-effet] leek1 me.effects.length apres auto-buff = " + count
			+ " type=" + leek1.getRegister("etype") + " caster=" + leek1.getRegister("ecaster"));
		Assert.assertTrue("l'auto-buff doit produire au moins un effet actif (count=" + count + ")", count >= 1);
		Assert.assertEquals("le 1er effet doit etre un objet Effect", "1", leek1.getRegister("isEffect"));
		Assert.assertEquals("le caster de l'effet d'auto-buff doit etre l'entite elle-meme",
			String.valueOf(leek1.getFId()), leek1.getRegister("ecaster"));
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

	private void attachPyAI(Leek leek, String code) {
		AIFile file = new AIFile("obj_" + System.nanoTime() + ".py", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	// ---- me.summon : le callback guest est rejoué par BulbAI à chaque tour du bulbe (mEntity de l'IA
	// invocatrice rebasculé sur le bulbe). Pendant ce tour, me / getEntity() doivent désigner le BULBE.
	// Verrou 1 = TypeMarshaller wrappe la fonction guest en FunctionLeekValue ; verrou 2 = me.id dynamique.
	// Les registres d'un summon sont redirigés vers son invocateur, donc leek1.getRegister voit le callback.

	@Test
	public void summonedBulbIsMeDuringItsTurn() throws Exception {
		String ai =
			"function turn() {"
			+ "  if (!getRegister('summoned')) {"
			+ "    setRegister('ownerId', '' + me.id);"
			+ "    var cb = function() {"
			+ "      setRegister('bulbMeId', '' + me.id);"
			+ "      setRegister('bulbGetEntity', '' + getEntity());"
			+ "      setRegister('bulbIsSummon', me.summoned ? '1' : '0');"
			+ "    };"
			+ "    var c = me.cell;"
			+ "    var cands = [Field.cellFromXY(c.x + 1, c.y), Field.cellFromXY(c.x - 1, c.y),"
			+ "                 Field.cellFromXY(c.x, c.y + 1), Field.cellFromXY(c.x, c.y - 1)];"
			+ "    var r = -99;"
			+ "    for (var i = 0; i < cands.length; i++) {"
			+ "      if (cands[i] != null && cands[i].empty) { r = me.summon(CHIP_PUNY_BULB, cands[i], cb); if (r > 0) break; }"
			+ "    }"
			+ "    setRegister('summonResult', '' + r);"
			+ "    setRegister('summoned', '1');"
			+ "  }"
			+ "}";
		attachJsAI(leek1, ai);
		attachJsAI(leek2, "function turn() {}");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		String summonResult = leek1.getRegister("summonResult");
		String ownerId = leek1.getRegister("ownerId");
		String bulbMeId = leek1.getRegister("bulbMeId");
		System.out.println("[summon-js] result=" + summonResult + " ownerId=" + ownerId + " bulbMeId=" + bulbMeId
			+ " bulbGetEntity=" + leek1.getRegister("bulbGetEntity") + " isSummon=" + leek1.getRegister("bulbIsSummon"));
		Assert.assertNotNull("me.summon doit reussir (verrou 1) et le callback du bulbe s'executer (resultat="
			+ summonResult + ")", bulbMeId);
		Assert.assertEquals("me.id pendant le tour du bulbe doit egaler getEntity()",
			leek1.getRegister("bulbGetEntity"), bulbMeId);
		Assert.assertNotEquals("me pendant le tour du bulbe doit etre le bulbe, pas l'invocateur (verrou 2)",
			ownerId, bulbMeId);
		Assert.assertEquals("me.summoned doit etre vrai pendant le tour du bulbe", "1", leek1.getRegister("bulbIsSummon"));
	}

	@Test
	public void summonedBulbIsMeDuringItsTurnPython() throws Exception {
		String ai =
			"def turn():\n"
			+ "    if not getRegister('summoned'):\n"
			+ "        setRegister('ownerId', str(me.id))\n"
			+ "        def cb():\n"
			+ "            setRegister('bulbMeId', str(me.id))\n"
			+ "            setRegister('bulbGetEntity', str(getEntity()))\n"
			+ "            setRegister('bulbIsSummon', '1' if me.summoned else '0')\n"
			+ "        c = me.cell\n"
			+ "        cands = [Field.cellFromXY(c.x + 1, c.y), Field.cellFromXY(c.x - 1, c.y), Field.cellFromXY(c.x, c.y + 1), Field.cellFromXY(c.x, c.y - 1)]\n"
			+ "        r = -99\n"
			+ "        for cand in cands:\n"
			+ "            if cand is not None and cand.empty:\n"
			+ "                r = me.summon(CHIP_PUNY_BULB, cand, cb)\n"
			+ "                if r > 0:\n"
			+ "                    break\n"
			+ "        setRegister('summonResult', str(r))\n"
			+ "        setRegister('summoned', '1')\n";
		attachPyAI(leek1, ai);
		attachPyAI(leek2, "def turn():\n    pass\n");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		String ownerId = leek1.getRegister("ownerId");
		String bulbMeId = leek1.getRegister("bulbMeId");
		System.out.println("[summon-py] result=" + leek1.getRegister("summonResult") + " ownerId=" + ownerId
			+ " bulbMeId=" + bulbMeId + " isSummon=" + leek1.getRegister("bulbIsSummon"));
		Assert.assertNotNull("me.summon Python doit reussir et le callback du bulbe s'executer", bulbMeId);
		Assert.assertEquals("me.id (Python) pendant le tour du bulbe doit egaler getEntity()",
			leek1.getRegister("bulbGetEntity"), bulbMeId);
		Assert.assertNotEquals("me (Python) pendant le tour du bulbe doit etre le bulbe, pas l'invocateur",
			ownerId, bulbMeId);
		Assert.assertEquals("me.summoned (Python) doit etre vrai pendant le tour du bulbe", "1",
			leek1.getRegister("bulbIsSummon"));
	}

	/**
	 * Une erreur dans le callback du bulbe est mappee en erreur d'IA (AI_INTERRUPTED via mapException),
	 * PAS en exception serveur, et n'empoisonne pas le contexte de l'invocateur : l'invocateur continue
	 * d'agir tour apres tour. Couvre le routage des exceptions guest par runGuestCallback.
	 */
	@Test
	public void summonCallbackErrorIsGracefulForOwner() throws Exception {
		String ai =
			"function turn() {"
			+ "  if (!getRegister('summoned')) {"
			+ "    var cb = function() { throw new Error('boum du bulbe'); };"
			+ "    var c = me.cell;"
			+ "    var cands = [Field.cellFromXY(c.x + 1, c.y), Field.cellFromXY(c.x - 1, c.y),"
			+ "                 Field.cellFromXY(c.x, c.y + 1), Field.cellFromXY(c.x, c.y - 1)];"
			+ "    for (var i = 0; i < cands.length; i++) {"
			+ "      if (cands[i] != null && cands[i].empty && me.summon(CHIP_PUNY_BULB, cands[i], cb) > 0) break;"
			+ "    }"
			+ "    setRegister('summoned', '1');"
			+ "  }"
			+ "  setRegister('ownerTurns', '' + (parseInt(getRegister('ownerTurns') || '0', 10) + 1));"
			+ "}";
		attachJsAI(leek1, ai);
		attachJsAI(leek2, "function turn() {}");
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		int ownerTurns = Integer.parseInt(leek1.getRegister("ownerTurns"));
		System.out.println("[summon-erreur] ownerTurns=" + ownerTurns);
		Assert.assertTrue("l'invocateur doit survivre a un callback de bulbe qui leve (ownerTurns=" + ownerTurns + ")",
			ownerTurns >= 2);
	}
}
