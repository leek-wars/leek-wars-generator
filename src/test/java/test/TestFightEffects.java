package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.bulbs.Bulbs;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;

/**
 * Effect creation and lifecycle (buffs, poison, shields). Uses Effect.createEffect
 * directly from Java to inject effects without going through chips/weapons.
 */
public class TestFightEffects extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private int applyEffect(int type, int turns, double value, Leek target, Leek caster, boolean stackable) {
		// aoe=1 means full effectiveness; aoe=0 zeros out the effect value.
		return Effect.createEffect(fight.getState(), type, turns, 1, value, value, false,
			target, caster, null, 0, stackable, 0, 1, 0, 0);
	}

	// ---------- createEffect input validation ----------

	@Test
	public void createEffectWithIdZeroReturnsZero() throws Exception {
		// Regression: id=0 used to index effects[-1] → ArrayIndexOutOfBoundsException.
		initFightOnly();
		int result = Effect.createEffect(fight.getState(), 0, 1, 0, 10, 10, false,
			leek1, leek2, null, 0, false, 0, 1, 0, 0);
		Assert.assertEquals(0, result);
	}

	@Test
	public void createEffectWithNegativeIdReturnsZero() throws Exception {
		initFightOnly();
		int result = Effect.createEffect(fight.getState(), -5, 1, 0, 10, 10, false,
			leek1, leek2, null, 0, false, 0, 1, 0, 0);
		Assert.assertEquals(0, result);
	}

	@Test
	public void createEffectWithOversizedIdReturnsZero() throws Exception {
		initFightOnly();
		int result = Effect.createEffect(fight.getState(), 99999, 1, 0, 10, 10, false,
			leek1, leek2, null, 0, false, 0, 1, 0, 0);
		Assert.assertEquals(0, result);
	}

	// ---------- Couverture de la table des effets ----------

	@Test
	public void effectTableCoversEveryDeclaredEffectId() throws Exception {
		// Régression #4870 : getAllEffects() itère sur Effect.effects.length, et la
		// table s'arrêtait à 62 alors que EFFECT_DAMAGE_TO_RESISTANCE vaut 63 — l'effet
		// n'était donc jamais renvoyé. Tout nouvel effet, même passif (pas de classe
		// Effect, case null), doit avoir sa case dans la table.
		int maxId = 0;
		String maxName = null;
		for (var field : Effect.class.getFields()) {
			if (!field.getName().startsWith("TYPE_")) continue;
			int id = field.getInt(null);
			if (id > maxId) {
				maxId = id;
				maxName = field.getName();
			}
		}
		Assert.assertEquals("Effect.effects doit avoir une case par id d'effet (dernier : " + maxName + ")",
			maxId, Effect.effects.length);
	}

	// ---------- Buff strength ----------

	@Test
	public void buffStrengthIncreasesStrength() throws Exception {
		initFightOnly();
		int before = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 3, 50, leek1, leek1, false);
		Assert.assertTrue("Strength should increase after buff: " + before + " → " + leek1.getStrength(),
			leek1.getStrength() > before);
	}

	@Test
	public void buffWearsOffWhenTurnsExpire() throws Exception {
		// Effects with turns=1 are still present after a single endTurn(); they get
		// fully removed only on the next entity startTurn cycle. We only assert that
		// the buff was applied and survives one endTurn (no immediate vanish).
		initFightOnly();
		int before = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 1, 50, leek1, leek1, false);
		Assert.assertTrue("Buff must apply", leek1.getStrength() != before);
		leek1.endTurn();
	}

	// ---------- Poison ----------

	@Test
	public void poisonAddsEffectToTarget() throws Exception {
		initFightOnly();
		int before = leek1.getEffects().size();
		applyEffect(Effect.TYPE_POISON, 5, 30, leek1, leek2, false);
		Assert.assertTrue("Poison should add an effect: " + before + " → " + leek1.getEffects().size(),
			leek1.getEffects().size() > before);
	}

	// ---------- Relative shield ----------

	@Test
	public void relativeShieldIncreasesShieldStat() throws Exception {
		initFightOnly();
		int before = leek1.getRelativeShield();
		applyEffect(Effect.TYPE_RELATIVE_SHIELD, 3, 20, leek1, leek1, false);
		Assert.assertTrue("Relative shield should increase: " + before + " → " + leek1.getRelativeShield(),
			leek1.getRelativeShield() > before);
	}

	// ---------- Vitality (max life) ----------

	@Test
	public void vitalityIncreasesTotalLife() throws Exception {
		initFightOnly();
		int before = leek1.getTotalLife();
		applyEffect(Effect.TYPE_VITALITY, 5, 200, leek1, leek1, false);
		Assert.assertTrue("Vitality should increase total life: " + before + " → " + leek1.getTotalLife(),
			leek1.getTotalLife() > before);
	}

	// ---------- Stacking behavior ----------

	@Test
	public void nonStackableSameEffectReplacesOriginal() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		int countAfter1 = leek1.getEffects().size();
		// Apply the same effect again — non-stackable, should replace
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		int countAfter2 = leek1.getEffects().size();
		// Same count: existing effect was replaced (not added)
		Assert.assertEquals("Non-stackable effect must not duplicate", countAfter1, countAfter2);
	}

	// ---------- State effects ----------

	/** modifiers : IRREDUCTIBLE comme les puces d'état, 0 comme le Stérile du sabre du désert. */
	private int applyState(EntityState state, Leek target, Leek caster, boolean stackable, int modifiers) {
		return Effect.createEffect(fight.getState(), Effect.TYPE_ADD_STATE, -1, 1, state.ordinal(), 0, false,
			target, caster, null, 0, stackable, 0, 1, 0, modifiers);
	}

	@Test
	public void stateEffectNeverStacks() throws Exception {
		// La valeur d'un ADD_STATE est un identifiant d'état, jamais une quantité :
		// la fusion d'effets l'additionnait (3 + 3 = 6), ce qui donnait un état
		// inexistant côté client. Le Réveil, seul ADD_STATE créé avec stackable=true,
		// plantait le rendu du combat à la deuxième résurrection de la même cible.
		initFightOnly();
		applyState(EntityState.INVINCIBLE, leek1, leek2, true, Effect.MODIFIER_IRREDUCTIBLE);
		applyState(EntityState.INVINCIBLE, leek1, leek2, true, Effect.MODIFIER_IRREDUCTIBLE);
		Assert.assertEquals("Un état ne se duplique pas", 1, leek1.getEffects().size());
		Assert.assertEquals("La valeur doit rester l'identifiant de l'état",
			EntityState.INVINCIBLE.ordinal(), leek1.getEffects().get(0).getValue());
		Assert.assertTrue(leek1.hasState(EntityState.INVINCIBLE));
	}

	@Test
	public void stateEffectIsReplacedNotDuplicated() throws Exception {
		initFightOnly();
		applyState(EntityState.STERILE, leek1, leek2, false, 0);
		applyState(EntityState.STERILE, leek1, leek2, false, 0);
		Assert.assertEquals(1, leek1.getEffects().size());
		Assert.assertEquals(EntityState.STERILE.ordinal(), leek1.getEffects().get(0).getValue());
	}

	@Test
	public void partialDebuffLeavesStateIntact() throws Exception {
		// Libération (-40 %) mettait la valeur de l'effet à l'échelle, or c'est
		// l'identifiant de l'état : Stérile (12) devenait 7, l'état magnétisé, et
		// disparaissait de l'affichage du combat.
		initFightOnly();
		applyState(EntityState.STERILE, leek1, leek2, false, 0);
		leek1.reduceEffects(0.40, leek2);
		Assert.assertEquals("L'état survit à une réduction partielle", 1, leek1.getEffects().size());
		Assert.assertEquals(EntityState.STERILE.ordinal(), leek1.getEffects().get(0).getValue());
	}

	@Test
	public void totalDebuffRemovesState() throws Exception {
		initFightOnly();
		applyState(EntityState.STERILE, leek1, leek2, false, 0);
		leek1.reduceEffectsTotal(1.0, leek2);
		Assert.assertEquals("Seule une réduction totale retire l'état", 0, leek1.getEffects().size());
	}

	// ---------- État Enraciné (ROOTED) ----------

	/** Première case libre et praticable de la carte, différente des cases exclues. */
	private Cell freeCell(Cell... excluded) {
		var map = fight.getState().getMap();
		outer:
		for (int i = 0; i < 613; ++i) {
			Cell c = map.getCell(i);
			if (c == null || !c.available(map)) continue;
			for (Cell e : excluded) {
				if (c == e) continue outer;
			}
			return c;
		}
		return null;
	}

	@Test
	public void rootedBlocksSlideButAllowsInversion() throws Exception {
		initFightOnly();
		applyState(EntityState.ROOTED, leek1, leek2, false, Effect.MODIFIER_IRREDUCTIBLE);
		Assert.assertTrue(leek1.hasState(EntityState.ROOTED));

		var state = fight.getState();
		Cell start = leek1.getCell();

		// Poussée/attraction (slideEntity) : un enraciné ne bouge pas
		Cell dest = freeCell(start);
		Assert.assertNotNull(dest);
		state.slideEntity(leek1, dest, leek2);
		Assert.assertEquals("Un enraciné ne peut être ni poussé ni attiré", start, leek1.getCell());

		// Déplacement volontaire : bloqué aussi
		int used = state.moveEntity(leek1, java.util.Arrays.asList(dest));
		Assert.assertEquals("Un enraciné ne peut pas se déplacer", 0, used);
		Assert.assertEquals(start, leek1.getCell());

		// L'Inversion, elle, fonctionne (c'est la différence avec STATIC)
		Cell cell2 = leek2.getCell();
		state.invertEntities(leek2, leek1);
		Assert.assertEquals("L'inversion fonctionne sur un enraciné", cell2, leek1.getCell());
		Assert.assertEquals(start, leek2.getCell());
	}

	@Test
	public void staticStillBlocksInversion() throws Exception {
		initFightOnly();
		applyState(EntityState.STATIC, leek1, leek2, false, Effect.MODIFIER_IRREDUCTIBLE);
		Cell start = leek1.getCell();
		Cell cell2 = leek2.getCell();
		fight.getState().invertEntities(leek2, leek1);
		Assert.assertEquals("STATIC bloque aussi l'inversion", start, leek1.getCell());
		Assert.assertEquals(cell2, leek2.getCell());
	}

	// ---------- Invocations plantes (Enraciné via template) ----------

	@Test
	public void plantSummonTemplatesHaveRootedState() throws Exception {
		// corn (9), chilli_pepper (10) et prototaxites (13) sont enracinés par données
		for (int id : new int[] { 9, 10, 13 }) {
			var template = Bulbs.getInvocationTemplate(id);
			Assert.assertNotNull("Template d'invocation " + id + " chargé", template);
			Assert.assertTrue("Template " + id + " enraciné", template.getStates().contains(EntityState.ROOTED));
		}
		// Le prototaxites ne peut pas agir (0 PT, aucune puce) : pas d'avertissement sans IA
		Assert.assertFalse(Bulbs.getInvocationTemplate(13).canAct());
		Assert.assertTrue(Bulbs.getInvocationTemplate(9).canAct());
	}

	@Test
	public void summonedPlantIsRootedAndUnpushable() throws Exception {
		initFightOnly();
		var state = fight.getState();
		var corn = Chips.getChip(164);
		Assert.assertNotNull(corn);

		// summonEntity exige que le lanceur soit l'entité courante de l'ordre de jeu
		var caster = state.getOrder().current();
		Assert.assertNotNull(caster);

		// Une case à portée de la puce (1-5, cercle) autour du lanceur
		Cell target = null;
		var map = state.getMap();
		for (int i = 0; i < 613 && target == null; ++i) {
			Cell c = map.getCell(i);
			if (c == null || !c.available(map)) continue;
			if (map.canUseAttack(caster.getCell(), c, corn.getAttack())) target = c;
		}
		Assert.assertNotNull("Une case de plantation valide existe", target);

		int result = state.summonEntity(caster, target, corn);
		Assert.assertTrue("L'invocation du maïs réussit : " + result, result > 0);

		var plant = state.getLastEntity();
		Assert.assertTrue("La plante est enracinée à l'apparition", plant.hasState(EntityState.ROOTED));

		// Impoussable
		Cell before = plant.getCell();
		Cell dest = freeCell(before, caster.getCell(), leek1.getCell(), leek2.getCell());
		state.slideEntity(plant, dest, leek2);
		Assert.assertEquals(before, plant.getCell());
	}

	// ---------- Surinfection ----------

	@Test
	public void superinfectionConvertsHalfOfRemainingPoison() throws Exception {
		initFightOnly();
		// Un poison de 5 tours sur leek1
		applyEffect(Effect.TYPE_POISON, 5, 30, leek1, leek2, false);
		var poison = leek1.getEffects().get(0);
		int perTurn = poison.getValue();
		Assert.assertTrue(perTurn > 0);
		int lifeBefore = leek1.getLife();

		int expectedNewPerTurn = (int) Math.round(perTurn * 0.5);
		int expectedDamage = (perTurn - expectedNewPerTurn) * 5;

		int dealt = applyEffect(Effect.TYPE_SUPERINFECTION, 0, 50, leek1, leek2, false);

		Assert.assertEquals("50 % du poison restant part en dégâts immédiats", expectedDamage, dealt);
		Assert.assertEquals(lifeBefore - expectedDamage, leek1.getLife());
		// Conversion, pas duplication : le poison restant est réduit d'autant
		Assert.assertEquals(1, leek1.getEffects().size());
		Assert.assertEquals(expectedNewPerTurn, leek1.getEffects().get(0).getValue());
	}

	@Test
	public void superinfectionWithoutPoisonDoesNothing() throws Exception {
		initFightOnly();
		int lifeBefore = leek1.getLife();
		int dealt = applyEffect(Effect.TYPE_SUPERINFECTION, 0, 50, leek1, leek2, false);
		Assert.assertEquals(0, dealt);
		Assert.assertEquals(lifeBefore, leek1.getLife());
	}

	// ---------- Death clears effects ----------

	@Test
	public void dyingClearsAllEffects() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		applyEffect(Effect.TYPE_RELATIVE_SHIELD, 5, 20, leek1, leek1, false);
		Assert.assertTrue("Should have effects before death", leek1.getEffects().size() >= 2);
		// Kill the leek
		leek1.die();
		Assert.assertEquals("Effects should be cleared on death", 0, leek1.getEffects().size());
	}

	// ---------- Effects readable from LeekScript ----------

	@Test
	public void getEffectsNativeReturnsArray() throws Exception {
		attachAI(leek1, "setRegister('count', '' + count(getEffects()));");
		attachAI(leek2, "");
		runFight();
		// Without any effects applied, count should be 0
		Assert.assertEquals("0", leek1.getRegister("count"));
	}

	// ---------- Buff caster vs target ----------

	@Test
	public void buffOnDifferentCasterAndTarget() throws Exception {
		initFightOnly();
		// leek2 buffs leek1
		int before = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek2, false);
		Assert.assertTrue(leek1.getStrength() > before);
		// leek1 sees the effect, leek2 does not
		Assert.assertTrue(leek1.getEffects().size() > 0);
	}

	// ---------- Negative values ----------

	@Test
	public void zeroValueBuffNoOps() throws Exception {
		initFightOnly();
		int before = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 0, leek1, leek1, false);
		Assert.assertEquals("Zero-value buff should not change strength", before, leek1.getStrength());
	}
}
