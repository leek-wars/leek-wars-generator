package com.leekwars;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.game.DbResolver;
import com.leekwars.game.Log;
import com.leekwars.game.Util;
import com.leekwars.game.attack.chips.Chip;
import com.leekwars.game.attack.chips.Chips;
import com.leekwars.game.attack.weapons.Weapon;
import com.leekwars.game.attack.weapons.Weapons;
import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.entity.Entity;
import com.leekwars.game.fight.entity.EntityAI;
import com.leekwars.game.fight.summons.SummonTemplate;
import com.leekwars.game.fight.summons.Summons;
import com.leekwars.game.leek.Leek;
import com.leekwars.game.leek.LeekLog;
import com.leekwars.game.trophy.TrophyVariables;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler;
import leekscript.compiler.LeekScript;
import leekscript.compiler.RandomGenerator;
import leekscript.functions.Functions;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class Generator {

	private static final String TAG = Generator.class.getSimpleName();
	
	static RandomGenerator randomGenerator = new RandomGenerator() {
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

	public static void main(String[] args) {
		String file = null;
		boolean nocache = false;
		boolean db_resolver = false;
		boolean verbose = false;
		boolean compile = false;
		int farmer = 0;

		for (String arg : args) {
			if (arg.startsWith("--")) {
				switch (arg.substring(2)) {
					case "nocache": nocache = true; break;
					case "dbresolver": db_resolver = true; break;
					case "verbose": verbose = true; break;
					case "compile": compile = true; break;
				}
				if (arg.startsWith("--farmer=")) {
					farmer = Integer.parseInt(arg.substring("--farmer=".length()));
				}
			} else {
				file = arg;
			}
		}
		Log.enable(verbose);
		Log.i(TAG, "Generator v1");
		if (file == null) {
			Log.i(TAG, "No scenario/ai file passed!");
			return;
		}
		
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions("com.leekwars.game.FightFunctions");
		LeekConstants.setExtraConstants("com.leekwars.game.FightConstants");
		LeekScript.setRandomGenerator(randomGenerator);
		if (db_resolver) {
			DbResolver dbResolver = new DbResolver("./resolver.php");
			dbResolver.setFarmer(farmer);
			LeekScript.setResolver(dbResolver);
		}
		loadWeapons();
		loadChips();
		loadSummons();
		loadFunctions();
		
		if (compile) {
			compileAI(file, nocache);
		} else {
			runScenario(file, nocache);
		}
	}

	private static void compileAI(String file, boolean nocache) {
		Log.i(TAG, "Compile AI " + file + "...");
		try {
			AIFile<?> ai = LeekScript.getResolver().resolve(file, null);
			long t = System.currentTimeMillis();
			String result = new IACompiler().analyze(ai);
			long time = System.currentTimeMillis() - t;
			Log.s(TAG, "Time: " + ((double) time / 1000) + " seconds");
			Log.s(TAG, "Analyze success!");
			System.out.println(result);
		} catch (Exception e1) {
			Log.e(TAG, "AI " + file + " not compiled");
			if (e1.getMessage() != null) {
				Log.e(TAG, e1.getMessage());
			}
			Log.e(TAG, "Compile failed!");
		}
	}

	private static void runScenario(String scenarioFile, boolean nocache) {
		JSONObject json = null;
		try {
			String data = new String(Files.readAllBytes(Paths.get(scenarioFile)), StandardCharsets.UTF_8);
			json = (JSONObject) JSONObject.parse(data);
			// System.out.println(json);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if (json.containsKey("random_seed")) {
			long seed = json.getLongValue("random_seed");
			randomGenerator.seed(seed);
		}
		
		Map<Integer, LeekLog> logs = new TreeMap<Integer, LeekLog>();

		Fight fight = new Fight();

		if (json.containsKey("max_turns")) {
			int max_turns = json.getIntValue("max_turns");
			fight.setMaxTurns(max_turns);
		}

		JSONArray teams = json.getJSONArray("teams");
		int t = 0;
		int id = 0;
		for (Object team : teams) {
			for (Object entityJson : (JSONArray) team) {
				JSONObject e = (JSONObject) entityJson;
				Entity entity = new Leek(id++, 
					e.getString("name"), e.getIntValue("farmer"),
					e.getIntValue("level"), e.getIntValue("life"), e.getIntValue("tp"), e.getIntValue("mp"), e.getIntValue("strength"), e.getIntValue("agility"), e.getIntValue("frequency"),
					e.getIntValue("wisdom"), e.getIntValue("resistance"), e.getIntValue("science"), e.getIntValue("magic"),	e.getIntValue("skin"),
					1212, // team id
					"team",	1212, // ai id
					"ai", "farmer", "France", 0 /* hat */);
				Log.i(TAG, "Create entity " + entity.getName());
				JSONArray weapons = e.getJSONArray("weapons");
				if (weapons != null) {
					for (Object w : weapons) {
						Weapon weapon = Weapons.getWeapon((Integer) w);
						if (weapon == null) {
							Log.e(TAG, "No such weapon: " + w);
							return;
						}
						entity.addWeapon(weapon);
					}
				}
				JSONArray chips = e.getJSONArray("chips");
				if (chips != null) {
					for (Object c : chips) {
						Integer chip = (Integer) c;
						entity.addChip(Chips.getChip(chip));
					}
				}
				int farmer = e.getIntValue("farmer");
				if (!logs.containsKey(farmer)) {
					logs.put(farmer, new LeekLog(entity));
				}
				String aiFile = e.getString("ai");
				boolean validAI = false;
				if (aiFile != null) {
					Log.i(TAG, "Compile AI " + aiFile + "...");
					((DbResolver) LeekScript.getResolver()).setFarmer(farmer);
					try {
						EntityAI ai = (EntityAI) LeekScript.compileFile(aiFile, "com.leekwars.game.fight.entity.EntityAI", "../../generator-v1/generator.jar", nocache);
						Log.i(TAG, "AI " + aiFile + " compiled!");
						entity.setAI(ai);
						ai.setEntity(entity);
						ai.setLogs(logs.get(farmer));
						validAI = true;
					} catch (Exception e1) {
						Log.w(TAG, "AI " + aiFile + " not compiled");
						Log.w(TAG, e1.getMessage());
					}
				}
				if (!validAI) {
					Log.w(TAG, "AI " + aiFile + " is not valid.");
					logs.get(farmer).addSystemLog(entity, LeekLog.SERROR, "", LeekLog.NO_AI_EQUIPPED, null);
				}
				fight.addEntity(t, entity);
				fight.getTrophyManager().addFarmer(new TrophyVariables(entity.getFarmer()));
			}
			t++;
		}

		try {
			Log.i(TAG, "Start fight...");
			fight.startFight();
			fight.finishFight();
			
			JSONObject report = new JSONObject();
			report.put("fight", fight.getActions().toJSON());
			report.put("winner", fight.getWinner());
			JSONObject logsJSON = new JSONObject();
			for (Integer farmer : logs.keySet()) {
				logsJSON.put(String.valueOf(farmer), logs.get(farmer).toJSON());
			}
			report.put("logs", logsJSON);
			
			System.out.println(report);
			
			// System.out.println("SHA-1: " + Util.sha1(report.toString()));
			
			// Write to file
			// try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("../client/src/report.json"), "utf-8"))) {
			// 	writer.write(report.toString());
			// }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadWeapons() {
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
	
	public static void loadChips() {
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

	public static void loadSummons() {
		try {
			Log.start(TAG, "- Loading summons... ");
			JSONObject summons = JSON.parseObject(Util.readFile("data/summons.json"));
			for (String id : summons.keySet()) {
				JSONObject summon = summons.getJSONObject(id);
				Summons.addInvocationTemplate(new SummonTemplate(Integer.parseInt(id), summon.getString("name"), summon.getJSONArray("chips"), summon.getJSONObject("characteristics")));
			}
			Log.end(summons.size() + " summons loaded.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void loadFunctions() {
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
}
