package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.leek.Leek;

/**
 * Fonctions d'entité appelées depuis l'IA d'un bulbe, dans un vrai combat : la fermeture
 * confiée à summon() s'exécute sur l'IA de l'invocateur, mais les fonctions sans argument
 * doivent répondre pour le bulbe.
 */
public class TestSummonFunctions extends FightTestBase {

	private static final int CHIP_PUNY_BULB = 73;

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		leek1 = new Leek(1, "A", 0, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "B", 0, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek1.addChip(Chips.getChip(CHIP_PUNY_BULB));
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	/** Invoque un bulbe chétif sur une case libre voisine au tour `turn`, avec `body` pour IA. */
	private static String summonBulbAtTurn(int turn, String body) {
		return "global summoned = false;"
			+ "if (!summoned && getTurn() == " + turn + ") {"
			+ "  var x = getCellX(getCell());"
			+ "  var y = getCellY(getCell());"
			+ "  var candidates = [getCellFromXY(x + 1, y), getCellFromXY(x - 1, y), getCellFromXY(x, y + 1), getCellFromXY(x, y - 1)];"
			+ "  for (var i = 0; i < count(candidates); i++) {"
			+ "    var c = candidates[i];"
			+ "    if (c != null && isEmptyCell(c)) {"
			+ "      summoned = true;"
			+ "      setRegister('summon_result', '' + summon(CHIP_PUNY_BULB, c, function() {" + body + "}));"
			+ "      break;"
			+ "    }"
			+ "  }"
			+ "}";
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
}
