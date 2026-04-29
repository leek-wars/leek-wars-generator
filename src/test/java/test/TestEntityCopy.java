package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.weapons.Weapon;
import com.leekwars.generator.weapons.Weapons;

/**
 * Regression tests for Entity copy constructor isolation (#3610).
 *
 * The copy ctor used to share mWeapons / mChips / passiveEffects references
 * between source and copy. Mutations on either side (addWeapon, applyLoadout)
 * leaked across — most dangerously after the introduction of setLoadout()
 * hooks where applyLoadout() calls .clear() on these collections.
 */
public class TestEntityCopy extends FightTestBase {

	private Leek leek;

	@Override
	protected void createLeeks() {
		leek = defaultLeek(1, "L1");
		fight.getState().addEntity(0, leek);
	}

	private static Weapon registerWeapon(int id) {
		Weapon existing = Weapons.getWeapon(id);
		if (existing != null) return existing;
		Weapon w = new Weapon(
			id, 5, 1, 6,
			com.leekwars.generator.util.Json.createArray(),
			(byte) 1, (byte) 1, true, id, "test_w_" + id,
			com.leekwars.generator.util.Json.createArray(), 0, false);
		Weapons.addWeapon(w);
		return w;
	}

	@Test
	public void mWeaponsAreNotSharedWithCopy() throws Exception {
		Weapon w1 = registerWeapon(99001);
		Weapon w2 = registerWeapon(99002);
		leek.addWeapon(w1);
		Leek copy = new Leek(leek);
		// Mutate the original after copy; the clone must not see it.
		leek.addWeapon(w2);
		Assert.assertEquals("Original sees both weapons", 2, leek.getWeapons().size());
		Assert.assertEquals("Copy is isolated from later mutations", 1, copy.getWeapons().size());
	}

	@Test
	public void copyIsNotAffectedByOriginalApplyLoadoutClear() throws Exception {
		Weapon w1 = registerWeapon(99003);
		leek.addWeapon(w1);
		Leek copy = new Leek(leek);
		// applyLoadout({}) clears mWeapons — the most destructive case.
		leek.applyLoadout(
			new com.leekwars.generator.state.FightLoadout(
				"empty",
				new java.util.ArrayList<>(),
				new java.util.ArrayList<>(),
				new java.util.ArrayList<>(),
				new java.util.HashMap<>()),
			java.util.Collections.emptySet());
		Assert.assertEquals("Original was cleared", 0, leek.getWeapons().size());
		Assert.assertEquals("Copy still has its weapon", 1, copy.getWeapons().size());
	}

	@Test
	public void applyLoadoutOnOriginalDoesNotEmptyCopy() throws Exception {
		// Most direct repro of the original bug: clone, then clear via applyLoadout.
		Weapon w1 = registerWeapon(99006);
		Weapon w2 = registerWeapon(99007);
		leek.addWeapon(w1);
		leek.addWeapon(w2);
		Leek copy = new Leek(leek);
		leek.applyLoadout(
			new com.leekwars.generator.state.FightLoadout(
				"empty",
				new java.util.ArrayList<>(),
				new java.util.ArrayList<>(),
				new java.util.ArrayList<>(),
				new java.util.HashMap<>()),
			java.util.Collections.emptySet());
		Assert.assertEquals("Original cleared by applyLoadout", 0, leek.getWeapons().size());
		Assert.assertEquals("Copy retains both weapons", 2, copy.getWeapons().size());
	}
}
