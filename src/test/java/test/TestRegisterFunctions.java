package test;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.RegisterManager;

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

	private final HashMap<Integer, Integer> fetches = new HashMap<>();
	private final HashMap<Integer, Boolean> savedAsNew = new HashMap<>();

	/** Remplace le RegisterManager de base par un qui compte les lectures et note le drapeau is_new. */
	private void spyRegisterManager() {
		fight.getState().setRegisterManager(new RegisterManager() {
			@Override public String getRegisters(int leek) {
				fetches.merge(leek, 1, Integer::sum);
				return registerStore.get(leek);
			}
			@Override public void saveRegisters(int leek, String registers, boolean is_new) {
				registerStore.put(leek, registers);
				savedAsNew.put(leek, is_new);
			}
		});
	}

	/**
	 * Un deleteRegister qui ne supprime rien, sur un poireau sans registres enregistrés,
	 * n'écrit rien en fin de combat (pas d'INSERT de '{}') et ne relit pas les registres à
	 * chaque appel.
	 */
	@Test
	public void deleteRegisterWithNothingStoredWritesNothing() throws Exception {
		spyRegisterManager();
		attachAI(leek1, "deleteRegister('a'); deleteRegister('b');");
		attachAI(leek2, "");
		runFight();
		Assert.assertFalse("rien d'enregistré : " + registerStore, registerStore.containsKey(1));
		Assert.assertEquals("une seule lecture des registres", Integer.valueOf(1), fetches.get(1));
	}

	/** Après un deleteRegister sans effet, un setRegister crée toujours les registres (INSERT). */
	@Test
	public void setRegisterAfterNoopDeleteIsInserted() throws Exception {
		spyRegisterManager();
		attachAI(leek1, "deleteRegister('a'); setRegister('b', '1');");
		attachAI(leek2, "");
		runFight();
		Assert.assertEquals("{\"b\":\"1\"}", registerStore.get(1));
		Assert.assertEquals("registres neufs : INSERT", Boolean.TRUE, savedAsNew.get(1));
		Assert.assertEquals("une seule lecture des registres", Integer.valueOf(1), fetches.get(1));
	}

	/** Un deleteRegister d'une clé absente de registres existants n'écrit rien non plus. */
	@Test
	public void deleteMissingKeyWritesNothing() throws Exception {
		spyRegisterManager();
		registerStore.put(1, "{\"keep\":\"2\"}");
		attachAI(leek1, "deleteRegister('a');");
		attachAI(leek2, "");
		runFight();
		Assert.assertFalse("aucune sauvegarde : " + savedAsNew, savedAsNew.containsKey(1));
	}
}
