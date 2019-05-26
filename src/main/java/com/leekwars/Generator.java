package com.leekwars;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.game.Util;
import com.leekwars.game.attack.chips.Chip;
import com.leekwars.game.attack.chips.Chips;
import com.leekwars.game.attack.weapons.Weapon;
import com.leekwars.game.attack.weapons.Weapons;
import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.entity.Entity;
import com.leekwars.game.fight.entity.EntityAI;
import com.leekwars.game.leek.Leek;
import com.leekwars.game.leek.LeekLog;
import com.leekwars.game.trophy.TrophyVariables;

import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.RandomGenerator;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class Generator {
	
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
		// System.out.println("Generator v1");
		String scenario = null;
		boolean nocache = false;

		for (String arg : args) {
			if (arg.startsWith("--")) {
				if (arg.substring(2).equals("nocache")) nocache = true;
			} else {
				scenario = arg;
			}
		}
		if (scenario == null) {
			System.out.println("No scenario file passed!");
			return;
		}
		// System.out.println("- Scenario : " + args[0]);
		
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions("com.leekwars.game.FightFunctions");
		LeekConstants.setExtraConstants("com.leekwars.game.FightConstants");
		LeekScript.setRandomGenerator(randomGenerator);
		loadWeapons();
		loadChips();
		
		runScenario(scenario, nocache);
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
				JSONArray weapons = e.getJSONArray("weapons");
				if (weapons != null) {
					for (Object w : weapons) {
						Integer weapon = (Integer) w;
						entity.addWeapon(Weapons.getWeapon(weapon));
					}
				}
				JSONArray chips = e.getJSONArray("chips");
				if (chips != null) {
					for (Object c : chips) {
						Integer chip = (Integer) c;
						entity.addChip(Chips.getChip(chip));
					}
				}
				try {
					String aiFile = e.getString("ai");
					if (aiFile != null) {
						// System.out.println("Compile AI " + aiFile + "...");
						EntityAI ai = (EntityAI) LeekScript.compileFile(aiFile, "com.leekwars.game.fight.entity.EntityAI", nocache);
						entity.setAI(ai);
						ai.setEntity(entity);
						int farmer = e.getIntValue("farmer");
						if (!logs.containsKey(farmer)) {
							logs.put(farmer, new LeekLog(entity));
						}
						ai.setLogs(logs.get(farmer));
					}
				} catch (LeekScriptException | LeekCompilerException e1) {
					e1.printStackTrace();
				}
				fight.addEntity(t, entity);

				fight.getTrophyManager().addFarmer(new TrophyVariables(entity.getFarmer()));
			}
			t++;
		}

		try {
			// System.out.println("Start fight...");
			fight.startFight();
			fight.finishFight();
			
			JSONObject report = new JSONObject();
			report.put("fight", fight.getActions().toJSON());
			
			JSONObject logsJSON = new JSONObject();
			for (Integer farmer : logs.keySet()) {
				logsJSON.put(String.valueOf(farmer), logs.get(farmer).toJSON());
			}
			report.put("logs", logsJSON);
			
			// System.out.println("Result:");
			System.out.println(report);
			
			// System.out.println("SHA-1: " + Util.sha1(report.toString()));
			
			// Write to file
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("../client/src/report.json"), "utf-8"))) {
				writer.write(report.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadWeapons() {
		try {
			// System.out.print("- Loading weapons... ");
			JSONObject weapons = JSON.parseObject(Util.readFile("data/weapons.json"));
			for (String id : weapons.keySet()) {
				JSONObject weapon = weapons.getJSONObject(id);
				Weapons.addWeapon(new Weapon(Integer.parseInt(id), (byte) 1, weapon.getInteger("cost"), weapon.getInteger("min_range"), 
						weapon.getInteger("max_range"), weapon.getJSONArray("effects"), weapon.getByte("launch_type"), weapon.getByte("area"), weapon.getBoolean("los"),
						weapon.getInteger("template"), weapon.getString("name")));
			}
			// System.out.println(weapons.size() + " weapons loaded.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadChips() {
		try {
			// System.out.print("- Loading chips... ");
			JSONObject chips = JSON.parseObject(Util.readFile("data/chips.json"));
			for (String id : chips.keySet()) {
				JSONObject chip = chips.getJSONObject(id);
				Chips.addChip(new Chip(Integer.parseInt(id), chip.getInteger("cost"), chip.getInteger("min_range"), 
						chip.getInteger("max_range"), chip.getJSONArray("effects"), chip.getByte("launch_type"), chip.getByte("area"), chip.getBoolean("los"),
						chip.getInteger("cooldown"), chip.getBoolean("team_cooldown"), chip.getInteger("initial_cooldown"), chip.getInteger("level"), 
						chip.getInteger("template"), chip.getString("name")));
			}
			// System.out.println(chips.size() + " chips loaded.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static RandomGenerator getRandom() {
		return randomGenerator;
	}
}
