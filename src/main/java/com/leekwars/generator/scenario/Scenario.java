package com.leekwars.generator.scenario;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.Log;
import com.leekwars.generator.Util;

public class Scenario {

	public static final String TAG = Scenario.class.getSimpleName();

	private static final FarmerInfo leekwarsFarmer = new FarmerInfo();
	static {
		leekwarsFarmer.id = 0;
		leekwarsFarmer.name = "Leek Wars";
		leekwarsFarmer.country = "fr";
	}

    public int seed = 0;
	public int maxTurns = 64;
	public int type = 0;
	public int context = 0;
	public int fightID = 0;
	public Map<Integer, FarmerInfo> farmers = new HashMap<Integer, FarmerInfo>();
	public Map<Integer, TeamInfo> teams = new HashMap<Integer, TeamInfo>();
	public List<List<EntityInfo>> entities = new ArrayList<List<EntityInfo>>();
	public JSONObject map = null;

	public Scenario() {
		// Between 1 and MAX_VALUE (included)
		this.seed = 1 + (int) ((Math.random() * System.nanoTime()) % (Integer.MAX_VALUE));
	}

    public static Scenario fromFile(File file) {

        Scenario scenario = new Scenario();

		JSONObject json = JSON.parseObject(Util.readFile(file));

		if (json.containsKey("random_seed")) {
            scenario.seed = json.getIntValue("random_seed");
        }
        if (json.containsKey("max_turns")) {
			scenario.maxTurns = json.getIntValue("max_turns");
		}
		for (Object farmerJson : json.getJSONArray("farmers")) {
			FarmerInfo farmer = new FarmerInfo();
			farmer.id = ((JSONObject) farmerJson).getInteger("id");
			farmer.name = ((JSONObject) farmerJson).getString("name");
			farmer.country = ((JSONObject) farmerJson).getString("country");
			scenario.farmers.put(farmer.id, farmer);
		}
		for (Object teamJson : json.getJSONArray("teams")) {
			TeamInfo team = new TeamInfo();
			team.id = ((JSONObject) teamJson).getInteger("id");
			team.name = ((JSONObject) teamJson).getString("name");
			scenario.teams.put(team.id, team);
		}
		for (Object teamJson : json.getJSONArray("entities")) {
            List<EntityInfo> team = new ArrayList<EntityInfo>();
			for (Object entityJson : (JSONArray) teamJson) {
				JSONObject e = (JSONObject) entityJson;
				EntityInfo entity = new EntityInfo(e);
				Log.i(TAG, "Created entity " + entity.name);
                team.add(entity);
			}
            scenario.entities.add(team);
        }
        return scenario;
	}

	public void addEntity(int teamID, EntityInfo entity) {
		if (entity == null || teamID < 0) {
			return;
		}
		while (entities.size() < teamID + 1) {
			entities.add(new ArrayList<EntityInfo>());
		}
		entities.get(teamID).add(entity);
	}

	public void setEntityAI(int team, int leek_id, String fullPath, int aiOwner) {
		for (EntityInfo entity : entities.get(team)) {
			if (entity.id == leek_id) {
				entity.ai = fullPath;
				entity.aiOwner = aiOwner;
			}
		}
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		JSONArray farmers = new JSONArray();
		for (FarmerInfo farmer : this.farmers.values()) {
			farmers.add(farmer.toJson());
		}
		json.put("farmers", farmers);
		JSONArray teams = new JSONArray();
		for (TeamInfo team : this.teams.values()) {
			teams.add(team.toJson());
		}
		json.put("teams", teams);
		JSONArray entities = new JSONArray();
		for (List<EntityInfo> list : this.entities) {
			JSONArray team = new JSONArray();
			for (EntityInfo entity : list) {
				team.add(entity.toJson());
			}
			entities.add(team);
		}
		json.put("entities", entities);
		return json;
	}

	@Override
	public String toString() {
		return toJson().toJSONString();
	}

	public FarmerInfo getFarmer(int farmer) {
		if (farmer == 0) {
			return leekwarsFarmer;
		} else {
			return this.farmers.get(farmer);
		}
	}
}