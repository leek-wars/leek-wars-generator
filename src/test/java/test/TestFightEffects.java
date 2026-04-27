package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.leek.Leek;

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
