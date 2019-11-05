package com.leekwars.generator.outcome;

import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.fight.action.Actions;
import com.leekwars.generator.fight.statistics.FightStatistics;
import com.leekwars.generator.leek.FarmerLog;

public class Outcome {
    /**
     * Fight: public data: entities, map, actions, flags, duration, ai times
     */
    public Actions fight;
    /**
     * Logs: debugs, marks, pauses
     */
    public Map<Integer, FarmerLog> logs = new TreeMap<Integer, FarmerLog>();
    /**
     * Winner team id
     */
	public int winner;
	/**
	 * Duration
	 */
	public int duration;
    /**
     * Fight statistics
     */
	public FightStatistics statistics;

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		JSONObject logsJSON = new JSONObject();
		for (Integer farmer : logs.keySet()) {
			logsJSON.put(String.valueOf(farmer), logs.get(farmer).toJSON());
		}
		json.put("fight", fight.toJSON());
		json.put("logs", logsJSON);
		json.put("winner", winner);
		json.put("duration", duration);
		json.put("statistics", statistics.toJson());
		return json;
	}

	public String toString() {
		return toJson().toJSONString();
	}
}