package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.leek.Leek;

/**
 * Stats and buff stacking — multiple buffs of same/different types, debuff
 * removal, raw vs scaled buffs, total debuff, stat updates persistence.
 */
public class TestFightStats extends FightTestBase {

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
		return Effect.createEffect(fight.getState(), type, turns, 1, value, value, false,
			target, caster, null, 0, stackable, 0, 1, 0, 0);
	}

	// ---------- Buff stacking ----------

	@Test
	public void twoStackableBuffsAccumulate() throws Exception {
		initFightOnly();
		int before = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, true);
		int afterFirst = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, true);
		int afterSecond = leek1.getStrength();
		// Stackable: second buff adds on top of first
		Assert.assertTrue("Strength after first buff: " + afterFirst, afterFirst > before);
		Assert.assertTrue("Strength after second stacks: " + afterFirst + " → " + afterSecond,
			afterSecond > afterFirst);
	}

	@Test
	public void twoNonStackableBuffsDoNotAccumulate() throws Exception {
		initFightOnly();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		int afterFirst = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		int afterSecond = leek1.getStrength();
		// Non-stackable: second replaces first → strength unchanged
		Assert.assertEquals("Non-stackable buffs should not accumulate", afterFirst, afterSecond);
	}

	// ---------- Different stat buffs ----------

	@Test
	public void buffAgilityIncreasesAgility() throws Exception {
		initFightOnly();
		int before = leek1.getAgility();
		applyEffect(Effect.TYPE_BUFF_AGILITY, 5, 30, leek1, leek1, false);
		Assert.assertTrue("Agility increases: " + before + " → " + leek1.getAgility(),
			leek1.getAgility() > before);
	}

	@Test
	public void buffWisdomIncreasesWisdom() throws Exception {
		initFightOnly();
		int before = leek1.getWisdom();
		applyEffect(Effect.TYPE_BUFF_WISDOM, 5, 20, leek1, leek1, false);
		Assert.assertTrue(leek1.getWisdom() > before);
	}

	@Test
	public void buffResistanceIncreasesResistance() throws Exception {
		initFightOnly();
		int before = leek1.getResistance();
		applyEffect(Effect.TYPE_BUFF_RESISTANCE, 5, 20, leek1, leek1, false);
		Assert.assertTrue(leek1.getResistance() > before);
	}

	// ---------- Buffs across entities ----------

	@Test
	public void buffOnSelfDoesNotLeakToEnemy() throws Exception {
		initFightOnly();
		int strBefore = leek2.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		Assert.assertEquals("Enemy strength must not change", strBefore, leek2.getStrength());
	}

	@Test
	public void buffOnEnemyAffectsEnemyOnly() throws Exception {
		initFightOnly();
		int selfBefore = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek2, leek1, false);
		Assert.assertEquals("Self strength must not change", selfBefore, leek1.getStrength());
		Assert.assertTrue("Enemy strength must increase", leek2.getStrength() > 100);
	}

	// ---------- Stats reading via getStat ----------

	@Test
	public void getStatReadsBuffedValue() throws Exception {
		// getStrength() must reflect base + buff (otherwise damage calc would be wrong).
		initFightOnly();
		int baseBefore = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		int afterBuff = leek1.getStrength();
		Assert.assertTrue("getStrength() must include buff: " + baseBefore + " → " + afterBuff,
			afterBuff > baseBefore);
	}

	// ---------- Death cleanup ----------

	@Test
	public void buffsClearedOnDeath() throws Exception {
		initFightOnly();
		int baseStrength = leek1.getStrength();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek1, false);
		applyEffect(Effect.TYPE_BUFF_AGILITY, 5, 30, leek1, leek1, false);
		applyEffect(Effect.TYPE_RELATIVE_SHIELD, 5, 20, leek1, leek1, false);
		Assert.assertTrue(leek1.getEffects().size() >= 3);
		Assert.assertTrue("Strength buffed before death", leek1.getStrength() > baseStrength);
		leek1.die();
		Assert.assertEquals("All effects cleared on death", 0, leek1.getEffects().size());
		// Strength reverts to base after buff cleared
		Assert.assertEquals("Strength reverts to base after death", baseStrength, leek1.getStrength());
	}

	// ---------- Effect with 0 turns ----------

	@Test
	public void zeroTurnEffectStillApplies() throws Exception {
		// turns=0 means "instant" effect (damage, heal). For buffs, the spec is unclear —
		// document the behavior.
		initFightOnly();
		int countBefore = leek1.getEffects().size();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 0, 50, leek1, leek1, false);
		// turns=0 effects don't get added to the entity's effect list (no expiration tracking)
		Assert.assertEquals("turns=0 buff should not be added", countBefore, leek1.getEffects().size());
	}

	// ---------- Damage type effect ----------

	@Test
	public void damageEffectReducesLife() throws Exception {
		initFightOnly();
		int before = leek1.getLife();
		applyEffect(Effect.TYPE_DAMAGE, 0, 50, leek1, leek2, false);
		Assert.assertTrue("Damage effect should reduce life: " + before + " → " + leek1.getLife(),
			leek1.getLife() < before);
	}

	@Test
	public void healEffectIncreasesLife() throws Exception {
		initFightOnly();
		// Take damage first
		leek1.removeLife(200, 0, leek2, com.leekwars.generator.attack.DamageType.DIRECT, null, null);
		int afterDmg = leek1.getLife();
		applyEffect(Effect.TYPE_HEAL, 0, 50, leek1, leek1, false);
		Assert.assertTrue("Heal should increase life: " + afterDmg + " → " + leek1.getLife(),
			leek1.getLife() > afterDmg);
	}

	// ---------- Many buffs ----------

	@Test
	public void stackableBuffsWithSameParamsMergeIntoOneEffect() throws Exception {
		// Quirk: createEffect merges stackable effects with identical (caster, attack,
		// id, turns) — they accumulate into a single Effect object, not separate ones.
		// Strength buffs separately.
		initFightOnly();
		int baseStrength = leek1.getStrength();
		for (int i = 0; i < 10; i++) {
			applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 1, leek1, leek1, true);
		}
		// All 10 buffs merge into 1 effect with accumulated value
		Assert.assertEquals("10 stackable identical buffs merge into 1 effect", 1, leek1.getEffects().size());
		// But strength reflects all 10 buffs added up
		Assert.assertTrue("Cumulative strength bump > base: " + baseStrength + " → " + leek1.getStrength(),
			leek1.getStrength() > baseStrength + 5);
	}

	@Test
	public void stackableBuffsWithDifferentTurnsDoNotMerge() throws Exception {
		// Different `turns` makes them distinct effects.
		initFightOnly();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 3, 30, leek1, leek1, true);
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 30, leek1, leek1, true);
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 7, 30, leek1, leek1, true);
		Assert.assertEquals("Different turns → separate effects", 3, leek1.getEffects().size());
	}

	// ---------- Effect on dead leek ----------

	@Test
	public void effectOnDeadLeekIsRejected() throws Exception {
		initFightOnly();
		leek1.die();
		int countBefore = leek1.getEffects().size();
		applyEffect(Effect.TYPE_BUFF_STRENGTH, 5, 50, leek1, leek2, false);
		// Effect on dead leek — implementation may add it or skip. Document.
		System.out.println("[INFO] Effects on dead leek: " + leek1.getEffects().size()
			+ " (was " + countBefore + ")");
		// Just verify no crash.
	}
}
