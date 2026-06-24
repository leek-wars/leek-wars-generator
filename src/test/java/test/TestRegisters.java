package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Registers;

/**
 * Plafond des registres : on doit pouvoir stocker exactement MAX_ENTRIES clés,
 * refuser toute clé supplémentaire, mais toujours pouvoir mettre à jour une clé
 * déjà présente (même au plafond). Sémantique alignée sur le site (LeekController).
 */
public class TestRegisters {

	@Test
	public void newKeyBeyondCapIsRejected() {
		Registers r = new Registers();
		for (int i = 0; i < Registers.MAX_ENTRIES; i++) {
			Assert.assertTrue("clé " + i + " doit être acceptée", r.set("k" + i, "" + i));
		}
		Assert.assertEquals(Registers.MAX_ENTRIES, r.getValues().size());
		// La 101e clé doit être refusée et ne rien écrire.
		Assert.assertFalse("une nouvelle clé au-delà du plafond doit être refusée", r.set("overflow", "x"));
		Assert.assertEquals(Registers.MAX_ENTRIES, r.getValues().size());
		Assert.assertNull(r.get("overflow"));
	}

	@Test
	public void existingKeyStaysUpdatableAtCap() {
		Registers r = new Registers();
		for (int i = 0; i < Registers.MAX_ENTRIES; i++) {
			r.set("k" + i, "" + i);
		}
		Assert.assertEquals(Registers.MAX_ENTRIES, r.getValues().size());
		// Au plafond, la mise à jour d'une clé existante reste autorisée.
		Assert.assertTrue("une clé existante doit rester modifiable au plafond", r.set("k0", "updated"));
		Assert.assertEquals("updated", r.get("k0"));
		Assert.assertEquals(Registers.MAX_ENTRIES, r.getValues().size());
	}

	@Test
	public void deleteFreesSlotForNewKey() {
		Registers r = new Registers();
		for (int i = 0; i < Registers.MAX_ENTRIES; i++) {
			r.set("k" + i, "" + i);
		}
		Assert.assertFalse(r.set("overflow", "x"));
		Assert.assertTrue(r.delete("k0"));
		Assert.assertTrue("après suppression, une nouvelle clé doit tenir", r.set("overflow", "x"));
		Assert.assertEquals(Registers.MAX_ENTRIES, r.getValues().size());
	}
}
