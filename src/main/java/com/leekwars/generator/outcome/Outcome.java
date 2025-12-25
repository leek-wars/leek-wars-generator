package com.leekwars.generator.outcome;

import java.util.Map;
import java.util.TreeMap;

import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.action.Actions;
import com.leekwars.generator.statistics.StatisticsManager;
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
	public StatisticsManager statistics;
	/**
	 * Exception
	 */
	public Exception exception = null;

	public long analyzeTime = 0;

	public long compilationTime = 0;

	public long executionTime = 0;

	public ObjectNode toJson() {
		ObjectNode json = Json.createObject();
		ObjectNode logsJSON = Json.createObject();
		for (var entry : logs.entrySet()) {
			logsJSON.set(String.valueOf(entry.getKey()), entry.getValue().toJSON());
		}
		json.set("fight", fight.toJSON());
		json.set("logs", logsJSON);
		json.put("winner", winner);
		json.put("duration", duration);
		json.put("analyze_time", analyzeTime);
		json.put("compilation_time", compilationTime);
		json.put("execution_time", executionTime);
		return json;
	}

	public String toString() {
		return toJson().toString();
	}
}