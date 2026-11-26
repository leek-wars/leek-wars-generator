package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.leek.Leek;

/**
 * Opérations des invocations. La fermeture confiée à summon() s'exécute sur l'IA de
 * l'invocateur, avec SON compteur d'opérations : une entité et ses invocations partagent un
 * même budget par tour. Seul le réveil d'une plante, qui tombe dans le tour de quelqu'un
 * d'autre, part d'un compteur neuf (cf. TestPlantAwakeningFight).
 *
 * Ordre de jeu : une invocation joue juste après son invocateur, la plus récente d'abord.
 */
public class TestSummonOperations extends FightTestBase {

	private static final int CHIP_PUNY_BULB = FightConstants.CHIP_PUNY_BULB.getIntValue();
	private static final int CHIP_CHILLI_PEPPER = FightConstants.CHIP_CHILLI_PEPPER.getIntValue();

	/** Brûle quelques centaines de milliers d'opérations, loin du plafond d'un tour. */
	private static final String BURN = "var s = 0; for (var k = 0; k < 50000; k++) { s = s + k; }";

	/** Au moins ce que BURN dépense : la marge qui distingue « a brûlé » de « n'a rien fait ». */
	private static final int BURNED = 100000;

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		leek1 = new Leek(1, "A", 0, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "B", 0, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek1.addChip(Chips.getChip(CHIP_PUNY_BULB));
		leek1.addChip(Chips.getChip(CHIP_CHILLI_PEPPER));
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** Registre entier de l'invocateur (les invocations écrivent dans les siens). */
	private int register(String key) {
		String value = leek1.getRegister(key);
		Assert.assertNotNull("registre " + key + " écrit", value);
		return Integer.parseInt(value);
	}

	/** Le bulbe démarre son tour au compteur que son invocateur vient de laisser. */
	@Test
	public void bulbStartsFromTheCounterItsSummonerLeft() throws Exception {
		attachAI(leek1, summonNextToMe("CHIP_PUNY_BULB", "getTurn() == 1",
			"function() { setRegister('bulb_start_' + getTurn(), '' + getOperations()); }")
			+ BURN
			+ "setRegister('leek_end_' + getTurn(), '' + getOperations());");
		attachAI(leek2, "");
		runFight();

		int leekEnd = register("leek_end_3");
		Assert.assertTrue("l'invocateur a brûlé des opérations : " + leekEnd, leekEnd > BURNED);
		Assert.assertTrue("le bulbe poursuit le compteur de l'invocateur", register("bulb_start_3") >= leekEnd);
	}

	/** Deux bulbes du même invocateur : le second reprend le compteur là où le premier l'a laissé. */
	@Test
	public void bulbsOfOneSummonerShareOneBudget() throws Exception {
		attachAI(leek1, summonNextToMeWhenever("CHIP_PUNY_BULB", "getTurn() == 1 || getTurn() == 5", "function() {"
			+ " var born = getBirthTurn();"
			+ " setRegister('b' + born + '_start_' + getTurn(), '' + getOperations());"
			+ BURN
			+ " setRegister('b' + born + '_end_' + getTurn(), '' + getOperations());"
			+ "}")
			+ "setRegister('leek_end_' + getTurn(), '' + getOperations());");
		attachAI(leek2, "");
		runFight();

		// Tour 6 : l'invocateur, puis le bulbe né au tour 5, puis celui né au tour 1.
		int leekEnd = register("leek_end_6");
		int youngStart = register("b5_start_6"), youngEnd = register("b5_end_6");
		int oldStart = register("b1_start_6"), oldEnd = register("b1_end_6");
		Assert.assertTrue("le plus jeune bulbe part du compteur de l'invocateur", youngStart >= leekEnd);
		Assert.assertTrue("ce qu'il brûle s'ajoute au compteur", youngEnd > youngStart + BURNED);
		Assert.assertTrue("l'aîné part du compteur laissé par le plus jeune", oldStart >= youngEnd);
		Assert.assertTrue("et y ajoute ce qu'il brûle", oldEnd > oldStart + BURNED);
	}

	/** Ce que les bulbes dépensent ne déborde pas sur le tour suivant de l'invocateur. */
	@Test
	public void summonerStartsEachTurnWithAFreshCounter() throws Exception {
		attachAI(leek1, "setRegister('leek_start_' + getTurn(), '' + getOperations());"
			+ summonNextToMe("CHIP_PUNY_BULB", "getTurn() == 1", "function() {"
				+ BURN
				+ " setRegister('bulb_end_' + getTurn(), '' + getOperations());"
				+ "}"));
		attachAI(leek2, "");
		runFight();

		Assert.assertTrue("le bulbe a brûlé des opérations au tour 2", register("bulb_end_2") > BURNED);
		Assert.assertTrue("l'invocateur repart de zéro au tour 3", register("leek_start_3") < 1000);
	}

	/**
	 * Forum 12474 : un invocateur qui épuise son budget fait planter ses bulbes ce tour-là ;
	 * ils ne repartent pas d'un compteur neuf.
	 */
	@Test
	public void summonerOutOfOperationsLeavesNothingToItsBulbs() throws Exception {
		attachAI(leek1, summonNextToMe("CHIP_PUNY_BULB", "getTurn() == 1", "function() { setRegister('bulb_turn_' + getTurn(), 'played'); }")
			+ "if (getTurn() >= 3) { var n = 0; while (true) { n++; } }");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("l'invocation a réussi", "1", leek1.getRegister("summon_result"));
		Assert.assertEquals("au tour 2, l'invocateur a laissé du budget : le bulbe joue", "played", leek1.getRegister("bulb_turn_2"));
		Assert.assertNull("au tour 3, l'invocateur a tout consommé : le bulbe ne peut plus rien faire", leek1.getRegister("bulb_turn_3"));
	}

	/**
	 * Une plante réveillée dans le tour de son invocateur (il est dans sa zone dès la
	 * plantation) a son propre compteur, mais l'invocateur paye ensuite ce qu'elle a dépensé.
	 */
	@Test
	public void plantAwakenedInItsSummonersTurnIsPaidByTheSummoner() throws Exception {
		attachAI(leek1, "global awakenings = 0;"
			+ "setRegister('leek_before_' + getTurn(), '' + getOperations());"
			+ summonNextToMe("CHIP_CHILLI_PEPPER", "getTurn() == 1", "function(e) {"
				+ " awakenings = awakenings + 1;"
				+ " setRegister('plant_turn_' + awakenings, '' + getTurn());"
				+ BURN
				+ " setRegister('plant_spent_' + awakenings, '' + getOperations());"
				+ "}")
			+ "setRegister('leek_after_' + getTurn(), '' + getOperations());");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("la plantation a réussi", "1", leek1.getRegister("summon_result"));
		Assert.assertEquals("le premier réveil a lieu à la plantation, au tour 1", "1", leek1.getRegister("plant_turn_1"));
		int plantSpent = register("plant_spent_1");
		Assert.assertTrue("la plante a brûlé des opérations : " + plantSpent, plantSpent > BURNED);
		Assert.assertTrue("le réveil, dans le tour de l'invocateur, lui est facturé",
			register("leek_after_1") - register("leek_before_1") >= plantSpent);
	}
}
