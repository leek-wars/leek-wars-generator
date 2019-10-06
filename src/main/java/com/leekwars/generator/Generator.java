package com.leekwars.generator;

import java.io.File;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.attack.weapons.Weapons;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.bulbs.BulbTemplate;
import com.leekwars.generator.fight.bulbs.Bulbs;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.leek.RegisterManager;
import com.leekwars.generator.report.Report;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.Scenario;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.RandomGenerator;
import leekscript.compiler.resolver.ResolverContext;
import leekscript.functions.Functions;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class Generator {

	private static final String TAG = Generator.class.getSimpleName();

	private static RegisterManager registerManager = null;
	private static RandomGenerator randomGenerator = new RandomGenerator() {
		private long n = 0;
		public void seed(long seed) {
			n = seed;
		}
		@Override
		public double getDouble() {
			n = n * 1103515245 + 12345;
			long r = (n / 65536) % 32768 + 32768;
			return (double) r / 65536;
		}
		@Override
		public int getInt(int min, int max) {
			if (max - min + 1 <= 0)
				return 0;
			return min + (int) (getDouble() * (max - min + 1));
		}
	};
	private boolean nocache = false;
	private String jar = "generator.jar";

	public Generator() {
		new File("ai/").mkdir();
		loadWeapons();
		loadChips();
		loadSummons();
		loadFunctions();
		LeekFunctions.setExtraFunctions("com.leekwars.generator.FightFunctions");
		LeekConstants.setExtraConstants("com.leekwars.generator.FightConstants");
		LeekScript.setRandomGenerator(randomGenerator);
	}

	/**
	 * Analyze an AI task: read a AI code and check for validity. Returns whether the AI is valid,
	 * or returns the list of errors.
	 *
	 * @param file The AI file name.
	 * @param context The AI resolver context (real folder or DB virtual folder).
	 * @return a string representing the analysis results: success or list of errors.
	 */
	public String analyzeAI(String file, ResolverContext context) {
		Log.i(TAG, "Analyze AI " + file + "...");
		try {
			AIFile<?> ai = LeekScript.getResolver().resolve(file, context);
			long t = System.currentTimeMillis();
			String result = new IACompiler().analyze(ai);
			long time = System.currentTimeMillis() - t;
			Log.s(TAG, "Time: " + ((double) time / 1000) + " seconds");
			Log.s(TAG, "Analyze success!");
			return result;
		} catch (Exception e1) {
			System.out.println(e1);
			e1.printStackTrace();
			Log.e(TAG, "AI " + file + " not compiled");
			if (e1.getMessage() != null) {
				Log.e(TAG, e1.getMessage());
			}
			Log.e(TAG, "Compile failed!");
			return "";
		}
	}

	/**
	 * Runs a scenario.
	 * @param scenario the scenario to run.
	 * @return the fight report generated.
	 */
	public Report runScenario(Scenario scenario) {

		if (scenario.seed != 0) {
			randomGenerator.seed(scenario.seed);
		}

		Report report = new Report();

		Fight fight = new Fight();
		fight.setMaxTurns(scenario.maxTurns);

		// Create logs and compile AIs
		int t = 0;
		for (List<EntityInfo> team : scenario.entities) {
			for (EntityInfo entityInfo : team) {

				Entity entity = entityInfo.createEntity();

				int farmer = entity.getFarmer();
				if (!report.logs.containsKey(farmer)) {
					report.logs.put(farmer, new LeekLog(entity));
				}
				String aiFile = entity.getAIFile();
				boolean validAI = false;
				if (aiFile != null) {
					Log.i(TAG, "Compile AI " + aiFile + "...");
					try {
						ResolverContext context = LeekScript.getResolver().createContext(farmer);
						EntityAI ai = (EntityAI) LeekScript.compileFileContext(aiFile, "com.leekwars.generator.fight.entity.EntityAI", getJar(), context, nocache);
						Log.i(TAG, "AI " + aiFile + " compiled!");
						entity.setAI(ai);
						ai.setEntity(entity);
						ai.setLogs(report.logs.get(farmer));
						validAI = true;
					} catch (Exception e1) {
						Log.w(TAG, "AI " + aiFile + " not compiled");
						Log.w(TAG, e1.getMessage());
					}
				}
				if (!validAI) {
					Log.w(TAG, "AI " + aiFile + " is not valid.");
					report.logs.get(farmer).addSystemLog(entity, LeekLog.SERROR, "", LeekLog.NO_AI_EQUIPPED, null);
				}
				fight.addEntity(t, entity);
			}
			t++;
		}

		try {
			Log.i(TAG, "Start fight...");
			fight.startFight();
			fight.finishFight();

			report.fight = fight.getActions().toJSON();
			report.winner = fight.getWinner();

			// Save registers
			for (Entity entity : fight.getEntities().values()) {
				if (!entity.isSummon() && entity.getRegisters() != null && (entity.getRegisters().isModified() || entity.getRegisters().isNew())) {
					getRegisterManager().saveRegisters(entity.getId(), entity.getRegisters().toJSONString(), entity.getRegisters().isNew());
				}
			}

			return report;

			// System.out.println("SHA-1: " + Util.sha1(report.toString()));

			// Write to file
			// try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("../client/src/report.json"), "utf-8"))) {
			// 	writer.write(report.toString());
			// }
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void loadWeapons() {
		try {
			Log.start(TAG, "- Loading weapons... ");
			JSONObject weapons = JSON.parseObject(Util.readFile("data/weapons.json"));
			for (String id : weapons.keySet()) {
				JSONObject weapon = weapons.getJSONObject(id);
				Weapons.addWeapon(new Weapon(Integer.parseInt(id), (byte) 1, weapon.getInteger("cost"), weapon.getInteger("min_range"),
						weapon.getInteger("max_range"), weapon.getJSONArray("effects"), weapon.getByte("launch_type"), weapon.getByte("area"), weapon.getBoolean("los"),
						weapon.getInteger("template"), weapon.getString("name")));
			}
			Log.end(weapons.size() + " weapons loaded.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadChips() {
		try {
			Log.start(TAG, "- Loading chips... ");
			JSONObject chips = JSON.parseObject(Util.readFile("data/chips.json"));
			for (String id : chips.keySet()) {
				JSONObject chip = chips.getJSONObject(id);
				Chips.addChip(new Chip(Integer.parseInt(id), chip.getInteger("cost"), chip.getInteger("min_range"),
						chip.getInteger("max_range"), chip.getJSONArray("effects"), chip.getByte("launch_type"), chip.getByte("area"), chip.getBoolean("los"),
						chip.getInteger("cooldown"), chip.getBoolean("team_cooldown"), chip.getInteger("initial_cooldown"), chip.getInteger("level"),
						chip.getInteger("template"), chip.getString("name")));
			}
			Log.end(chips.size() + " chips loaded.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadSummons() {
		try {
			Log.start(TAG, "- Loading bulbs... ");
			JSONObject summons = JSON.parseObject(Util.readFile("data/summons.json"));
			for (String id : summons.keySet()) {
				JSONObject summon = summons.getJSONObject(id);
				Bulbs.addInvocationTemplate(new BulbTemplate(Integer.parseInt(id), summon.getString("name"), summon.getJSONArray("chips"), summon.getJSONObject("characteristics")));
			}
			Log.end(summons.size() + " summons loaded.");
		} catch (Exception e) {
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	public static RandomGenerator getRandom() {
		return randomGenerator;
	}
	public void setNocache(boolean nocache) {
		this.nocache = nocache;
	}
	public String getJar() {
		return jar;
	}
	public void setJar(String jar) {
		this.jar = jar;
	}

	public static void setRegisterManager(RegisterManager registerManager) {
		Generator.registerManager = registerManager;
	}
	public static RegisterManager getRegisterManager() {
		return registerManager;
	}
}
