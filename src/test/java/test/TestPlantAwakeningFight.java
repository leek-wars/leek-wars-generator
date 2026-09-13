package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.leek.Leek;

/**
 * Éveil des plantes, de bout en bout dans un vrai combat : l'IA du joueur invoque un
 * Piment à côté d'elle, la plante se réveille aussitôt — l'invocateur est déjà dans sa
 * zone — et la fonction confiée au summon() joue avec l'entité entrante en argument.
 *
 * Complète TestPlantAwakening, qui teste la mécanique sans lancer d'IA.
 */
public class TestPlantAwakeningFight extends FightTestBase {

	private static final int CHIP_CHILLI_PEPPER = 165;

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		// PT large : la puce d'invocation en coûte plusieurs, et on veut voir la plante
		// jouer dans la foulée.
		leek1 = new Leek(1, "A", 0, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "B", 0, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek1.addChip(Chips.getChip(CHIP_CHILLI_PEPPER));
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	@Test
	public void lInvocateurDansLaZoneReveilleSaPlanteEtLaFonctionRecoitSonId() throws Exception {
		// Code au premier niveau, rejoué à chaque tour : c'est ainsi qu'une IA Leek Wars
		// s'écrit, `global` est ce qui survit d'un tour à l'autre.
		attachAI(leek1, ""
			+ "global awakenings = 0;"
			+ "global planted = false;"
			+ "if (!planted) {"
			+ "  var x = getCellX(getCell());"
			+ "  var y = getCellY(getCell());"
			+ "  var candidates = [getCellFromXY(x + 1, y), getCellFromXY(x - 1, y), getCellFromXY(x, y + 1), getCellFromXY(x, y - 1)];"
			+ "  for (var i = 0; i < count(candidates); i++) {"
			+ "    var c = candidates[i];"
			+ "    if (c != null && isEmptyCell(c)) {"
			+ "      planted = true;"
			+ "      var r = summon(CHIP_CHILLI_PEPPER, c, function(e) {"
			+ "        awakenings = awakenings + 1;"
			+ "        setRegister('trigger', '' + e);"
			+ "        setRegister('same_as_getter', getPlantTrigger() == e ? 'yes' : 'no');"
			+ "        setRegister('awakenings', '' + awakenings);"
			+ "        setRegister('plant_tp', '' + getTP());"
			+ "      });"
			+ "      setRegister('summon_result', '' + r);"
			+ "      break;"
			+ "    }"
			+ "  }"
			+ "}");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("l'invocation a réussi", "1", leek1.getRegister("summon_result"));
		// La plante s'est réveillée au moins une fois, et c'est bien l'invocateur qui l'a
		// réveillée : il était dans sa zone au moment de la plantation.
		Assert.assertNotNull("la fonction de la plante a joué", leek1.getRegister("awakenings"));
		Assert.assertTrue(Integer.parseInt(leek1.getRegister("awakenings")) >= 1);
		Assert.assertEquals("" + leek1.getFId(), leek1.getRegister("trigger"));
		Assert.assertEquals("getPlantTrigger() rend la même entité", "yes", leek1.getRegister("same_as_getter"));
		// Les PT sont rendus à plein au réveil : la plante en a au moins de quoi lancer
		// sa petite puce.
		Assert.assertTrue("PT rendus au réveil", Integer.parseInt(leek1.getRegister("plant_tp")) >= 3);
	}
}
