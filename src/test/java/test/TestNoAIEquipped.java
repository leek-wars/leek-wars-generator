package test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.leekwars.generator.Generator;
import com.leekwars.generator.action.Action;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.scenario.TeamInfo;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.test.LocalTrophyManager;

import tools.jackson.databind.JsonNode;

import leekscript.AILog;
import leekscript.common.Error;
import leekscript.compiler.LeekScript;

/**
 * Une entité sans IA équipée (la tourelle d'un camp de bots en combat de test, une tourelle
 * d'équipe ou un poireau sans IA) passe ses tours : un avertissement NO_AI_EQUIPPED, une
 * seule fois, et aucune action de plantage. Une IA équipée mais introuvable plante toujours.
 * Le combat passe par Generator.runScenario, comme celui du worker.
 */
public class TestNoAIEquipped {

	private static final int TEAM = 7;
	private static final int TURNS = 3;

	private Outcome outcome;
	private JsonNode actions;
	private final Map<String, Integer> fids = new HashMap<>();

	@Before
	public void setUp() {
		// Une autre classe de test a pu laisser son système de fichiers : l'IA introuvable doit
		// l'être dans celui par défaut.
		LeekScript.resetFileSystem();
	}

	private static EntityInfo entity(String name, int type, int team) {
		var e = new EntityInfo();
		e.id = name.hashCode();
		e.name = name;
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

	/** Camp 0 : `first` ; camp 1 : un poireau sans IA et la tourelle de l'équipe (une tourelle
	 * seule ne garde pas son camp en vie, le combat finirait au premier tour). */
	private void run(EntityInfo first) {
		var scenario = new Scenario();
		scenario.seed = 1;
		scenario.maxTurns = TURNS;
		var team = new TeamInfo();
		team.id = TEAM;
		scenario.teams.put(TEAM, team);
		scenario.addEntity(0, first);
		scenario.addEntity(1, entity("poireau", Entity.TYPE_LEEK, TEAM));
		scenario.addEntity(1, entity("tourelle", Entity.TYPE_TURRET, TEAM));
		var registers = new RegisterManager() {
			@Override public String getRegisters(int leek) { return null; }
			@Override public void saveRegisters(int leek, String registers, boolean isNew) {}
		};
		outcome = new Generator().runScenario(scenario, null, registers, new LocalTrophyManager());
		Assert.assertNull("le combat ne doit pas lever d'exception", outcome.exception);
		var report = outcome.fight.toJSON();
		actions = report.get("actions");
		for (var leek : report.get("leeks")) {
			fids.put(leek.get("name").asString(), leek.get("id").asInt());
		}
	}

	private int countActions(String entity, int type) {
		int fid = fids.get(entity), count = 0;
		for (var action : actions) {
			if (action.get(0).asInt() == type && action.get(1).asInt() == fid) count++;
		}
		return count;
	}

	/** Les poireaux loguent chez leur éleveur (0), la tourelle sous la clé -TEAM (cf. Generator.runScenario). */
	private int countSystemLogs(String entity, int level, Error error) {
		int fid = fids.get(entity), count = 0;
		var logs = outcome.logs.get(entity.equals("tourelle") ? -TEAM : 0);
		for (var actionLogs : logs.toJSON()) {
			for (var log : actionLogs) {
				if (log.get(0).asInt() == fid && log.get(1).asInt() == level && log.get(3).asInt() == error.ordinal()) count++;
			}
		}
		return count;
	}

	@Test
	public void entitiesWithoutAIPassTheirTurnsWithoutCrashing() {
		run(entity("joueur", Entity.TYPE_LEEK, 0));

		for (var entity : List.of("joueur", "poireau", "tourelle")) {
			Assert.assertEquals(entity + " joue ses tours", TURNS, countActions(entity, Action.END_TURN));
			Assert.assertEquals(entity + " ne plante pas", 0, countActions(entity, Action.AI_ERROR));
			Assert.assertEquals(1, countSystemLogs(entity, AILog.SWARNING, Error.NO_AI_EQUIPPED));
			Assert.assertEquals(0, countSystemLogs(entity, AILog.SERROR, Error.NO_AI_EQUIPPED));
		}
	}

	@Test
	public void missingAIStillCrashes() {
		var leek = entity("joueur", Entity.TYPE_LEEK, 0);
		leek.ai = "introuvable.leek";
		leek.ai_folder = -12345;
		run(leek);

		Assert.assertEquals("une IA équipée mais introuvable plante à chaque tour", TURNS, countActions("joueur", Action.AI_ERROR));
		Assert.assertEquals(1, countSystemLogs("joueur", AILog.SERROR, Error.AI_NOT_EXISTING));
		Assert.assertEquals("la tourelle sans IA, elle, ne plante pas", 0, countActions("tourelle", Action.AI_ERROR));
	}
}
