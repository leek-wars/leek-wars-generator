package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;

/**
 * Fonctions de registres appelées depuis une IA, de bout en bout jusqu'au RegisterManager.
 */
public class TestRegisterFunctions extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "L1");
		leek2 = defaultLeek(2, "L2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/**
	 * 5pilow/leek-wars#2901 : deleteRegister appelé avant tout getRegister/setRegister du
	 * combat ne faisait rien (registres pas encore chargés), la clé survivait au combat.
	 */
	@Test
	public void deleteRegisterWithoutPriorAccess() throws Exception {
		registerStore.put(1, "{\"test\":\"1\",\"keep\":\"2\"}");
		attachAI(leek1, "deleteRegister('test');");
		attachAI(leek2, "");
		runFight();
		String saved = registerStore.get(1);
		Assert.assertFalse("la clé supprimée ne doit plus être enregistrée : " + saved, saved.contains("\"test\""));
		Assert.assertTrue("les autres clés restent : " + saved, saved.contains("\"keep\":\"2\""));
	}
}
