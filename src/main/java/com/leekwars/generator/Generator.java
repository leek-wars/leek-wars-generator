package com.leekwars.generator;

import java.io.File;
import java.util.List;

import com.leekwars.generator.util.Json;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.bulbs.BulbTemplate;
import com.leekwars.generator.bulbs.Bulbs;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.chips.ChipType;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.component.Component;
import com.leekwars.generator.component.Components;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.weapons.Weapon;
import com.leekwars.generator.weapons.Weapons;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.FightListener;
import com.leekwars.generator.fight.StatisticsManager;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.state.Entity;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;
import leekscript.common.Error;

public class Generator {

	private static final String TAG = Generator.class.getSimpleName();

	private static ErrorManager errorManager = null;

	public boolean use_leekscript_cache = true;

	public Generator() {
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions(FightFunctions.getFunctions(), "com.leekwars.generator.classes.*");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
		loadWeapons();
		loadChips();
		loadSummons();
		loadComponents();
	}

	/**
	 * Analyze an AI task: read a AI code and check for validity. Returns whether
	 * the AI is valid, or returns the list of errors.
	 *
	 * @param file    The AI file name.
	 * @param context The AI resolver context (real folder or DB virtual folder).
	 * @return a object representing the analysis results: success or list of
	 *         errors.
	 */
	public AnalyzeResult analyzeAI(AIFile ai, int farmer) {
		Log.i(TAG, "Analyze AI " + ai + "..." + ai.hashCode());
		try {
			long t = System.currentTimeMillis();
			AnalyzeResult result = new IACompiler().analyze(ai);
			long time = System.currentTimeMillis() - t;
			Log.s(TAG, "Time: " + ((double) time / 1000) + " seconds");
			Log.s(TAG, "Analyze success!");
			if (result.tooMuchErrors != null) {
				errorManager.exception(result.tooMuchErrors, -1, farmer, ai);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace(System.out);
			Log.e(TAG, "AI " + ai + " not compiled");
			if (e.getMessage() != null) {
				Log.e(TAG, e.getMessage());
			}
			Log.e(TAG, "Compile failed!");
			errorManager.exception(e, 0, farmer, ai);
			// Create a result with internal error
			AnalyzeResult result = new AnalyzeResult();
			result.success = false;
			result.informations = Json.createArray();
			ArrayNode error = Json.createArray();
			error.add(0);
			error.add(ai != null ? ai.getId() : 0);
			error.add(1);
			error.add(0);
			error.add(1);
			error.add(0);
			error.add(Error.INTERNAL_ERROR.ordinal());
			result.informations.add(error);
			return result;
		}
	}

	/**
	 * Runs a scenario.
	 *
	 * @param scenario the scenario to run.
	 * @return the fight outcome generated.
	 */
	public Outcome runScenario(Scenario scenario, FightListener listener, RegisterManager registerManager, StatisticsManager statisticsManager) {

		Outcome outcome = new Outcome();

		Fight fight = new Fight(this, listener);
		fight.getState().setRegisterManager(registerManager);
		fight.setStatisticsManager(statisticsManager);
		fight.setId(scenario.fightID);
		fight.setBoss(scenario.boss);
		fight.setMaxTurns(scenario.maxTurns);
		fight.getState().setType(scenario.type);
		fight.getState().setContext(scenario.context);
		fight.getState().setCustomMap(scenario.map);
		fight.getState().seed(scenario.seed);

		// Create logs and compile AIs
		int t = 0;
		for (List<EntityInfo> team : scenario.entities) {
			for (EntityInfo entityInfo : team) {

				// Create farmer logs
				int aiOwner = entityInfo.aiOwner;
				if (entityInfo.type == Entity.TYPE_MOB) aiOwner = 0;
				if (!outcome.logs.containsKey(aiOwner)) {
					outcome.logs.put(aiOwner, new FarmerLog(fight, entityInfo.farmer));
				}
				// Create entity
				var entity = entityInfo.createEntity(this, scenario, fight);
				fight.getState().addEntity(t, entity);
				entity.setFight(fight);
				entity.setBirthTurn(1);

				// Resolve AI
				entity.setLogs(new LeekLog(outcome.logs.get(aiOwner), entity));
				entity.setAIFile(EntityAI.resolve(this, entityInfo, entity));
			}
			t++;
		}

		try {
			Log.i(TAG, "Start fight...");
			fight.startFight(scenario.drawCheckLife);
			fight.finishFight();

			outcome.fight = fight.getState().getActions();
			outcome.fight.dead = fight.getState().getDeadReport();
			outcome.winner = fight.getWinner();
			outcome.duration = fight.getState().getDuration();
			outcome.statistics = statisticsManager;
			for (var entity : fight.getState().getEntities().values()) {
				if (entity.getAI() != null) {
					outcome.analyzeTime += ((EntityAI) entity.getAI()).getAnalyzeTime();
					outcome.compilationTime += ((EntityAI) entity.getAI()).getCompileTime();
				}
			}
			outcome.executionTime = fight.executionTime;

			// Save registers
			for (var entity : fight.getState().getEntities().values()) {
				if (!entity.isSummon() && entity.getRegisters() != null	&& (entity.getRegisters().isModified() || entity.getRegisters().isNew())) {
					registerManager.saveRegisters(entity.getId(), entity.getRegisters().toJSONString(), entity.getRegisters().isNew());
				}
			}
			Log.i(TAG, "SHA-1: " + Util.sha1(outcome.toString()));
			Log.s(TAG, "Fight generated!");

			return outcome;

			// Write to file
			// try (Writer writer = new BufferedWriter(new OutputStreamWriter(new
			// FileOutputStream("../client/src/report.json"), "utf-8"))) {
			// writer.write(report.toString());
			// }
		} catch (Exception e) {
			outcome.exception = e;
			e.printStackTrace();
			Log.e(TAG, "Error during fight generation!");
			return outcome;
		}
	}

	private void loadWeapons() {
		try {
			Log.start(TAG, "- Loading weapons... ");
			ObjectNode weapons = Json.parseObject(Util.readFile("data/weapons.json"));
			for (var entry : weapons.properties()) {
				String id = entry.getKey();
				ObjectNode weapon = (ObjectNode) entry.getValue();
				Weapons.addWeapon(new Weapon(weapon.get("item").intValue(), weapon.get("cost").intValue(),
						weapon.get("min_range").intValue(), weapon.get("max_range").intValue(), (ArrayNode) weapon.get("effects"),
						(byte) weapon.get("launch_type").intValue(), (byte) weapon.get("area").intValue(), weapon.get("los").booleanValue(),
						weapon.get("template").intValue(), weapon.get("name").asString(), (ArrayNode) weapon.get("passive_effects"), weapon.get("max_uses").intValue()));
			}
			Log.end(weapons.size() + " weapons loaded.");
		} catch (Exception e) {
			Log.end();
			Log.e(TAG, "Error loading weapons! cwd: " + System.getProperty("user.dir") + ", e: " + e);
			exception(e);
		}
	}

	private void loadChips() {
		try {
			Log.start(TAG, "- Loading chips... ");
			ObjectNode chips = Json.parseObject(Util.readFile("data/chips.json"));
			for (var entry : chips.properties()) {
				String id = entry.getKey();
				ObjectNode chip = (ObjectNode) entry.getValue();
				// System.out.println("New chip " + chip.get("name").asString() + " " + id + " " + chip.get("template").intValue());
				Chips.addChip(new Chip(Integer.parseInt(id), chip.get("cost").intValue(), chip.get("min_range").intValue(),
						chip.get("max_range").intValue(), (ArrayNode) chip.get("effects"), (byte) chip.get("launch_type").intValue(),
						(byte) chip.get("area").intValue(), chip.get("los").booleanValue(), chip.get("cooldown").intValue(),
						chip.get("team_cooldown").booleanValue(), chip.get("initial_cooldown").intValue(), chip.get("level").intValue(),
						chip.get("template").intValue(), chip.get("name").asString(), ChipType.values()[chip.get("type").intValue()], chip.get("max_uses").intValue()));
			}
			Log.end(chips.size() + " chips loaded.");
		} catch (Exception e) {
			Log.end();
			Log.e(TAG, "Error loading chips! cwd: " + System.getProperty("user.dir") + ", e: " + e);
			exception(e);
		}
	}

	private void loadSummons() {
		try {
			Log.start(TAG, "- Loading bulbs... ");
			ObjectNode summons = Json.parseObject(Util.readFile("data/summons.json"));
			for (var entry : summons.properties()) {
				String id = entry.getKey();
				ObjectNode summon = (ObjectNode) entry.getValue();
				Bulbs.addInvocationTemplate(new BulbTemplate(Integer.parseInt(id), summon.get("name").asString(),
						(ArrayNode) summon.get("chips"), (ObjectNode) summon.get("characteristics")));
			}
			Log.end(summons.size() + " summons loaded.");
		} catch (Exception e) {
			Log.end();
			Log.e(TAG, "Error loading summons! cwd: " + System.getProperty("user.dir") + ", e: " + e);
			exception(e);
		}
	}

	private void loadComponents() {
		try {
			Log.start(TAG, "- Loading components... ");
			ObjectNode components = Json.parseObject(Util.readFile("data/components.json"));
			for (var entry : components.properties()) {
				String id = entry.getKey();
				ObjectNode component = (ObjectNode) entry.getValue();
				Components.addComponent(new Component(Integer.parseInt(id), component.get("name").asString(), (ArrayNode) component.get("stats"), component.get("template").intValue()));
			}
			Log.end(components.size() + " components loaded.");
		} catch (Exception e) {
			Log.end();
			Log.e(TAG, "Error loading components! cwd: " + System.getProperty("user.dir") + ", e: " + e);
			exception(e);
		}
	}

	public void setCache(boolean cache) {
		this.use_leekscript_cache = cache;
	}

	/**
	 * Compile an AI task (debug purposes)
	 */
	/*
	public String compileAI(String file, ResolverContext context) {
		Log.i(TAG, "Compile AI " + file + "...");
		try {
			AI ai = LeekScript.compileFileContext(file, "com.leekwars.generator.fight.entity.EntityAI", context, false);
			return ai != null ? "success" : "failure";
		} catch (LeekScriptException e) {
			System.out.println("LeekScriptException " + e.getType());
			e.printStackTrace();
			return e.getMessage();
		} catch (LeekCompilerException e) {
			System.out.println("LeekCompilerException");
			e.printStackTrace();
			return e.getMessage();
		} catch (Exception e) {
			System.out.println("Exception");
			e.printStackTrace();
			return e.getMessage();
		}
	}
	*/

	public String downloadAI(AIFile ai) {
		Log.i(TAG, "Download AI " + ai + "...");
		try {
			return LeekScript.mergeFile(ai);
		} catch (Exception e) {
			System.out.println("Exception " + e.getMessage());
			e.printStackTrace(System.out);
			return e.getMessage();
		}
	}

	public static void setErrorManager(ErrorManager manager) {
		errorManager = manager;
	}

	public void exception(Throwable e) {
		if (errorManager != null) {
			errorManager.exception(e, -1);
		}
	}
	public void exception(Throwable e, Fight fight) {
		if (errorManager != null) {
			errorManager.exception(e, fight.getId());
		}
	}
	public void exception(Throwable e, Fight fight, int farmer, AIFile file) {
		if (errorManager != null) {
			errorManager.exception(e, fight.getId(), farmer, file);
		}
	}
}
