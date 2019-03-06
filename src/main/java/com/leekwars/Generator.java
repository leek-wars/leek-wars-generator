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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.entity.Entity;
import com.leekwars.game.fight.entity.EntityAI;
import com.leekwars.game.leek.Leek;
import com.leekwars.game.leek.LeekLog;
import com.leekwars.game.trophy.TrophyVariables;

import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.LeekConstants;
import leekscript.runner.LeekFunctions;

public class Generator {

	static {
		new File("ai/").mkdir();
		LeekFunctions.setExtraFunctions("com.leekwars.game.FightFunctions");
		LeekConstants.setExtraConstants("com.leekwars.game.FightConstants");
	}

	public static void main(String[] args) {
		System.out.println("Generator v1");
		if (args.length < 1) {
			System.out.println("No scenario file passed!");
			return;
		}
		System.out.println("Scenario : " + args[0]);
		runScenario(args[0]);
	}

	private static void runScenario(String scenarioFile) {
		JSONObject json = null;
		try {
			String data = new String(Files.readAllBytes(Paths.get(scenarioFile)), StandardCharsets.UTF_8);
			json = (JSONObject) JSONObject.parse(data);
			System.out.println(json);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Fight fight = new Fight();
		JSONArray teams = json.getJSONArray("teams");
		int t = 0;
		int id = 0;
		for (Object team : teams) {
			for (Object entityJson : (JSONArray) team) {
				JSONObject e = (JSONObject) entityJson;
				Entity entity = new Leek(id++, 
					e.getString("name"),
					1212, // farmer
					e.getIntValue("level"), e.getIntValue("life"), e.getIntValue("tp"), e.getIntValue("mp"), e.getIntValue("strength"), e.getIntValue("agility"), e.getIntValue("frequency"),
					e.getIntValue("wisdom"), e.getIntValue("resistance"), e.getIntValue("science"), e.getIntValue("magic"),	e.getIntValue("skin"),
					1212, // team id
					"team",	1212, // ai id
					"ai", "farmer", "France", 0 /* hat */);
				try {
					EntityAI ai = (EntityAI) LeekScript.compileFile(e.getString("ai"), "com.leekwars.game.fight.entity.EntityAI");
					entity.setAI(ai);
					ai.setEntity(entity);
					ai.setLogs(new LeekLog());
				} catch (LeekScriptException | LeekCompilerException e1) {
					e1.printStackTrace();
				}
				fight.addEntity(t, entity);
			}
			t++;
		}
		fight.getTrophyManager().addFarmer(new TrophyVariables(1212));

		try {
			fight.initFight();
			fight.startFight();
			fight.finishFight();
			String report = fight.getJSON();
			System.out.println("Result:");
			System.out.println(report);
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("report.json"), "utf-8"))) {
			   writer.write(report);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
