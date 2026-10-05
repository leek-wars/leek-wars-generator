package test;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.action.Action;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.FarmerInfo;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.scenario.TeamInfo;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.AILog;
import leekscript.common.Error;

/**
 * Une entité sans IA équipée (la tourelle d'un camp de bots en combat de test, une tourelle
 * d'équipe ou un poireau sans IA) passe ses tours : un avertissement NO_AI_EQUIPPED, une
 * seule fois, et aucune action de plantage. Une IA équipée mais introuvable plante toujours.
 * Le combat passe par Generator.runScenario, comme celui du worker.
 */
public class TestNoAIEquipped {

	private static final int TEAM = 7;
	private static final int TURNS = 3;

	private static EntityInfo entity(int id, int type, int team) {
		var e = new EntityInfo();
		e.id = id;
		e.name = "e" + id;
		e.type = type;
		e.team = team;
		e.level = 100;
		e.life = 1000;
		e.tp = 10;
		e.mp = 3;
		e.frequency = 100;
		e.cores = 10;
		e.ram = 10;
		return e;
	}

	private static Outcome run(EntityInfo first) {
		var scenario = new Scenario();
		scenario.maxTurns = TURNS;
		var farmer = new FarmerInfo();
		farmer.name = "Éleveur";
		farmer.country = "fr";
		scenario.farmers.put(0, farmer);
		var team = new TeamInfo();
		team.id = TEAM;
		team.name = "Équipe";
		scenario.teams.put(TEAM, team);
		// Camp 0 : le premier poireau ; camp 1 : un poireau sans IA et la tourelle de l'équipe
		// (une tourelle seule ne garde pas son camp en vie, le combat finirait au premier tour).
		scenario.addEntity(0, first);
		scenario.addEntity(1, entity(2, Entity.TYPE_LEEK, TEAM));
		scenario.addEntity(1, entity(3, Entity.TYPE_TURRET, TEAM));
		var registers = new RegisterManager() {
			@Override public String getRegisters(int leek) { return null; }
			@Override public void saveRegisters(int leek, String registers, boolean isNew) {}
		};
		return new Generator().runScenario(scenario, null, registers, new LocalTrophyManager());
	}

	private static int countActions(Outcome outcome, int type) {
		int count = 0;
		for (var action : outcome.fight.toJSON().get("actions")) {
			if (action.get(0).asInt() == type) count++;
		}
		return count;
	}

	private static int countSystemLogs(FarmerLog logs, int level, Error error) {
		int count = 0;
		for (var actionLogs : logs.toJSON()) {
			for (var log : actionLogs) {
				if (log.get(1).asInt() == level && log.get(3).asInt() == error.ordinal()) count++;
			}
		}
		return count;
	}

	@Test
	public void entitiesWithoutAIPassTheirTurnsWithoutCrashing() {
		var outcome = run(entity(1, Entity.TYPE_LEEK, 0));

		Assert.assertEquals("aucun plantage", 0, countActions(outcome, Action.AI_ERROR));
		Assert.assertEquals("les trois entités jouent leurs tours", 3 * TURNS, countActions(outcome, Action.END_TURN));
		// Un avertissement par entité : les deux poireaux loguent chez leur éleveur, la tourelle
		// sous -équipe (cf. Generator.runScenario).
		FarmerLog farmerLogs = outcome.logs.get(0), turretLogs = outcome.logs.get(-TEAM);
		Assert.assertEquals(2, countSystemLogs(farmerLogs, AILog.SWARNING, Error.NO_AI_EQUIPPED));
		Assert.assertEquals(1, countSystemLogs(turretLogs, AILog.SWARNING, Error.NO_AI_EQUIPPED));
		Assert.assertEquals(0, countSystemLogs(farmerLogs, AILog.SERROR, Error.NO_AI_EQUIPPED));
		Assert.assertEquals(0, countSystemLogs(turretLogs, AILog.SERROR, Error.NO_AI_EQUIPPED));
	}

	@Test
	public void missingAIStillCrashes() {
		var leek = entity(1, Entity.TYPE_LEEK, 0);
		leek.ai = "introuvable.leek";
		leek.ai_folder = -12345;
		var outcome = run(leek);

		Assert.assertEquals("une IA équipée mais introuvable plante à chaque tour", TURNS, countActions(outcome, Action.AI_ERROR));
		Assert.assertEquals(1, countSystemLogs(outcome.logs.get(0), AILog.SERROR, Error.AI_NOT_EXISTING));
	}
}
