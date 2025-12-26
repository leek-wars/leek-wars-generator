package com.leekwars.generator.scenario;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.leekwars.generator.util.Json;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
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
	public int boss = 0;
	public Map<Integer, FarmerInfo> farmers = new HashMap<Integer, FarmerInfo>();
	public Map<Integer, TeamInfo> teams = new HashMap<Integer, TeamInfo>();
	public List<List<EntityInfo>> entities = new ArrayList<List<EntityInfo>>();
	public ObjectNode map = null;
	/**
	 * Si match nul, on regarde le poireau qui a le plus de vie (en absolu).
	 */
	public boolean drawCheckLife = false;

	public Scenario() {
		// Between 1 and MAX_VALUE (included)
		this.seed = 1 + (int) ((Math.random() * System.nanoTime()) % (Integer.MAX_VALUE));
	}

    public static Scenario fromFile(File file) {

        Scenario scenario = new Scenario();

		ObjectNode json = Json.parseObject(Util.readFile(file));

		if (json.has("random_seed")) {
            scenario.seed = json.get("random_seed").intValue();
        }
        if (json.has("max_turns")) {
			scenario.maxTurns = json.get("max_turns").intValue();
		}
		for (var farmerJson : (ArrayNode) json.get("farmers")) {
			FarmerInfo farmer = new FarmerInfo();
			farmer.id = ((ObjectNode) farmerJson).get("id").intValue();
			farmer.name = ((ObjectNode) farmerJson).get("name").asString();
			farmer.country = ((ObjectNode) farmerJson).get("country").asString();
			scenario.farmers.put(farmer.id, farmer);
		}
		for (var teamJson : (ArrayNode) json.get("teams")) {
			TeamInfo team = new TeamInfo();
			team.id = ((ObjectNode) teamJson).get("id").intValue();
			team.name = ((ObjectNode) teamJson).get("name").asString();
			scenario.teams.put(team.id, team);
		}
		for (var teamJson : (ArrayNode) json.get("entities")) {
            List<EntityInfo> team = new ArrayList<EntityInfo>();
			for (var entityJson : (ArrayNode) teamJson) {
				ObjectNode e = (ObjectNode) entityJson;
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

	public ObjectNode toJson() {
		ObjectNode json = Json.createObject();
		ArrayNode farmers = Json.createArray();
		for (FarmerInfo farmer : this.farmers.values()) {
			farmers.add(farmer.toJson());
		}
		json.set("farmers", farmers);
		ArrayNode teams = Json.createArray();
		for (TeamInfo team : this.teams.values()) {
			teams.add(team.toJson());
		}
		json.set("teams", teams);
		ArrayNode entities = Json.createArray();
		for (List<EntityInfo> list : this.entities) {
			ArrayNode team = Json.createArray();
			for (EntityInfo entity : list) {
				team.add(entity.toJson());
			}
			entities.add(team);
		}
		json.set("entities", entities);
		return json;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public FarmerInfo getFarmer(int farmer) {
		if (farmer == 0) {
			return leekwarsFarmer;
		} else {
			return this.farmers.get(farmer);
		}
	}

	public void setDrawCheckLife(boolean drawCheckLife) {
		this.drawCheckLife = drawCheckLife;
	}
}