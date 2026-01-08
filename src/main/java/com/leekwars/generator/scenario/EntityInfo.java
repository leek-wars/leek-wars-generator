package com.leekwars.generator.scenario;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.util.Json;
import com.leekwars.generator.chips.Chips;
import com.leekwars.generator.entity.Bulb;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.turret.Turret;
import com.leekwars.generator.weapons.Weapons;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.fight.Fight;

public class EntityInfo {

	static public final String TAG = EntityInfo.class.getSimpleName();

	static private final Class<?> classes[] = {
		Leek.class,
		Bulb.class,
		Turret.class
	};

	public int id;
	public String name;
	public String ai;
	public int ai_folder;
	public String ai_path;
	public int ai_version;
	public boolean ai_strict;
	public int aiOwner;
	public int type;
	public int farmer;
	public int team;
	public int level;
	public boolean dead;
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
	public int cores;
	public int ram;
	public List<Integer> chips = new ArrayList<Integer>();
	public List<Integer> weapons = new ArrayList<Integer>();
	public Integer cell;
	public int skin;
	public int hat;
	public boolean metal;
	public int face;
	public Class<?> customClass;
	public int orientation;

	public EntityInfo() {
	}

	public EntityInfo(ObjectNode e) {
		if (e.has("id")) {
			id = e.get("id").intValue();
		}
		name = e.get("name").asString();
		if (e.has("ai")) {
			ai = e.get("ai").asString();
		}
		if (e.has("ai_folder")) {
			ai_folder = e.get("ai_folder").intValue();
		}
		if (e.hasNonNull("ai_path")) {
			ai_path = e.get("ai_path").asString();
		}
		if (e.hasNonNull("ai_version")) {
			ai_version = e.get("ai_version").intValue();
		}
		if (e.hasNonNull("ai_strict")) {
			ai_strict = e.get("ai_strict").booleanValue();
		}
		if (e.hasNonNull("farmer")) {
			farmer = e.get("farmer").intValue();
		}
		if (e.hasNonNull("team")) {
			team = e.get("team").intValue();
		}
		level = e.get("level").intValue();
		life = e.get("life").intValue();
		tp = e.get("tp").intValue();
		mp = e.get("mp").intValue();
		strength = e.get("strength").intValue();
		if (e.has("agility")) {
			agility = e.get("agility").intValue();
		}
		if (e.has("frequency")) {
			frequency = e.get("frequency").intValue();
		}
		if (e.has("wisdom")) {
			wisdom = e.get("wisdom").intValue();
		}
		if (e.has("resistance")) {
			resistance = e.get("resistance").intValue();
		}
		if (e.has("science")) {
			science = e.get("science").intValue();
		}
		if (e.has("magic")) {
			magic = e.get("magic").intValue();
		}
		if (e.has("cores")) {
			cores = e.get("cores").intValue();
		}
		if (e.has("ram")) {
			ram = e.get("ram").intValue();
		}

		ArrayNode weapons = (ArrayNode) e.get("weapons");
		if (weapons != null) {
			for (var w : weapons) {
				this.weapons.add(w.intValue());
			}
		}
		ArrayNode chips = (ArrayNode) e.get("chips");
		if (chips != null) {
			for (var c : chips) {
				this.chips.add(c.intValue());
			}
		}
		if (e.hasNonNull("cell")) {
			cell = e.get("cell").intValue();
		}
	}

	public Entity createEntity(Generator generator, Scenario scenario, Fight fight) {

		Entity entity;
		try {
			var clazz = this.customClass != null ? this.customClass : classes[type];
			entity = (Entity) clazz.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			generator.exception(e, fight);
			return null;
		}
		entity.setId(id);
		entity.setName(name);
		entity.setLevel(level);
		entity.setTotalLife(life);
		entity.setLife(life);
		entity.setStrength(strength);
		entity.setAgility(agility);
		entity.setWisdom(wisdom);
		entity.setResistance(resistance);
		entity.setScience(science);
		entity.setMagic(magic);
		entity.setFrequency(frequency);
		entity.setCores(cores);
		entity.setRAM(ram);
		entity.setTP(tp);
		entity.setMP(mp);
		entity.setFarmer(farmer);
		entity.setDead(dead);
		entity.setOrientation(orientation);
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
		entity.setMetal(metal);
		entity.setFace(face);
		entity.setInitialCell(cell);

		for (Object w : weapons) {
			var weapon = Weapons.getWeapon((Integer) w);
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

	public ObjectNode toJson() {
		ObjectNode json = Json.createObject();
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
		json.put("cores", cores);
		json.put("ram", ram);
		json.put("tp", tp);
		json.put("mp", mp);
		json.put("farmer", farmer);
		json.put("team", team);
		json.put("ai", ai);
		json.put("ai_owner", aiOwner);
		ArrayNode weapons = Json.createArray();
		for (int weapon : this.weapons) {
			weapons.add(weapon);
		}
		json.set("weapons", weapons);
		ArrayNode chips = Json.createArray();
		for (int chip : this.chips) {
			chips.add(chip);
		}
		json.set("chips", chips);
		return json;
	}
}