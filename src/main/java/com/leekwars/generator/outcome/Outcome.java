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

	/**
	 * Ce que setLoadout() laisse derrière lui, poireau par poireau : la répartition de
	 * capital avec laquelle le poireau a joué (null = la sienne, celle d'avant le combat)
	 * et si une potion de restat a été consommée pour y arriver.
	 *
	 * Le débit se fait poireau par poireau et non plus par éleveur : c'est la mémoire de
	 * CE poireau qui décide, et elle rend un lot de combats identiques gratuit après le
	 * premier.
	 */
	public static class LoadoutOutcome {
		public final int farmer;
		public final Map<Integer, Integer> capital;
		public final boolean restatCharged;

		public LoadoutOutcome(int farmer, Map<Integer, Integer> capital, boolean restatCharged) {
			this.farmer = farmer;
			this.capital = capital;
			this.restatCharged = restatCharged;
		}
	}

	/** État d'ensemble de chaque poireau joueur à la fin du combat, par id de poireau. */
	public Map<Integer, LoadoutOutcome> loadouts = new TreeMap<>();

	/**
	 * Potions consommées par éleveur — l'ancien débit, que `loadouts` remplace.
	 * Gardé tant que le worker de prod ne lit pas le nouveau champ : le générateur se
	 * déploie AVANT le serveur, et une version qui ne compile pas contre l'autre casse la
	 * chaîne de build du worker.
	 */
	@Deprecated
	public Map<Integer, Integer> restatPotionsConsumed = new TreeMap<>();

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