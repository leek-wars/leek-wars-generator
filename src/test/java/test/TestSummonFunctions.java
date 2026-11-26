package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.leek.Leek;

/**
 * Fonctions d'entité appelées depuis l'IA d'un bulbe, dans un vrai combat : la fermeture
 * confiée à summon() s'exécute sur l'IA de l'invocateur, mais les fonctions sans argument
 * doivent répondre pour le bulbe.
 */
public class TestSummonFunctions extends FightTestBase {

	private static final int CHIP_PUNY_BULB = FightConstants.CHIP_PUNY_BULB.getIntValue();

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		leek1 = summonerLeek(1, "A");
		leek2 = summonerLeek(2, "B");
		leek1.addChip(Chips.getChip(CHIP_PUNY_BULB));
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}


	/**
	 * 5pilow/leek-wars#3109 : getBirthTurn() sans argument, dans l'IA d'un bulbe, rend le tour
	 * d'invocation du bulbe, pas celui de l'invocateur.
	 * 5pilow/leek-wars#2888 : setRegister() depuis un bulbe écrit dans les registres de
	 * l'invocateur, sauvegardés en fin de combat.
	 */
	@Test
	public void bulbSeesItsOwnBirthTurnAndWritesSummonerRegisters() throws Exception {
		attachAI(leek1, summonBulbAtTurn(3, ""
			+ "setRegister('bulb_birth', '' + getBirthTurn());"
			+ "setRegister('bulb_is_summon', '' + isSummon());"
			+ "setRegister('bulb_summoner', '' + getSummoner());")
			+ "setRegister('leek_birth', '' + getBirthTurn());");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("l'invocation a réussi", "1", leek1.getRegister("summon_result"));
		Assert.assertEquals("le bulbe est né au tour 3", "3", leek1.getRegister("bulb_birth"));
		Assert.assertEquals("true", leek1.getRegister("bulb_is_summon"));
		Assert.assertEquals("" + leek1.getFId(), leek1.getRegister("bulb_summoner"));
		Assert.assertEquals("l'invocateur, lui, est né au tour 1", "1", leek1.getRegister("leek_birth"));
		String saved = registerStore.get(leek1.getId());
		Assert.assertNotNull(saved);
		Assert.assertTrue("registre écrit par le bulbe sauvegardé chez l'invocateur : " + saved, saved.contains("\"bulb_birth\":\"3\""));
	}

	/**
	 * listen() dans l'IA d'un bulbe rend ce que le bulbe a entendu depuis la fin de son tour
	 * précédent, et non la liste de l'invocateur, vidée juste avant lui à la fin de son tour.
	 */
	@Test
	public void bulbListensToItsOwnMessages() throws Exception {
		attachAI(leek1, summonBulbAtTurn(2, ""
			+ "if (getTurn() >= 3) setRegister('bulb_heard_' + getTurn(), '' + listen());")
			+ "if (getTurn() >= 3) setRegister('leek_heard_' + getTurn(), '' + listen());");
		attachAI(leek2, "say('hello ' + getTurn());");
		runFight();

		Assert.assertEquals("l'invocation a réussi", "1", leek1.getRegister("summon_result"));
		// Le bulbe joue juste après son invocateur : quel que soit le camp qui ouvre le tour, ils
		// ont entendu le même message de l'adversaire depuis la fin de leur tour précédent
		String heard = leek1.getRegister("leek_heard_3");
		Assert.assertTrue("l'invocateur a entendu l'adversaire : " + heard, heard != null && heard.contains("hello"));
		Assert.assertEquals(heard, leek1.getRegister("bulb_heard_3"));
	}
}
