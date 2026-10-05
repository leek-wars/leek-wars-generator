package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.action.Action;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.state.Entity;

import leekscript.AILog;
import leekscript.common.Error;

/**
 * Une entité sans IA équipée (la tourelle d'un camp de bots en combat de test, une tourelle
 * d'équipe ou un poireau sans IA) passe ses tours : un avertissement NO_AI_EQUIPPED, une
 * seule fois, et aucune action de plantage. Une IA équipée mais introuvable plante toujours.
 */
public class TestNoAIEquipped extends FightTestBase {

	private Leek player;
	private Leek idle;

	@Override
	protected void createLeeks() {
		player = defaultLeek(1, "Joueur");
		idle = defaultLeek(2, "SansIA");
		fight.getState().addEntity(0, player);
		fight.getState().addEntity(1, idle);
	}

	/** Comme Generator.runScenario : logs posés, puis IA résolue depuis la description. */
	private void resolve(Entity entity, EntityInfo info) {
		entity.setLogs(new LeekLog(farmerLog, entity));
		entity.setFight(fight);
		entity.setBirthTurn(1);
		entity.setAIFile(EntityAI.resolve(generator, info, entity));
	}

	private int countSystemLogs(Entity entity, int level, Error error) {
		int count = 0;
		for (var actionLogs : farmerLog.toJSON()) {
			for (var log : actionLogs) {
				if (log.get(0).asInt() == entity.getFId() && log.get(1).asInt() == level && log.get(3).asInt() == error.ordinal()) {
					count++;
				}
			}
		}
		return count;
	}

	@Test
	public void entityWithoutAIPassesItsTurnsWithoutCrashing() throws Exception {
		attachAI(player, "return 1");
		resolve(idle, new EntityInfo());
		runFight();

		Assert.assertFalse(idle.isAIEquipped());
		Assert.assertTrue("l'entité sans IA a bien joué ses tours", countActions(Action.END_TURN, idle) > 1);
		Assert.assertEquals("aucun plantage", 0, countActions(Action.AI_ERROR, idle));
		Assert.assertEquals(1, countSystemLogs(idle, AILog.SWARNING, Error.NO_AI_EQUIPPED));
		Assert.assertEquals(0, countSystemLogs(idle, AILog.SERROR, Error.NO_AI_EQUIPPED));
	}

	@Test
	public void missingAIStillCrashes() throws Exception {
		attachAI(player, "return 1");
		var info = new EntityInfo();
		info.ai = "introuvable.leek";
		info.ai_folder = -12345;
		resolve(idle, info);
		runFight();

		Assert.assertTrue(idle.isAIEquipped());
		Assert.assertTrue("une IA équipée mais introuvable plante", countActions(Action.AI_ERROR, idle) > 0);
		Assert.assertEquals(1, countSystemLogs(idle, AILog.SERROR, Error.AI_NOT_EXISTING));
	}
}
