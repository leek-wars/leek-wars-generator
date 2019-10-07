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

    public long seed = 0;
    public int maxTurns = 64;
	public Map<Integer, FarmerInfo> farmers = new HashMap<Integer, FarmerInfo>();
	public Map<Integer, TeamInfo> teams = new HashMap<Integer, TeamInfo>();
	public List<List<EntityInfo>> entities = new ArrayList<List<EntityInfo>>();

    public static Scenario fromFile(File file) {

        Scenario scenario = new Scenario();

		JSONObject json = JSON.parseObject(Util.readFile(file));

		if (json.containsKey("random_seed")) {
            scenario.seed = json.getLongValue("random_seed");
        }
        if (json.containsKey("max_turns")) {
			scenario.maxTurns = json.getIntValue("max_turns");
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
		while (teams.size() < teamID + 1) {
			entities.add(new ArrayList<EntityInfo>());
		}
		entities.get(teamID).add(entity);
	}

	public void setTeamID(int teamID, int teamRealID) {
		while (teams.size() < teamID + 1) {
			entities.add(new ArrayList<EntityInfo>());
		}
		teams.get(teamID).id = teamRealID;
	}
}