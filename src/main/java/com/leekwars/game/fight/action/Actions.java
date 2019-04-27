package com.leekwars.game.fight.action;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.game.attack.chips.Chip;
import com.leekwars.game.attack.weapons.Weapon;
import com.leekwars.game.fight.entity.Entity;
import com.leekwars.game.fight.entity.Summon;
import com.leekwars.game.maps.Cell;
import com.leekwars.game.maps.Map;

public class Actions {

	private final List<Action> actions;

	private final JSONArray leeks = new JSONArray();
	private final JSONObject map = new JSONObject();
	private final JSONArray team1 = new JSONArray();
	private final JSONArray team2 = new JSONArray();

	private int mNextEffectId = 0;

	public Actions() {
		this.actions = new ArrayList<Action>();
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

	public JSONObject toJSON() {

		JSONArray json = new JSONArray();

		for (Action log : actions) {
			json.add(log.getJSON());
		}
		JSONObject retour = new JSONObject();
		retour.put("leeks", leeks);
		retour.put("team1", team1);
		retour.put("team2", team2);
		retour.put("map", map);
		retour.put("actions", json);

		return retour;
	}

	public void addEntity(Entity entity, boolean validAI) {

		JSONObject object = new JSONObject();

		object.put("id", entity.getFId());
		object.put("level", entity.getLevel());
		object.put("skin", entity.getSkin());
		object.put("hat", (entity.getHat() > 0) ? entity.getHat() : null);

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
		object.put("cellPos", entity.getCell().getId());
		object.put("farmer", entity.getFarmer());
		object.put("type", entity.getType());

		object.put("summon", entity instanceof Summon);
		object.put("owner", entity.getOwnerId());

		if (entity.getTeam() == 0) {
			team1.add(entity.getFId());
		} else {
			team2.add(entity.getFId());
		}
		leeks.add(object);
	}

	public void addMap(Map map) {

		JSONObject obstacles = new JSONObject();
		for (int i = 0; i < (map.getWidth() * 2 - 1) * map.getHeight(); i++) {
			Cell c = map.getCell(i);
			if (c != null && !c.isWalkable() && c.getObstacle() != -1) {
				JSONArray infos = new JSONArray();
				infos.add(c.getObstacle());
				infos.add(c.getObstacleSize());
				obstacles.put(String.valueOf(c.getId()), infos);
			}
		}
		this.map.put("obstacles", obstacles);
		this.map.put("type", map.getType());
		this.map.put("width", map.getWidth());
		this.map.put("height", map.getWidth());
	}
}
