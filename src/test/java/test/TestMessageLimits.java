package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.Entity;

/**
 * La boîte aux lettres d'une entité garde 200 messages d'alliés et 200 d'ennemis par tour, comptés à
 * part : un adversaire qui l'inonde ne fait plus tomber les messages de l'équipe, et sendTo renvoie
 * false pour un message refusé. Et la boîte d'un mob reste illisible pour les autres entités, avec
 * le getMessages des IA LS1-3 comme avec celui d'aujourd'hui.
 */
public class TestMessageLimits extends FightTestBase {

	private Leek ally;
	private Leek receiver;
	private Leek enemy;
	private Leek mob;

	@Override
	protected void createLeeks() {
		ally = defaultLeek(1, "Allie");
		receiver = defaultLeek(2, "Destinataire");
		enemy = defaultLeek(3, "Ennemi");
		mob = new Leek(defaultLeek(4, "Mob")) {
			@Override
			public int getType() {
				return Entity.TYPE_MOB;
			}
		};
		fight.getState().addEntity(0, ally);
		fight.getState().addEntity(0, receiver);
		fight.getState().addEntity(1, enemy);
		fight.getState().addEntity(1, mob);
	}

	private String registers(Leek leek) {
		var registers = registerStore.get(leek.getId());
		Assert.assertNotNull(leek.getName() + " n'a rien enregistré", registers);
		return registers;
	}

	/** Envoie 300 messages au poireau nommé au tour 1, et enregistre combien sendTo en a acceptés. */
	private static String flood(String target) {
		return "if (getTurn() == 1) {\n"
			+ "  var target = null\n"
			+ "  for (var e in getAliveEnemies()) if (getName(e) == '" + target + "') target = e\n"
			+ "  for (var e in getAliveAllies()) if (getName(e) == '" + target + "') target = e\n"
			+ "  var accepted = 0\n"
			+ "  for (var i = 0; i < 300; i++) if (sendTo(target, 1, i)) accepted++\n"
			+ "  setRegister('accepted', accepted)\n"
			+ "}";
	}

	@Test
	public void enemyFloodDoesNotCrowdOutAllyMessages() throws Exception {
		attachAI(ally, 4, flood("Destinataire"));
		attachAI(enemy, 4, flood("Destinataire"));
		attachAI(receiver, 4, "global allies = 0\n"
			+ "global enemies = 0\n"
			+ "for (var m in getMessages()) {\n"
			+ "  if (isAlly(m[0])) allies++ else enemies++\n"
			+ "}\n"
			+ "if (getTurn() == 3) setRegister('r', allies + ',' + enemies)");
		attachAI(mob, 4, "");
		runFight();

		Assert.assertTrue(registers(ally), registers(ally).contains("\"accepted\":\"200\""));
		Assert.assertTrue(registers(enemy), registers(enemy).contains("\"accepted\":\"200\""));
		Assert.assertTrue(registers(receiver), registers(receiver).contains("\"r\":\"200,200\""));
	}

	@Test
	public void legacyGetMessagesCannotReadAMobMailbox() throws Exception {
		// L'ennemi écrit chaque tour au mob, son allié, et au destinataire ; l'allié, en LS1, lit les deux boîtes
		attachAI(enemy, 4, "for (var e in getAliveEnemies()) if (getName(e) == 'Destinataire') sendTo(e, 1, 'x')\n"
			+ "for (var e in getAliveAllies()) if (getName(e) == 'Mob') sendTo(e, 1, 'x')");
		attachAI(ally, 1, "global mobMax = 0;\n"
			+ "global leekMax = 0;\n"
			+ "for (var e in getAliveEnemies()) {\n"
			+ "  if (getName(e) == \"Mob\") { mobMax = max(mobMax, count(getMessages(e))); }\n"
			+ "}\n"
			+ "for (var e in getAliveAllies()) {\n"
			+ "  if (getName(e) == \"Destinataire\") { leekMax = max(leekMax, count(getMessages(e))); }\n"
			+ "}\n"
			+ "setRegister(\"r\", mobMax + \",\" + leekMax);");
		attachAI(receiver, 4, "");
		attachAI(mob, 4, "");
		runFight();

		var r = registers(ally);
		Assert.assertTrue(r, r.contains("\"r\":\"0,"));
		Assert.assertFalse("la boîte du destinataire, elle, se lit : " + r, r.contains("\"r\":\"0,0\""));
	}
}
