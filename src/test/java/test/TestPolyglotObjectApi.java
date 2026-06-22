package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;
import com.leekwars.generator.state.Entity;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * API de combat ORIENTÉE OBJET (tranche 1 : me / Entity / Cell / Fight) pour les IA polyglot.
 * Couche guest au-dessus de l'API plate. On vérifie les lectures de propriétés (me.life, me.cell.x),
 * l'accès aux entités (Fight.getNearestEnemy() -> Entity), et un vrai combat où deux IA en API objet
 * se rapprochent (me.moveToward(enemy), me.cell.distance(enemy)).
 */
public class TestPolyglotObjectApi extends FightTestBase {

	private Leek leek1;
	private Leek leek2;

	@Override
	protected void createLeeks() {
		leek1 = defaultLeek(1, "Obj1");
		leek2 = defaultLeek(2, "Obj2");
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, leek2);
	}

	private Object eval(PolyglotSandbox sb, String src) throws Exception {
		PolyglotEntityAI ai = new PolyglotEntityAI("js", src, sb);
		ai.setEntity(leek1);
		ai.setLogs(new LeekLog(farmerLog, leek1));
		ai.setFight(fight);
		return ai.runIA();
	}

	@Test
	public void entityPropertiesReadState() throws Exception {
		initFightOnly();
		try (PolyglotSandbox sb = new PolyglotSandbox("js")) {
			Assert.assertEquals((long) leek1.getLife(), ((Number) eval(sb, "me.life;")).longValue());
			Assert.assertEquals((long) leek1.getStat(Entity.STAT_STRENGTH), ((Number) eval(sb, "me.strength;")).longValue());
			// me.cell.x == getCellX(getCell()) ; me est l'IA courante.
			Object cx = eval(sb, "me.cell.x;");
			Assert.assertNotNull(cx);
			// Fight.getNearestEnemy() renvoie une Entity dont l'id est le fid de leek2.
			Assert.assertEquals((long) leek2.getFId(), ((Number) eval(sb, "Fight.getNearestEnemy().id;")).longValue());
			// distance de me a l'ennemi via l'objet == via l'API plate.
			Object dObj = eval(sb, "me.cell.distance(Fight.getNearestEnemy());");
			Object dFlat = eval(sb, "getCellDistance(getCell(), getCell(getNearestEnemy()));");
			Assert.assertEquals(((Number) dFlat).longValue(), ((Number) dObj).longValue());
		}
	}

	private void attachJsAI(Leek leek, String code) {
		AIFile file = new AIFile("obj_" + System.nanoTime() + ".js", code,
			System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek.getId(), false);
		leek.setAIFile(file);
		leek.setLogs(new LeekLog(farmerLog, leek));
		leek.setFight(fight);
		leek.setBirthTurn(1);
	}

	@Test
	public void objectApiMoverClosesDistanceInRealFight() throws Exception {
		String ai =
			"function turn() {"
			+ "  var enemy = Fight.getNearestEnemy();"
			+ "  if (enemy == null) return;"
			+ "  if (getRegister('start') == null) setRegister('start', '' + me.cell.distance(enemy));"
			+ "  me.moveToward(enemy);"
			+ "  setRegister('end', '' + me.cell.distance(enemy));"
			+ "}";
		attachJsAI(leek1, ai);
		attachJsAI(leek2, ai);
		runFight();

		Assert.assertTrue(leek1.getAI() instanceof PolyglotEntityAI);
		int s1 = Integer.parseInt(leek1.getRegister("start"));
		int e1 = Integer.parseInt(leek1.getRegister("end"));
		int s2 = Integer.parseInt(leek2.getRegister("start"));
		int e2 = Integer.parseInt(leek2.getRegister("end"));
		System.out.println("[objet] leek1 distance " + s1 + " -> " + e1 + " | leek2 " + s2 + " -> " + e2);
		Assert.assertTrue("leek1 (API objet) doit s'etre rapproche : " + s1 + " -> " + e1, e1 < s1);
		Assert.assertTrue("leek2 (API objet) doit s'etre rapproche : " + s2 + " -> " + e2, e2 < s2);
	}
}
