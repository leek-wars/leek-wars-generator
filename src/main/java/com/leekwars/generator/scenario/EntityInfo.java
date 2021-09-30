package com.leekwars.generator.scenario;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.attack.weapons.Weapons;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Bulb;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.turret.Turret;
import com.leekwars.generator.leek.Leek;

public class EntityInfo {

	static public final String TAG = EntityInfo.class.getSimpleName();

	static private final Class<?> classes[] = { Leek.class, Bulb.class, Turret.class };

	public int id;
	public String name;
	public String ai;
	public int ai_folder;
	public String ai_path;
	public int aiOwner;
	public int type;
	public int farmer;
	public int team;
	public int level;
	public int life;
	public int tp;
	public int mp;
	public int strength;
	public int agility;
	public int frequency;
	public int wisdom;
	public int resistance;
	public int science;
	public int magic;
	public List<Integer> chips = new ArrayList<Integer>();
	public List<Integer> weapons = new ArrayList<Integer>();
	public int cell;
	public boolean static_;
	public int skin;
	public int hat;

	public EntityInfo() {
	}

	public EntityInfo(JSONObject e) {
		id = e.getIntValue("id");
		name = e.getString("name");
		ai = e.getString("ai");
		ai_folder = e.getIntValue("ai_folder");
		ai_path = e.getString("ai_path");
		farmer = e.getIntValue("farmer");
		team = e.getIntValue("team");
		level = e.getIntValue("level");
		life = e.getIntValue("life");
		tp = e.getIntValue("tp");
		mp = e.getIntValue("mp");
		strength = e.getIntValue("strength");
		agility = e.getIntValue("agility");
		frequency = e.getIntValue("frequency");
		wisdom = e.getIntValue("wisdom");
		resistance = e.getIntValue("resistance");
		science = e.getIntValue("science");
		magic = e.getIntValue("magic");
		static_ = e.getBooleanValue("static");

		JSONArray weapons = e.getJSONArray("weapons");
		if (weapons != null) {
			for (Object w : weapons) {
				this.weapons.add((Integer) w);
			}
		}
		JSONArray chips = e.getJSONArray("chips");
		if (chips != null) {
			for (Object c : chips) {
				this.chips.add((Integer) c);
			}
		}
		cell = e.getIntValue("cell");
	}

	public Entity createEntity(Generator generator, Scenario scenario, Fight fight) {

		Entity entity;
		try {
			entity = (Entity) classes[type].getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			generator.exception(e, fight);
			return null;
		}
		entity.setId(id);
		entity.setName(name);
		entity.setLevel(level);
		entity.setLife(life);
		entity.setStrength(strength);
		entity.setAgility(agility);
		entity.setWisdom(wisdom);
		entity.setResistance(resistance);
		entity.setScience(science);
		entity.setMagic(magic);
		entity.setFrequency(frequency);
		entity.setTP(tp);
		entity.setMP(mp);
		entity.setStatic(static_);
		entity.setFarmer(farmer);
		if (farmer >= 0) {
			entity.setFarmerName(scenario.getFarmer(farmer).name);
			entity.setFarmerCountry(scenario.getFarmer(farmer).country);
		}
		entity.setAIName(ai);
		entity.setTeamID(team);
		if (team > 0) {
			entity.setTeamName(scenario.teams.get(team).name);
		}
		entity.setSkin(skin);
		entity.setHat(hat);

		for (Object w : weapons) {
			Weapon weapon = Weapons.getWeapon((Integer) w);
			if (weapon == null) {
				Log.e(TAG, "No such weapon: " + w);
			} else {
				entity.addWeapon(weapon);
			}
		}
		for (Object c : chips) {
			Integer chip = (Integer) c;
			entity.addChip(Chips.getChip(chip));
		}

		return entity;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		json.put("level", level);
		json.put("strength", strength);
		json.put("agility", agility);
		json.put("wisdom", wisdom);
		json.put("resistance", resistance);
		json.put("science", science);
		json.put("magic", magic);
		json.put("frequency", frequency);
		json.put("tp", tp);
		json.put("mp", mp);
		json.put("static", static_);
		json.put("farmer", farmer);
		json.put("team", team);
		json.put("ai", ai);
		json.put("ai_owner", aiOwner);
		JSONArray weapons = new JSONArray();
		for (int weapon : this.weapons) {
			weapons.add(weapon);
		}
		json.put("weapons", weapons);
		JSONArray chips = new JSONArray();
		for (int chip : this.chips) {
			chips.add(chip);
		}
		json.put("chips", chips);
		return json;
	}
}