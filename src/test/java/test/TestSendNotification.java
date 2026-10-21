package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.leek.Leek;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * sendNotification() : ce que le combat garde pour le worker, qui remet les notifications aux
 * éleveurs dans la limite de leur quota du jour.
 */
public class TestSendNotification extends FightTestBase {

	private static final int CHIP_PUNY_BULB = FightConstants.CHIP_PUNY_BULB.getIntValue();

	private Leek leek1, leek2;

	@Override
	protected void createLeeks() {
		// leek1 appartient à l'éleveur 7, leek2 à personne (comme un poireau de test)
		leek1 = new Leek(1, "A", 7, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek2 = new Leek(2, "B", 0, 150, 1200, 20, 6, 300, 100, 100, 100, 100, 0, 0, 8, 64, 0, false, 0, 0, "", 0, "", "", "", 0);
		leek1.addChip(Chips.getChip(CHIP_PUNY_BULB));
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	@Test
	public void notificationGoesToTheLeekFarmer() throws Exception {
		attachAI(leek1, "if (getTurn() == 1) setRegister('sent', '' + sendNotification('Victoire', 'au tour ' + getTurn()));");
		attachAI(leek2, "if (getTurn() == 1) setRegister('sent', '' + sendNotification('Personne', 'à prévenir'));");
		runFight();

		Assert.assertEquals("true", leek1.getRegister("sent"));
		Assert.assertEquals("un poireau sans éleveur n'envoie rien", "false", leek2.getRegister("sent"));
		Assert.assertEquals(1, fight.getNotifications().size());
		var n = fight.getNotifications().get(0);
		Assert.assertEquals(7, n.farmer);
		Assert.assertEquals(1, n.leek);
		Assert.assertEquals("Victoire", n.title);
		Assert.assertEquals("au tour 1", n.message);
	}

	@Test
	public void messageIsOptionalAndTitleRequired() throws Exception {
		attachAI(leek1, "if (getTurn() == 1) {"
			+ "  setRegister('alone', '' + sendNotification('Seul'));"
			+ "  setRegister('empty', '' + sendNotification('   ', 'sans titre'));"
			+ "  setRegister('null', '' + sendNotification('Nul', null));"
			+ "  setRegister('nullTitle', '' + sendNotification(null, 'sans titre'));"
			+ "}");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("true", leek1.getRegister("alone"));
		Assert.assertEquals("un titre vide est refusé", "false", leek1.getRegister("empty"));
		Assert.assertEquals("true", leek1.getRegister("null"));
		Assert.assertEquals("un titre null est refusé", "false", leek1.getRegister("nullTitle"));
		Assert.assertEquals(2, fight.getNotifications().size());
		Assert.assertEquals("", fight.getNotifications().get(0).message);
		Assert.assertEquals("", fight.getNotifications().get(1).message);
	}

	@Test
	public void textIsCleanedAndCutWithoutSplittingAnEmoji() throws Exception {
		attachAI(leek1, "if (getTurn() == 1) {"
			+ "  var title = ''; for (var i = 0; i < 49; i++) title += 'a';"
			+ "  var message = ''; for (var i = 0; i < 300; i++) message += 'é';"
			+ "  sendNotification(title + '😀😀', message);"
			+ "  sendNotification('a\\tb\\nc', 'demi ' + charAt('😀', 0));"
			+ "  var big = 'x'; for (var i = 0; i < 20; i++) big += big;"
			+ "  sendNotification(big, big);"
			+ "}");
		attachAI(leek2, "");
		runFight();

		var cut = fight.getNotifications().get(0);
		Assert.assertEquals(50, cut.title.codePointCount(0, cut.title.length()));
		Assert.assertTrue("l'emoji gardé est entier : " + cut.title, cut.title.endsWith("a😀"));
		Assert.assertEquals(200, cut.message.length());

		var cleaned = fight.getNotifications().get(1);
		Assert.assertEquals("tabulation et retour à la ligne deviennent des espaces", "a b c", cleaned.title);
		Assert.assertEquals("un demi-emoji devient « ? »", "demi ?", cleaned.message);

		var big = fight.getNotifications().get(2);
		Assert.assertEquals("un million de caractères, coupés", 50, big.title.length());
		Assert.assertEquals(200, big.message.length());
	}

	@Test
	public void atMostTheDailyQuotaPerFarmerAndFight() throws Exception {
		attachAI(leek1, "if (getTurn() == 1) {"
			+ "  var sent = 0;"
			+ "  for (var i = 0; i < 60; i++) if (sendNotification('n' + i)) sent++;"
			+ "  setRegister('sent', '' + sent);"
			+ "}");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("" + Fight.MAX_NOTIFICATIONS, leek1.getRegister("sent"));
		Assert.assertEquals(Fight.MAX_NOTIFICATIONS, fight.getNotifications().size());
		Assert.assertEquals("n49", fight.getNotifications().get(Fight.MAX_NOTIFICATIONS - 1).title);
	}

	@Test
	public void bulbNotifiesItsSummonerFarmer() throws Exception {
		attachAI(leek1, summonNextToMe("CHIP_PUNY_BULB", "getTurn() == 2",
			"function() { sendNotification('Bulbe', '' + isSummon()); }"));
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals("l'invocation a réussi", "1", leek1.getRegister("summon_result"));
		Assert.assertFalse(fight.getNotifications().isEmpty());
		for (var n : fight.getNotifications()) {
			Assert.assertEquals(7, n.farmer);
			Assert.assertEquals("au nom de son invocateur", 1, n.leek);
			Assert.assertEquals("true", n.message);
		}
	}

	/** L'exemple de la documentation. */
	@Test
	public void afterFightCanNotify() throws Exception {
		attachAI(leek1, "function afterFight() {\n\tsendNotification('Combat terminé', 'Tour ' + getTurn() + ', il me reste ' + getLife() + ' PV')\n}");
		attachAI(leek2, "");
		runFight();

		Assert.assertEquals(1, fight.getNotifications().size());
		Assert.assertEquals("Combat terminé", fight.getNotifications().get(0).title);
		Assert.assertTrue(fight.getNotifications().get(0).message, fight.getNotifications().get(0).message.matches("Tour \\d+, il me reste \\d+ PV"));
	}

	@Test
	public void javascriptAndPythonUseSystem() throws Exception {
		attachPolyglotAI(leek1, "js", "var me = Fight.me; function turn() { if (Fight.turn == 1) { System.sendNotification('JS', 'message'); System.sendNotification('JS seul'); } }");
		attachPolyglotAI(leek2, "js", "function turn() {}");
		runFight();

		Assert.assertEquals(2, fight.getNotifications().size());
		Assert.assertEquals("message", fight.getNotifications().get(0).message);
		Assert.assertEquals("", fight.getNotifications().get(1).message);

		setUp();
		attachPolyglotAI(leek1, "py", "def turn():\n    if Fight.turn == 1:\n        System.sendNotification('Python', 'message')\n        System.sendNotification('Python seul')\n");
		attachPolyglotAI(leek2, "py", "def turn():\n    pass\n");
		runFight();

		Assert.assertEquals(2, fight.getNotifications().size());
		Assert.assertEquals("Python", fight.getNotifications().get(0).title);
		Assert.assertEquals("", fight.getNotifications().get(1).message);
	}

	// Exemple de la page sendNotification de l'encyclopédie, dans les trois langages
	private static final String DOC_LEEKSCRIPT = ""
		+ "// Appelée automatiquement à la fin du combat\n"
		+ "function afterFight() {\n"
		+ "\t// Pas en combat de test : on garde le quota du jour pour les vrais combats\n"
		+ "\tif (getFightContext() != FIGHT_CONTEXT_TEST && isDead(getEntity())) {\n"
		+ "\t\tsendNotification('Défaite', 'Mort au tour ' + getTurn() + ', ' + getAliveEnemiesCount() + ' ennemi(s) encore debout')\n"
		+ "\t}\n"
		+ "}\n";
	private static final String DOC_JAVASCRIPT = ""
		+ "// Appelée automatiquement à la fin du combat\n"
		+ "function afterFight() {\n"
		+ "\t// Pas en combat de test : on garde le quota du jour pour les vrais combats\n"
		+ "\tif (Fight.context !== Fight.Context.TEST && Fight.me.dead) {\n"
		+ "\t\tSystem.sendNotification('Défaite', 'Mort au tour ' + Fight.turn + ', ' + Fight.getAliveEnemiesCount() + ' ennemi(s) encore debout')\n"
		+ "\t}\n"
		+ "}\n"
		+ "\n"
		+ "// En TypeScript comme en JavaScript, les hooks ne sont appelés que si l'IA définit turn()\n"
		+ "function turn() {\n"
		+ "\t// ...\n"
		+ "}\n";
	private static final String DOC_PYTHON = ""
		+ "# Appelée automatiquement à la fin du combat\n"
		+ "def afterFight():\n"
		+ "\t# Pas en combat de test : on garde le quota du jour pour les vrais combats\n"
		+ "\tif Fight.context != Fight.Context.TEST and Fight.me.dead:\n"
		+ "\t\tSystem.sendNotification('Défaite', 'Mort au tour ' + str(Fight.turn) + ', ' + str(Fight.getAliveEnemiesCount()) + ' ennemi(s) encore debout')\n"
		+ "\n"
		+ "# En Python, les hooks ne sont appelés que si l'IA définit turn()\n"
		+ "def turn():\n"
		+ "\tpass\n";

	/** leek1 tombe sous le pistolet de leek2, dans le contexte donné. */
	private void runDocExample(String extension, String code, int context) throws Exception {
		setUp();
		fight.getState().setContext(context);
		leek1.setLife(1);
		leek2.addWeapon(com.leekwars.generator.weapons.Weapons.getWeapon(FightConstants.WEAPON_PISTOL.getIntValue()));
		if (extension == null) attachAI(leek1, code);
		else attachPolyglotAI(leek1, extension, code);
		attachAI(leek2, "var e = getNearestEnemy(); setWeapon(WEAPON_PISTOL); moveToward(e); useWeapon(e); useWeapon(e);");
		runFight();
	}

	@Test
	public void encyclopediaExampleNotifiesADefeatOutsideTestFights() throws Exception {
		for (var example : new String[][] { { null, DOC_LEEKSCRIPT }, { "js", DOC_JAVASCRIPT }, { "py", DOC_PYTHON } }) {
			String language = example[0] == null ? "LeekScript" : example[0];
			runDocExample(example[0], example[1], Fight.CONTEXT_GARDEN);
			Assert.assertTrue(language + " : leek1 est mort", leek1.isDead());
			Assert.assertEquals(language, 1, fight.getNotifications().size());
			Assert.assertEquals(language, "Défaite", fight.getNotifications().get(0).title);
			Assert.assertTrue(language + " : " + fight.getNotifications().get(0).message,
				fight.getNotifications().get(0).message.matches("Mort au tour \\d+, 1 ennemi\\(s\\) encore debout"));

			runDocExample(example[0], example[1], Fight.CONTEXT_TEST);
			Assert.assertTrue(language + " : leek1 est mort", leek1.isDead());
			Assert.assertTrue(language + " : rien en combat de test", fight.getNotifications().isEmpty());
		}
	}

	private void attachPolyglotAI(Leek leek, String extension, String code) {
		attachAI(leek, new AIFile("notif_" + System.nanoTime() + "." + extension, code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false));
	}
}
