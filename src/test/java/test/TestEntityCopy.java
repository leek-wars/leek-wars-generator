package test;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.weapons.Weapon;

/**
 * Regression tests for Entity copy constructor isolation (#3610).
 *
 * The copy ctor used to share mWeapons / mChips / passiveEffects references
 * between source and copy. Mutations on either side (addWeapon, applyLoadout)
 * leaked across, most dangerously after the introduction of setLoadout()
 * hooks where applyLoadout() calls .clear() on these collections.
 */
public class TestEntityCopy extends FightTestBase {

	private Leek leek;

	@Override
	protected void createLeeks() {
		leek = defaultLeek(1, "L1");
		fight.getState().addEntity(0, leek);
	}

	@Test
	public void mWeaponsAreNotSharedWithCopy() throws Exception {
		Weapon w1 = registerWeapon(99001, "test_w_99001", false);
		Weapon w2 = registerWeapon(99002, "test_w_99002", false);
		leek.addWeapon(w1);
		Leek copy = new Leek(leek);
		// Mutate the original after copy; the clone must not see it.
		leek.addWeapon(w2);
		Assert.assertEquals("Original sees both weapons", 2, leek.getWeapons().size());
		Assert.assertEquals("Copy is isolated from later mutations", 1, copy.getWeapons().size());
	}

	@Test
	public void applyLoadoutOnOriginalDoesNotEmptyCopy() throws Exception {
		// Most direct repro of the original bug: clone, then clear via applyLoadout.
		Weapon w1 = registerWeapon(99006, "test_w_99006", false);
		Weapon w2 = registerWeapon(99007, "test_w_99007", false);
		leek.addWeapon(w1);
		leek.addWeapon(w2);
		Leek copy = new Leek(leek);
		leek.applyLoadout(emptyLoadout("empty"), Collections.emptySet(), true);
		Assert.assertEquals("Original cleared by applyLoadout", 0, leek.getWeapons().size());
		Assert.assertEquals("Copy retains both weapons", 2, copy.getWeapons().size());
	}
}
