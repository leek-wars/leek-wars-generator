package com.leekwars.generator;

import java.io.File;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.chips.ChipType;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.attack.weapons.Weapons;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.FightListener;
import com.leekwars.generator.fight.bulbs.BulbTemplate;
import com.leekwars.generator.fight.bulbs.Bulbs;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.fight.statistics.StatisticsManager;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.Scenario;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.resolver.FileSystemContext;
import leekscript.compiler.resolver.ResolverContext;
import leekscript.functions.Functions;
import leekscript.runner.AI;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;
import leekscript.common.Error;

public class Generator {

	private static final String TAG = Generator.class.getSimpleName();

	private static ErrorManager errorManager = null;

	public boolean use_leekscript_cache = true;

	public Generator() {
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions("com.leekwars.generator.FightFunctions");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
		loadWeapons();
		loadChips();
		loadSummons();
		loadFunctions();
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
	public AnalyzeResult analyzeAI(String file, ResolverContext context) {
		Log.i(TAG, "Analyze AI " + file + "...");
		AIFile<?> ai = null;
		try {
			ai = context instanceof FileSystemContext ? LeekScript.getFileSystemResolver().resolve(file, context) : LeekScript.getResolver().resolve(file, context);
			long t = System.currentTimeMillis();
			AnalyzeResult result = new IACompiler().analyze(ai);
			long time = System.currentTimeMillis() - t;
			Log.s(TAG, "Time: " + ((double) time / 1000) + " seconds");
			Log.s(TAG, "Analyze success!");
			return result;
		} catch (Exception e) {
			e.printStackTrace(System.out);
			Log.e(TAG, "AI " + file + " not compiled");
			if (e.getMessage() != null) {
				Log.e(TAG, e.getMessage());
			}
			Log.e(TAG, "Compile failed!");
			errorManager.exception(e, 0);
			// Create a result with internal error
			AnalyzeResult result = new AnalyzeResult();
			result.success = false;
			result.informations = new JSONArray();
			JSONArray error = new JSONArray();
			error.add(0);
			error.add(ai != null ? ai.getId() : 0);
			error.add(1);
			error.add(0);
			error.add("?");
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
		fight.setRegisterManager(registerManager);
		fight.setStatisticsManager(statisticsManager);
		fight.setId(scenario.fightID);
		fight.setMaxTurns(scenario.maxTurns);
		fight.setType(scenario.type);
		fight.setContext(scenario.context);
		fight.setCustomMap(scenario.map);
		fight.seed(scenario.seed);

		// Create logs and compile AIs
		int t = 0;
		for (List<EntityInfo> team : scenario.entities) {
			for (EntityInfo entityInfo : team) {

				// Create farmer logs
				int aiOwner = entityInfo.aiOwner;
				if (!outcome.logs.containsKey(aiOwner)) {
					outcome.logs.put(aiOwner, new FarmerLog(fight, entityInfo.farmer));
				}
				// Create entity
				Entity entity = entityInfo.createEntity(this, scenario, fight);
				fight.addEntity(t, entity);

				// Resolve AI
				entity.setLogs(new LeekLog(outcome.logs.get(aiOwner), entity));
				entity.setAIFile(EntityAI.resolve(this, entityInfo, entity));
			}
			t++;
		}

		try {
			Log.i(TAG, "Start fight...");
			fight.startFight();
			fight.finishFight();

			outcome.fight = fight.getActions();
			outcome.fight.dead = fight.getDeadReport();
			outcome.winner = fight.getWinner();
			outcome.duration = fight.getDuration();
			outcome.statistics = statisticsManager;
			for (var entity : fight.getEntities().values()) {
				if (entity.getAI() != null) {
					outcome.analyzeTime += entity.getAI().getAnalyzeTime();
					outcome.compilationTime += entity.getAI().getCompileTime();
				}
			}
			outcome.executionTime = fight.executionTime;

			// Save registers
			for (Entity entity : fight.getEntities().values()) {
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
			JSONObject weapons = JSON.parseObject(Util.readFile("data/weapons.json"));
			for (String id : weapons.keySet()) {
				JSONObject weapon = weapons.getJSONObject(id);
				Weapons.addWeapon(new Weapon(weapon.getInteger("item"), (byte) 1, weapon.getInteger("cost"),
						weapon.getInteger("min_range"), weapon.getInteger("max_range"), weapon.getJSONArray("effects"),
						weapon.getByte("launch_type"), weapon.getByte("area"), weapon.getBoolean("los"),
						weapon.getInteger("template"), weapon.getString("name"), weapon.getJSONArray("passive_effects")));
			}
			Log.end(weapons.size() + " weapons loaded.");
		} catch (Exception e) {
			Log.e(TAG, "Error loading weapons!");
			exception(e);
		}
	}

	private void loadChips() {
		try {
			Log.start(TAG, "- Loading chips... ");
			JSONObject chips = JSON.parseObject(Util.readFile("data/chips.json"));
			for (String id : chips.keySet()) {
				JSONObject chip = chips.getJSONObject(id);
				Chips.addChip(new Chip(Integer.parseInt(id), chip.getInteger("cost"), chip.getInteger("min_range"),
						chip.getInteger("max_range"), chip.getJSONArray("effects"), chip.getByte("launch_type"),
						chip.getByte("area"), chip.getBoolean("los"), chip.getInteger("cooldown"),
						chip.getBoolean("team_cooldown"), chip.getInteger("initial_cooldown"), chip.getInteger("level"),
						chip.getInteger("template"), chip.getString("name"), ChipType.values()[chip.getInteger("type")]));
			}
			Log.end(chips.size() + " chips loaded.");
		} catch (Exception e) {
			exception(e);
		}
	}

	private void loadSummons() {
		try {
			Log.start(TAG, "- Loading bulbs... ");
			JSONObject summons = JSON.parseObject(Util.readFile("data/summons.json"));
			for (String id : summons.keySet()) {
				JSONObject summon = summons.getJSONObject(id);
				Bulbs.addInvocationTemplate(new BulbTemplate(Integer.parseInt(id), summon.getString("name"),
						summon.getJSONArray("chips"), summon.getJSONObject("characteristics")));
			}
			Log.end(summons.size() + " summons loaded.");
		} catch (Exception e) {
			exception(e);
		}
	}

	private void loadFunctions() {
		try {
			Log.start(TAG, "- Loading functions... ");
			JSONObject functions = JSON.parseObject(Util.readFile("data/functions.json"));
			for (String name : functions.keySet()) {
				JSONObject function = functions.getJSONObject(name);
				Functions.addFunctionOperations(name, function.getIntValue("op"), function.getString("var_op"));
			}
			Log.end(functions.size() + " functions loaded.");
		} catch (Exception e) {
			exception(e);
		}
	}

	public void setCache(boolean cache) {
		this.use_leekscript_cache = cache;
	}

	/**
	 * Compile an AI task (debug purposes)
	 */
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

	public String downloadAI(String file, ResolverContext context) {
		Log.i(TAG, "Download AI " + file + "...");
		try {
			return LeekScript.mergeFile(file, context);
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
	public void exception(Throwable e, Fight fight, int aiID) {
		if (errorManager != null) {
			errorManager.exception(e, fight.getId(), aiID);
		}
	}
}
