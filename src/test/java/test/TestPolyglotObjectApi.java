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
		// `me` n'est plus un global : on le lie depuis Fight.me (usage documenté) avant le snippet.
		PolyglotEntityAI ai = new PolyglotEntityAI("js", "var me = Fight.me; " + src, sb);
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
			// distance de me a l'ennemi : cohérente entre les deux chemins objet (cell.distance / Field).
			Object dObj = eval(sb, "me.cell.distance(Fight.getNearestEnemy());");
			Object dField = eval(sb, "Field.cellDistance(me.cell, Fight.getNearestEnemy().cell);");
			Assert.assertEquals(((Number) dField).longValue(), ((Number) dObj).longValue());
		}
	}

	private Object evalPy(PolyglotSandbox sb, String expr) throws Exception {
		// `me` n'est plus un global : on le lie depuis Fight.me (usage documenté) avant le snippet.
		PolyglotEntityAI ai = new PolyglotEntityAI("python", "me = Fight.me\ndef turn():\n    return " + expr + "\n", sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai.runIA();
	}

	/**
	 * #4540 : Debug.mark(Cell) plantait au combat avec « isinstance() arg 2 must be a type or tuple of
	 * types ». Cause : le prelude de facturation (PY_CHARGE_GUARD) remplacait builtins.list par une
	 * FONCTION -> `isinstance(x, list)` (dans objects.py _cidlist/_unwrap ET dans le code joueur) cassait.
	 * Fix : proxy a metaclasse (les types restent des types pour isinstance ET la construction reste
	 * facturee). On verifie les DEUX : semantique de type correcte + facturation conservee.
	 */
	@Test
	public void debugMarkIsinstanceAndBillingOnBuiltinTypes() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			// Code exact du rapport (une Cell + Color) : ne doit plus lever.
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "Debug.mark(Field.cellFromXY(17, 0), Color.BLUE, 1)"));
			// Variante liste, mentionnee par l'auteur.
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "Debug.mark([Field.cellFromXY(17, 0), Field.cellFromXY(18, 0)], Color.RED, 1)"));
			// isinstance / issubclass / fromkeys sur ces types (usage joueur courant) doivent marcher.
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance([], list)"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance({}, dict)"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance(set(), set)"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance((1,), tuple)"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance(frozenset(), frozenset)"));
			Assert.assertEquals(Boolean.FALSE, evalPy(sb, "isinstance([], dict)"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "dict.fromkeys([1, 2]) == {1: None, 2: None}"));
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "list(range(3)) == [0, 1, 2] and set([1, 1, 2]) == {1, 2}"));
			// FACTURATION CONSERVEE : construire une grande collection doit couter ~N ops (le proxy facture
			// via __call__). Un simple no-op coute une poignee d'ops -> le delta prouve le comptage.
			long ops = ((Number) evalPy(sb, "(list(range(50000)), System.operations)[1]")).longValue();
			Assert.assertTrue("list(range(50000)) doit etre facture (proxy), ops=" + ops, ops > 20000);
		}
	}

	@Test
	public void pythonObjectPropertiesReadState() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals((long) leek1.getLife(), ((Number) evalPy(sb, "me.life")).longValue());
			Assert.assertEquals((long) leek1.getStat(Entity.STAT_STRENGTH), ((Number) evalPy(sb, "me.strength")).longValue());
			Assert.assertEquals((long) leek2.getFId(), ((Number) evalPy(sb, "Fight.getNearestEnemy().id")).longValue());
			Object dObj = evalPy(sb, "me.cell.distance(Fight.getNearestEnemy())");
			Object dField = evalPy(sb, "Field.cellDistance(me.cell, Fight.getNearestEnemy().cell)");
			Assert.assertEquals(((Number) dField).longValue(), ((Number) dObj).longValue());
			// Objet Weapon en Python : stat cohérente avec le template hôte.
			int pistol = com.leekwars.generator.FightConstants.WEAPON_PISTOL.getIntValue();
			Assert.assertEquals((long) com.leekwars.generator.weapons.Weapons.getWeapon(pistol).getCost(),
				((Number) evalPy(sb, "Weapon.pistol.cost")).longValue());
		}
	}

	@Test
	public void weaponChipFieldObjects() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Weapon : stats statiques (pas besoin d'equiper l'arme), coherentes avec le template hote.
			int pistol = com.leekwars.generator.FightConstants.WEAPON_PISTOL.getIntValue();
			Assert.assertEquals((long) com.leekwars.generator.weapons.Weapons.getWeapon(pistol).getCost(),
				((Number) eval(sb, "new Weapon(" + pistol + ").cost;")).longValue());
			Assert.assertEquals(true, eval(sb, "Weapon.pistol.maxRange >= Weapon.pistol.minRange;"));
			Assert.assertEquals(true, eval(sb, "typeof Weapon.pistol.name === 'string' && Weapon.pistol.name.length > 0;"));
			// Chip : stats statiques.
			Assert.assertEquals(true, eval(sb, "typeof Chip.lightning.cost === 'number' && Chip.lightning.cost > 0;"));
			// Field : cellFromXY renvoie une Cell coherente (round-trip x/y -> id).
			Assert.assertEquals(true, eval(sb,
				"var c = Field.cellFromXY(5, 5); c === null || Field.cellFromXY(c.x, c.y).id === c.id;"));
			Assert.assertEquals(true, eval(sb, "typeof Field.type === 'number';"));
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
			// Weapon.pistol est une instance dont l'id vaut la constante hote WEAPON_PISTOL.
			Assert.assertEquals((long) com.leekwars.generator.FightConstants.WEAPON_PISTOL.getIntValue(),
				((Number) eval(sb, "Weapon.pistol.id;")).longValue());
			// Membres riches accessibles directement (coherents avec le template hote).
			int pistol = com.leekwars.generator.FightConstants.WEAPON_PISTOL.getIntValue();
			Assert.assertEquals((long) com.leekwars.generator.weapons.Weapons.getWeapon(pistol).getCost(),
				((Number) eval(sb, "Weapon.pistol.cost;")).longValue());
			// camelCase depuis SNAKE_CASE : WEAPON_MACHINE_GUN -> Weapon.machineGun.
			Assert.assertEquals((long) com.leekwars.generator.FightConstants.WEAPON_MACHINE_GUN.getIntValue(),
				((Number) eval(sb, "Weapon.machineGun.id;")).longValue());
			// Chip pareil.
			Assert.assertEquals((long) com.leekwars.generator.FightConstants.CHIP_LIGHTNING.getIntValue(),
				((Number) eval(sb, "Chip.lightning.id;")).longValue());
			// IDENTITE DE POOL : deux acces a la meme constante = MEME objet (comparable par ===).
			Assert.assertEquals(true, eval(sb, "Weapon.pistol === Weapon.pistol;"));
			// ... et l'arme equipee (via weap()) est le meme singleton que la constante objet.
			Assert.assertEquals(true, eval(sb,
				"me.setWeapon(Weapon.pistol); me.weapon === Weapon.pistol;"));
			// Ergonomie : passer la constante objet a une action l'unwrap (wid) comme un id plat.
			Assert.assertEquals(true, eval(sb,
				"me.setWeapon(Weapon.pistol); me.weapon.name === Weapon.pistol.name;"));
		}
	}

	/** Constantes objet Weapon.pistol aussi en PYTHON : instance poolee, identite avec l'arme equipee. */
	@Test
	public void weaponChipObjectConstantsPython() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals((long) com.leekwars.generator.FightConstants.WEAPON_PISTOL.getIntValue(),
				((Number) evalPy(sb, "Weapon.pistol.id")).longValue());
			int pistol = com.leekwars.generator.FightConstants.WEAPON_PISTOL.getIntValue();
			Assert.assertEquals((long) com.leekwars.generator.weapons.Weapons.getWeapon(pistol).getCost(),
				((Number) evalPy(sb, "Weapon.pistol.cost")).longValue());
			// camelCase identique au runtime JS (WEAPON_MACHINE_GUN -> machineGun, CHIP_FIRE_BALL -> fireBall).
			Assert.assertEquals((long) com.leekwars.generator.FightConstants.WEAPON_MACHINE_GUN.getIntValue(),
				((Number) evalPy(sb, "Weapon.machineGun.id")).longValue());
			Assert.assertEquals((long) com.leekwars.generator.FightConstants.CHIP_FIRE_BALL.getIntValue(),
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
			// summoned : un poireau de depart n'est pas une invocation.
			Assert.assertEquals(false, eval(sb, "me.summoned;"));
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
			// cell.content : ma propre case contient un joueur (Cell.Type.PLAYER).
			Assert.assertEquals(true, eval(sb, "me.cell.content === Cell.Type.PLAYER;"));
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

	/** Valeur hôte d'une constante de combat (source de vérité, les globales plates n'existant plus). */
	private static long fc(String name) {
		return com.leekwars.generator.FightConstants.valueOf(name).getIntValue();
	}

	/**
	 * Full POO : hiérarchie Item (Weapon/Chip extends Item), conteneurs de constantes par famille
	 * (Effect.SHIELD, State.PACIFIST, Entity.Stat.STRENGTH, Fight.Type.SOLO, Item.LaunchType.LINE,
	 * Field.NEXUS, Chest.Type.WOOD...) et dispatch d'instances typées (getNearestEnemy -> Leek). Les
	 * valeurs objet doivent coïncider avec les constantes HÔTE (les globales plates n'existent plus). (#3179)
	 */
	@Test
	public void objectConstantContainersAndHierarchy() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			// Hiérarchie Item : une arme/puce EST un Item.
			Assert.assertEquals(true, eval(sb, "(Weapon.pistol instanceof Item) && (Chip.lightning instanceof Item);"));
			// Conteneurs de catégories == constantes hôte (MAJUSCULES, valeur identique).
			Assert.assertEquals(fc("EFFECT_ABSOLUTE_SHIELD"), ((Number) eval(sb, "Effect.ABSOLUTE_SHIELD;")).longValue());
			Assert.assertEquals(fc("EFFECT_DAMAGE"), ((Number) eval(sb, "Effect.DAMAGE;")).longValue());
			Assert.assertEquals(fc("STATE_UNHEALABLE"), ((Number) eval(sb, "State.UNHEALABLE;")).longValue());
			Assert.assertEquals(fc("STAT_STRENGTH"), ((Number) eval(sb, "Entity.Stat.STRENGTH;")).longValue());
			Assert.assertEquals(fc("ENTITY_LEEK"), ((Number) eval(sb, "Entity.Type.LEEK;")).longValue());
			// Anti-pollution : Bulb.Type (propre) ne doit PAS hériter Entity.Type -> pas de LEEK dessus.
			Assert.assertEquals(fc("BULB_PUNY"), ((Number) eval(sb, "Bulb.Type.PUNY;")).longValue());
			Assert.assertEquals(true, eval(sb, "Bulb.Type.LEEK === undefined;"));
			Assert.assertEquals((long) leekscript.runner.LeekConstants.CELL_EMPTY.getIntValue(),
				((Number) eval(sb, "Cell.Type.EMPTY;")).longValue());
			Assert.assertEquals(fc("LAUNCH_TYPE_LINE"), ((Number) eval(sb, "Item.LaunchType.LINE;")).longValue());
			Assert.assertEquals(fc("AREA_CIRCLE_1"), ((Number) eval(sb, "Item.Area.CIRCLE_1;")).longValue());
			Assert.assertEquals(fc("MAP_NEXUS"), ((Number) eval(sb, "Field.NEXUS;")).longValue());
			// Sous-conteneurs de Fight.
			Assert.assertEquals(fc("FIGHT_TYPE_SOLO"), ((Number) eval(sb, "Fight.Type.SOLO;")).longValue());
			Assert.assertEquals(fc("FIGHT_CONTEXT_GARDEN"), ((Number) eval(sb, "Fight.Context.GARDEN;")).longValue());
			Assert.assertEquals(fc("BOSS_FENNEL_KING"), ((Number) eval(sb, "Fight.Boss.FENNEL_KING;")).longValue());
			Assert.assertEquals(fc("EROSION_DAMAGE"), ((Number) eval(sb, "Fight.Erosion.DAMAGE;")).longValue());
			Assert.assertEquals(fc("USE_SUCCESS"), ((Number) eval(sb, "Fight.Use.SUCCESS;")).longValue());
			// Les MESSAGE_* vivent sur la classe Message (cf Network.getMessages).
			Assert.assertEquals(fc("MESSAGE_HEAL"), ((Number) eval(sb, "Message.Type.HEAL;")).longValue());
			// Types des sous-classes d'entité.
			Assert.assertEquals(fc("CHEST_WOOD"), ((Number) eval(sb, "Chest.Type.WOOD;")).longValue());
			Assert.assertEquals(fc("MOB_GRAAL"), ((Number) eval(sb, "Mob.Type.GRAAL;")).longValue());
			// Feature.type est une catégorie Effect (le "croisement" assumé).
			Assert.assertEquals(true, eval(sb, "var f = Weapon.pistol.features; f.length === 0 || typeof f[0].type === 'number';"));
			// Dispatch : dans un combat leek vs leek, l'ennemi est une instance Leek (donc Entity).
			Assert.assertEquals(true, eval(sb, "var e = Fight.getNearestEnemy(); (e instanceof Leek) && (e instanceof Entity);"));
		}
	}

	/** Full POO côté PYTHON : hiérarchie Item, conteneurs de constantes, dispatch d'instances typées. */
	@Test
	public void objectConstantContainersPython() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js", "python")) {
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "isinstance(Weapon.pistol, Item) and isinstance(Chip.lightning, Item)"));
			Assert.assertEquals(fc("EFFECT_DAMAGE"), ((Number) evalPy(sb, "Effect.DAMAGE")).longValue());
			Assert.assertEquals(fc("STATE_UNHEALABLE"), ((Number) evalPy(sb, "State.UNHEALABLE")).longValue());
			Assert.assertEquals(fc("STAT_STRENGTH"), ((Number) evalPy(sb, "Entity.Stat.STRENGTH")).longValue());
			Assert.assertEquals(fc("ENTITY_LEEK"), ((Number) evalPy(sb, "Entity.Type.LEEK")).longValue());
			// Anti-pollution : Bulb.Type propre, ne contient pas LEEK (héritée d'Entity.Type).
			Assert.assertEquals(fc("BULB_PUNY"), ((Number) evalPy(sb, "Bulb.Type.PUNY")).longValue());
			Assert.assertEquals(Boolean.TRUE, evalPy(sb, "not hasattr(Bulb.Type, 'LEEK')"));
			Assert.assertEquals(fc("FIGHT_TYPE_SOLO"), ((Number) evalPy(sb, "Fight.Type.SOLO")).longValue());
			Assert.assertEquals(fc("LAUNCH_TYPE_LINE"), ((Number) evalPy(sb, "Item.LaunchType.LINE")).longValue());
			Assert.assertEquals(fc("MAP_NEXUS"), ((Number) evalPy(sb, "Field.NEXUS")).longValue());
			Assert.assertEquals(fc("CHEST_WOOD"), ((Number) evalPy(sb, "Chest.Type.WOOD")).longValue());
			Assert.assertEquals(fc("MOB_GRAAL"), ((Number) evalPy(sb, "Mob.Type.GRAAL")).longValue());
			Assert.assertEquals(fc("MESSAGE_HEAL"), ((Number) evalPy(sb, "Message.Type.HEAL")).longValue());
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
			// JS : Weapon.features[i] est une Feature dont type/minValue/maxValue == son tableau brut (.raw).
			Object js = eval(sb,
				"(function(){"
				+ "  var o = Weapon.pistol.features;"  // property, pas d'appel
				+ "  if (o.length === 0) return 'empty';"
				+ "  if (!(o[0] instanceof Feature)) return 'class';"
				+ "  if (o[0].type !== o[0].raw[0]) return 'type';"
				+ "  if (o[0].minValue !== o[0].raw[1]) return 'min';"
				+ "  if (o[0].maxValue !== o[0].raw[2]) return 'max';"
				+ "  return 'ok:' + o.length;"
				+ "})();");
			Assert.assertTrue("Feature JS incohérent: " + js, String.valueOf(js).startsWith("ok:"));
			// Chip : même structure, property features (un sort de dégâts déclare au moins une Feature).
			Assert.assertEquals(true, eval(sb, "Chip.lightning.features.length > 0;"));
			// Python : même vue nommée du tableau brut.
			Assert.assertEquals(Boolean.TRUE,
				evalPy(sb, "Weapon.pistol.features[0].type == Weapon.pistol.features[0].raw[0]"));
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
			+ "  if (!Registers.get('done')) {"
			+ "    me.useChip(Chip.protein, me);"
			+ "    var es = me.effects;"
			+ "    Registers.set('count', '' + es.length);"
			+ "    if (es.length > 0) {"
			+ "      Registers.set('isEffect', es[0] instanceof Effect ? '1' : '0');"
			+ "      Registers.set('etype', '' + es[0].type);"
			+ "      Registers.set('ecaster', '' + (es[0].caster == null ? -1 : es[0].caster.id));"
			+ "    }"
			+ "    Registers.set('done', '1');"
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
		// `me` n'est plus un global : on le lie depuis Fight.me (usage documenté), comme eval().
		AIFile file = new AIFile("obj_" + System.nanoTime() + ".js", "var me = Fight.me; " + code,
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
			+ "  if (Registers.get('start') == null) Registers.set('start', '' + me.cell.distance(enemy));"
			+ "  me.moveToward(enemy);"
			+ "  Registers.set('end', '' + me.cell.distance(enemy));"
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
		// `me` n'est plus un global : on le lie depuis Fight.me (usage documenté), comme evalPy().
		AIFile file = new AIFile("obj_" + System.nanoTime() + ".py", "me = Fight.me\n" + code,
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
			+ "  if (!Registers.get('summoned')) {"
			+ "    Registers.set('ownerId', '' + me.id);"
			+ "    var cb = function() {"
			+ "      Registers.set('bulbMeId', '' + me.id);"
			+ "      Registers.set('bulbGetEntity', '' + Fight.me.id);"
			+ "      Registers.set('bulbIsSummon', me.summoned ? '1' : '0');"
			+ "    };"
			+ "    var c = me.cell;"
			+ "    var cands = [Field.cellFromXY(c.x + 1, c.y), Field.cellFromXY(c.x - 1, c.y),"
			+ "                 Field.cellFromXY(c.x, c.y + 1), Field.cellFromXY(c.x, c.y - 1)];"
			+ "    var r = -99;"
			+ "    for (var i = 0; i < cands.length; i++) {"
			+ "      if (cands[i] != null && cands[i].empty) { r = me.summon(Chip.punyBulb, cands[i], cb); if (r > 0) break; }"
			+ "    }"
			+ "    Registers.set('summonResult', '' + r);"
			+ "    Registers.set('summoned', '1');"
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
			+ "    if not Registers.get('summoned'):\n"
			+ "        Registers.set('ownerId', str(me.id))\n"
			+ "        def cb():\n"
			+ "            Registers.set('bulbMeId', str(me.id))\n"
			+ "            Registers.set('bulbGetEntity', str(Fight.me.id))\n"
			+ "            Registers.set('bulbIsSummon', '1' if me.summoned else '0')\n"
			+ "        c = me.cell\n"
			+ "        cands = [Field.cellFromXY(c.x + 1, c.y), Field.cellFromXY(c.x - 1, c.y), Field.cellFromXY(c.x, c.y + 1), Field.cellFromXY(c.x, c.y - 1)]\n"
			+ "        r = -99\n"
			+ "        for cand in cands:\n"
			+ "            if cand is not None and cand.empty:\n"
			+ "                r = me.summon(Chip.punyBulb, cand, cb)\n"
			+ "                if r > 0:\n"
			+ "                    break\n"
			+ "        Registers.set('summonResult', str(r))\n"
			+ "        Registers.set('summoned', '1')\n";
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
			+ "  if (!Registers.get('summoned')) {"
			+ "    var cb = function() { throw new Error('boum du bulbe'); };"
			+ "    var c = me.cell;"
			+ "    var cands = [Field.cellFromXY(c.x + 1, c.y), Field.cellFromXY(c.x - 1, c.y),"
			+ "                 Field.cellFromXY(c.x, c.y + 1), Field.cellFromXY(c.x, c.y - 1)];"
			+ "    for (var i = 0; i < cands.length; i++) {"
			+ "      if (cands[i] != null && cands[i].empty && me.summon(Chip.punyBulb, cands[i], cb) > 0) break;"
			+ "    }"
			+ "    Registers.set('summoned', '1');"
			+ "  }"
			+ "  Registers.set('ownerTurns', '' + (parseInt(Registers.get('ownerTurns') || '0', 10) + 1));"
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
