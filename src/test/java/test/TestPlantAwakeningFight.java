package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.action.Action;
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

	/**
	 * IA qui plante un Piment sur une case libre voisine, une fois pour le combat, et lui
	 * confie `awakening` comme corps de fonction d'Éveil. `globals` est posé au premier
	 * niveau, avant la plantation.
	 */
	private static String summonChilliNextToMe(String globals, String awakening) {
		// Code au premier niveau, rejoué à chaque tour : c'est ainsi qu'une IA Leek Wars
		// s'écrit, `global` est ce qui survit d'un tour à l'autre.
		return globals
			+ "global planted = false;"
			+ "if (!planted) {"
			+ "  var x = getCellX(getCell());"
			+ "  var y = getCellY(getCell());"
			+ "  var candidates = [getCellFromXY(x + 1, y), getCellFromXY(x - 1, y), getCellFromXY(x, y + 1), getCellFromXY(x, y - 1)];"
			+ "  for (var i = 0; i < count(candidates); i++) {"
			+ "    var c = candidates[i];"
			+ "    if (c != null && isEmptyCell(c)) {"
			+ "      planted = true;"
			+ "      var r = summon(CHIP_CHILLI_PEPPER, c, function(e) {" + awakening + "});"
			+ "      setRegister('summon_result', '' + r);"
			+ "      break;"
			+ "    }"
			+ "  }"
			+ "}";
	}

	@Test
	public void lInvocateurDansLaZoneReveilleSaPlanteEtLaFonctionRecoitSonId() throws Exception {
		attachAI(leek1, summonChilliNextToMe("global awakenings = 0;", ""
			+ "awakenings = awakenings + 1;"
			+ "setRegister('trigger', '' + e);"
			+ "setRegister('same_as_getter', getPlantTrigger() == e ? 'yes' : 'no');"
			+ "setRegister('awakenings', '' + awakenings);"
			+ "setRegister('plant_tp', '' + getTP());"));
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

	/**
	 * getAwakeningZone() répond à la question « cette entité joue-t-elle un tour ? » :
	 * 0 pour tout le monde, le rayon de la zone pour une plante qui attend qu'on entre.
	 */
	@Test
	public void laZoneDEveilSeLitSurSoiCommeSurUneAutreEntite() throws Exception {
		attachAI(leek1, summonChilliNextToMe("", ""
			// Ici `me` est la plante : sa zone sur elle-même, et celle de l'entité entrante.
			+ "setRegister('plant_zone', '' + getAwakeningZone());"
			+ "setRegister('trigger_zone', '' + getAwakeningZone(e));"
			+ "setRegister('plant_zone_by_id', '' + getAwakeningZone(getEntity()));"));
		attachAI(leek2, "setRegister('my_zone', '' + getAwakeningZone());");
		runFight();

		Assert.assertEquals("l'invocation a réussi", "1", leek1.getRegister("summon_result"));
		Assert.assertEquals("le Piment a une zone de rayon 3", "3", leek1.getRegister("plant_zone"));
		Assert.assertEquals("la même zone, lue par id", "3", leek1.getRegister("plant_zone_by_id"));
		Assert.assertEquals("l'entité qui l'a réveillée, elle, joue son tour", "0", leek1.getRegister("trigger_zone"));
		Assert.assertEquals("un poireau n'a pas de zone", "0", leek2.getRegister("my_zone"));
	}

	/**
	 * Une plante se réveille dans le tour d'un autre : elle ne doit pas hériter du compteur
	 * d'opérations laissé par le dernier tour de son invocateur. La fermeture s'exécute sur
	 * l'IA de l'invocateur (cf. BulbAI) — sans remise à zéro, une IA qui consomme son budget
	 * tue sa propre plante d'une erreur « trop d'opérations » dès qu'un ennemi entre dans la
	 * zone.
	 */
	@Test
	public void leReveilNeRepartPasDuCompteurDOperationsDeLInvocateur() throws Exception {
		attachAI(leek1, summonChilliNextToMe("global awakenings = 0;", ""
			+ "awakenings = awakenings + 1;"
			+ "setRegister('ops' + awakenings, '' + getOperations());")
			// Puis l'invocateur brûle des opérations, à chaque tour : c'est ce compteur-là
			// que le réveil suivant, dans le tour de l'ennemi qui approche, ne doit pas voir.
			+ " var s = 0; for (var k = 0; k < 400000; k++) { s = s + k; }"
			+ " setRegister('burned', '' + getOperations());");
		attachAI(leek2, "moveToward(getNearestEnemy());");
		runFight();

		Assert.assertTrue("l'invocateur a bien brûlé des opérations",
			Integer.parseInt(leek1.getRegister("burned")) > 1000000);
		// Réveil 1 : à la plantation, dans le tour de l'invocateur. Réveil 2 : l'ennemi entre
		// dans la zone, dans SON tour, l'invocateur ayant fini le sien à 2 millions d'ops.
		Assert.assertNotNull("l'ennemi a fini par entrer dans la zone", leek1.getRegister("ops2"));
		Assert.assertTrue("le réveil compte ses propres opérations",
			Integer.parseInt(leek1.getRegister("ops2")) < 1000);
	}

	/**
	 * Le rapport doit dire qui agit pendant un réveil. SAY et USE_CHIP ne portent pas
	 * l'entité qui agit — le client la déduit du dernier LEEK_TURN — et un réveil tombe au
	 * milieu du tour de quelqu'un d'autre : sans la parenthèse PLANT_AWAKE / PLANT_ASLEEP,
	 * l'invocateur prononce les say() de sa plante et lance ses puces (#5088).
	 */
	@Test
	public void lesActionsDuReveilSontEncadreesParPlantAwakeEtPlantAsleep() throws Exception {
		attachAI(leek1, summonChilliNextToMe("", "say('plante');") + " say('poireau');");
		attachAI(leek2, "");
		runFight();

		var actions = fight.getState().getActions().toJSON().get("actions");
		int awake = -1, asleep = -1, plantSay = -1, leekSay = -1, plantTP = -1;
		for (int i = 0; i < actions.size(); ++i) {
			var action = actions.get(i);
			int type = action.get(0).asInt();
			if (type == Action.PLANT_AWAKE && awake == -1) {
				awake = i;
				plantTP = action.get(3).asInt();
			} else if (type == Action.PLANT_ASLEEP && asleep == -1) {
				asleep = i;
			} else if (type == Action.SAY) {
				if (action.get(1).asString().equals("plante") && plantSay == -1) plantSay = i;
				if (action.get(1).asString().equals("poireau") && leekSay == -1) leekSay = i;
			}
		}
		Assert.assertNotEquals("la plante s'est réveillée", -1, awake);
		Assert.assertNotEquals("le réveil est refermé", -1, asleep);
		Assert.assertNotEquals("la plante a parlé", -1, plantSay);
		Assert.assertNotEquals("le poireau a parlé", -1, leekSay);
		Assert.assertTrue("le say de la plante est dans la parenthèse", awake < plantSay && plantSay < asleep);
		Assert.assertTrue("le say du poireau est hors de la parenthèse", leekSay > asleep);
		// Les PT rendus voyagent avec l'action, sinon le client ferait descendre ceux de la
		// plante d'un réveil à l'autre sans jamais les remonter.
		Assert.assertTrue("les PT pleins de la plante sont dans l'action", plantTP >= 4);
	}
}
