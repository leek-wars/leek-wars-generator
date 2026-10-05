package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;

import leekscript.compiler.AIFile;

/**
 * Un message passe d'une IA à l'autre, et les deux peuvent tourner dans des versions
 * différentes de LeekScript : le destinataire reçoit des tableaux de SA version, que ses
 * fonctions acceptent (count() refusait un tableau LS1 dans une IA LS4).
 */
public class TestMessagesAcrossVersions extends FightTestBase {

	private Leek sender;
	private Leek receiver;

	@Override
	protected void createLeeks() {
		sender = defaultLeek(1, "Expediteur");
		receiver = defaultLeek(2, "Destinataire");
		fight.getState().addEntity(0, sender);
		fight.getState().addEntity(1, receiver);
	}

	private void attach(Leek leek, int version, String code) {
		attachAI(leek, new AIFile("<messages_v" + version + "_" + leek.getId() + "_" + System.nanoTime() + ">", code,
			System.currentTimeMillis(), version, leek.getId(), false));
	}

	/** Valeur du registre `r` du destinataire à la fin du combat. */
	private String result() {
		var registers = registerStore.get(receiver.getId());
		Assert.assertNotNull("le destinataire n'a rien reçu", registers);
		return registers;
	}

	@Test
	public void legacyArraysBecomeArraysAndMapsInV4() throws Exception {
		attach(sender, 1, "sendTo(getNearestEnemy(), 1, [\"CHANSON\", [19, 0]]);\n"
			+ "sendTo(getNearestEnemy(), 2, [\"a\": 1, \"b\": [5]]);");
		attach(receiver, 4, "var r = '';\n"
			+ "for (var m in getMessages()) {\n"
			+ "  var p = m[2];\n"
			+ "  if (m[1] == 1) r += count(p) + ',' + count(p[1]) + ',' + p[0] + ',' + (p instanceof Array) + ';';\n"
			+ "  else r += (p instanceof Map) + ',' + p['a'] + ',' + count(p['b']) + ';';\n"
			+ "}\n"
			+ "if (r != '') setRegister('r', r);");
		runFight();

		Assert.assertTrue(result(), result().contains("2,2,CHANSON,true;true,1,1;"));
	}

	@Test
	public void arraysAndMapsBecomeLegacyArraysInV1() throws Exception {
		attach(sender, 4, "sendTo(getNearestEnemy(), 1, ['CHANSON', [19, 0]]);\n"
			+ "sendTo(getNearestEnemy(), 2, ['k': [7, 8]]);");
		attach(receiver, 1, "var r = \"\";\n"
			+ "for (var m in getMessages()) {\n"
			+ "  var p = m[2];\n"
			+ "  if (m[1] == 1) { r = r + count(p) + \",\" + count(p[1]) + \";\"; }\n"
			+ "  else { r = r + count(p[\"k\"]) + \";\"; }\n"
			+ "}\n"
			+ "if (r != \"\") { setRegister(\"r\", r); }");
		runFight();

		Assert.assertTrue(result(), result().contains("2,2;2;"));
	}
}
