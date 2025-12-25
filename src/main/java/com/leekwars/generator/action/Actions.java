package com.leekwars.generator.action;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.statistics.StatisticsManager;

public class Actions {

	private final List<Action> actions;

	private final List<Entity> entities = new ArrayList<Entity>();
	private final ArrayNode leeks = Json.createArray();
	public final ObjectNode map = Json.createObject();
	public ObjectNode dead = Json.createObject();
	public ObjectNode ops = Json.createObject();
	public ObjectNode times = Json.createObject();

	private int mNextEffectId = 0;

	public Actions() {
		this.actions = new ArrayList<Action>();
	}

	public Actions(Actions actions) {
		this.actions = new ArrayList<>(actions.actions);
	}

	public int getEffectId() {
		return mNextEffectId++;
	}

	public void log(Action log) {
		actions.add(log);
	}

	public int getNextId() {
		return actions.size();
	}

	public int currentID() {
		return actions.size() - 1;
	}

	public ObjectNode toJSON() {

		ArrayNode json = Json.createArray();

		for (Action log : actions) {
			json.add(log.getJSON());
		}
		ObjectNode retour = Json.createObject();
		retour.set("leeks", leeks);
		retour.set("map", map);
		retour.set("actions", json);
		retour.set("dead", dead);
		retour.set("ops", ops);

		return retour;
	}

	public void addOpsAndTimes(StatisticsManager statistics) {
		for (var entry : statistics.getOperationsByEntity().entrySet()) {
			ops.put(String.valueOf(entry.getKey()), entry.getValue());
		}
	}

	public void addEntity(Entity entity, boolean critical) {

		entities.add(entity);

		ObjectNode object = Json.createObject();

		object.put("id", entity.getFId());
		object.put("level", entity.getLevel());
		object.put("skin", entity.getSkin());
		object.put("hat", (entity.getHat() > 0) ? entity.getHat() : null);
		object.put("metal", entity.getMetal());
		object.put("face", entity.getFace());

		object.put("life", entity.getLife());
		object.put("strength", entity.getStat(Entity.CHARAC_STRENGTH));
		object.put("wisdom", entity.getStat(Entity.CHARAC_WISDOM));
		object.put("agility", entity.getStat(Entity.CHARAC_AGILITY));
		object.put("resistance", entity.getStat(Entity.CHARAC_RESISTANCE));
		object.put("frequency", entity.getStat(Entity.CHARAC_FREQUENCY));
		object.put("science", entity.getStat(Entity.CHARAC_SCIENCE));
		object.put("magic", entity.getStat(Entity.CHARAC_MAGIC));
		object.put("tp", entity.getTP());
		object.put("mp", entity.getMP());

		object.put("team", entity.getTeam() + 1);
		object.put("name", entity.getName());
		object.put("cellPos", entity.getCell() != null ? entity.getCell().getId() : null);
		object.put("farmer", entity.getFarmer());
		object.put("type", entity.getType());
		object.put("orientation", entity.getOrientation());

		object.put("summon", entity.isSummon());
		if (entity.isSummon()) {
			object.put("owner", entity.getSummoner().getFId());
			object.put("critical", critical);
		}

		leeks.add(object);
	}

	public void addMap(Map map) {

		ObjectNode obstacles = Json.createObject();
		for (int i = 0; i < (map.getWidth() * 2 - 1) * map.getHeight(); i++) {
			Cell c = map.getCell(i);
			if (c != null && !c.isWalkable() && c.getObstacleSize() > 0) {

				if (map.getId() != 0) {
					obstacles.put(String.valueOf(c.getId()), c.getObstacle());
				} else {
					ArrayNode obstacle = Json.createArray();
					obstacle.add(c.getObstacle());
					obstacle.add(c.getObstacleSize());

					if (map.isCustom()) {
						obstacles.set(String.valueOf(c.getId()), obstacle);
					} else {
						obstacles.put(String.valueOf(c.getId()), c.getObstacleSize());
					}
				}
			}
		}
		if (map.getId() != 0) {
			this.map.put("id", map.getId());
		}
		this.map.set("obstacles", obstacles);
		this.map.put("type", map.getType());
		this.map.put("width", map.getWidth());
		this.map.put("height", map.getWidth());
		if (map.getPattern() != null) {
			this.map.set("pattern", map.getPattern());
		}
	}
}
