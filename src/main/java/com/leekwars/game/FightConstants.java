package com.leekwars.game;

import com.leekwars.game.attack.effect.Effect;
import com.leekwars.game.fight.Fight;

import leekscript.runner.LeekFunctions;

public class FightConstants {

	public final static int MAX_TURNS = Fight.MAX_TURNS;

	public final static int CELL_EMPTY = 0;
	public final static int CELL_PLAYER = 1;
	public final static int CELL_OBSTACLE = 2;

	// Type entity
	public final static int ENTITY_LEEK = 1;
	public final static int ENTITY_BULB = 2;

	// Constants
	public final static int EFFECT_TARGET_ALLIES = Effect.TARGET_ALLIES;
	public final static int EFFECT_TARGET_ENEMIES = Effect.TARGET_ENEMIES;
	public final static int EFFECT_TARGET_CASTER = Effect.TARGET_CASTER;
	public final static int EFFECT_TARGET_NOT_CASTER = 0;
	public final static int EFFECT_TARGET_NON_SUMMONS = Effect.TARGET_NON_SUMMONS;
	public final static int EFFECT_TARGET_SUMMONS = Effect.TARGET_SUMMONS;

	public final static int EFFECT_DAMAGE = 1;
	public final static int EFFECT_HEAL = 2;
	public final static int EFFECT_FORCE = 3;
	public final static int EFFECT_AGILITY = 4;
	public final static int EFFECT_RELATIVE_SHIELD = 5;
	public final static int EFFECT_ABSOLUTE_SHIELD = 6;
	public final static int EFFECT_MP = 7;
	public final static int EFFECT_TP = 8;
	public final static int EFFECT_TELEPORT = 10;
	public final static int EFFECT_INVERT = Effect.TYPE_PERMUTATION;
	public final static int EFFECT_RESURRECT = Effect.TYPE_RESURRECT;

	public final static int EFFECT_BUFF_DAMAGE = 1;
	public final static int EFFECT_BUFF_HEAL = 2;
	public final static int EFFECT_BUFF_FORCE = 3;
	public final static int EFFECT_BUFF_STRENGTH = 3;
	public final static int EFFECT_BUFF_AGILITY = 4;
	public final static int EFFECT_BUFF_RELATIVE_SHIELD = 5;
	public final static int EFFECT_BUFF_ABSOLUTE_SHIELD = 6;
	public final static int EFFECT_BUFF_MP = 7;
	public final static int EFFECT_BUFF_TP = 8;
	public final static int EFFECT_BUFF_DEBUFF = 9;
	public final static int EFFECT_BUFF_TELEPORTATION = 10;
	public final static int EFFECT_BOOST_MAX_LIFE = Effect.TYPE_VITALITY;
	public final static int EFFECT_POISON = Effect.TYPE_POISON;
	public final static int EFFECT_SUMMON = Effect.TYPE_SUMMON;
	public final static int EFFECT_DEBUFF = Effect.TYPE_DEBUFF;
	public final static int EFFECT_KILL = Effect.TYPE_KILL;
	public final static int EFFECT_SHACKLE_MP = Effect.TYPE_SHACKLE_MP;
	public final static int EFFECT_SHACKLE_TP = Effect.TYPE_SHACKLE_TP;
	public final static int EFFECT_SHACKLE_STRENGTH = Effect.TYPE_SHACKLE_STRENGTH;
	public final static int EFFECT_DAMAGE_RETURN = Effect.TYPE_DAMAGE_RETURN;
	public final static int EFFECT_BUFF_RESISTANCE = Effect.TYPE_BUFF_RESISTANCE;
	public final static int EFFECT_BUFF_WISDOM = Effect.TYPE_BUFF_WISDOM;
	public final static int EFFECT_SHACKLE_MAGIC = Effect.TYPE_SHACKLE_MAGIC;
	public final static int EFFECT_ANTIDOTE = Effect.TYPE_ANTIDOTE;
	public final static int EFFECT_AFTEREFFECT = Effect.TYPE_AFTEREFFECT;
	public final static int EFFECT_VULNERABILITY = Effect.TYPE_VULNERABILITY;

	// Résultats attaque
	public final static int USE_CRITICAL = 2;
	public final static int USE_SUCCESS = 1;
	public final static int USE_FAILED = 0;
	public final static int USE_INVALID_TARGET = -1;
	public final static int USE_NOT_ENOUGH_TP = -2;
	public final static int USE_INVALID_COOLDOWN = -3;
	public final static int USE_INVALID_POSITION = -4;
	public final static int USE_TOO_MUCH_SUMMONS = -5;
	public final static int USE_TOO_MANY_SUMMONS = -5;
	public final static int USE_RESURRECT_INVALID_ENTIITY = -6;

	// Armes
	public final static int WEAPON_PISTOL = 37;
	public final static int WEAPON_MACHINE_GUN = 38;
	public final static int WEAPON_DOUBLE_GUN = 39;
	public final static int WEAPON_SHOTGUN = 41;
	public final static int WEAPON_MAGNUM = 45;
	public final static int WEAPON_LASER = 42;
	public final static int WEAPON_GRENADE_LAUNCHER = 43;
	public final static int WEAPON_FLAME_THROWER = 46;
	public final static int WEAPON_DESTROYER = 40;
	public final static int WEAPON_GAZOR = 48;
	public final static int WEAPON_ELECTRISOR = 44;
	public final static int WEAPON_M_LASER = 47;
	public final static int WEAPON_B_LASER = 60;
	public final static int WEAPON_KATANA = 107;
	public final static int WEAPON_BROADSWORD = 108;
	public final static int WEAPON_AXE = 109;

	// Messages
	public final static int MESSAGE_HEAL = 1;
	public final static int MESSAGE_ATTACK = 2;
	public final static int MESSAGE_DEBUFF = 3;
	public final static int MESSAGE_SHIELD = 4;
	public final static int MESSAGE_BUFF_MP = 5;
	public final static int MESSAGE_BUFF_TP = 6;
	public final static int MESSAGE_BUFF_FORCE = 7;
	public final static int MESSAGE_BUFF_STRENGTH = 7;
	public final static int MESSAGE_BUFF_AGILITY = 8;
	public final static int MESSAGE_MOVE_TOWARD = 9;
	public final static int MESSAGE_MOVE_AWAY = 10;
	public final static int MESSAGE_MOVE_TOWARD_CELL = 11;
	public final static int MESSAGE_MOVE_AWAY_CELL = 12;
	public final static int MESSAGE_CUSTOM = 13;

	// Area
	public final static int AREA_POINT = 1;// Une seule cellule
	public final static int AREA_LASER_LINE = 2;
	public final static int AREA_CIRCLE_1 = 3;
	public final static int AREA_CIRCLE_2 = 4;
	public final static int AREA_CIRCLE_3 = 5;

	// Chips
	public final static int CHIP_BANDAGE = 3;
	public final static int CHIP_CURE = 4;
	public final static int CHIP_DRIP = 10;
	public final static int CHIP_REGENERATION = 35;
	public final static int CHIP_VACCINE = 11;
	public final static int CHIP_SHOCK = 1;
	public final static int CHIP_FLASH = 6;
	public final static int CHIP_LIGHTNING = 33;
	public final static int CHIP_SPARK = 18;
	public final static int CHIP_FLAME = 5;
	public final static int CHIP_METEORITE = 36;
	public final static int CHIP_PEBBLE = 19;
	public final static int CHIP_ROCK = 7;
	public final static int CHIP_ROCKFALL = 32;
	public final static int CHIP_ICE = 2;
	public final static int CHIP_STALACTITE = 30;
	public final static int CHIP_ICEBERG = 31;
	public final static int CHIP_SHIELD = 20;
	public final static int CHIP_HELMET = 21;
	public final static int CHIP_ARMOR = 22;
	public final static int CHIP_WALL = 23;
	public final static int CHIP_RAMPART = 24;
	public final static int CHIP_FORTRESS = 29;
	public final static int CHIP_PROTEIN = 8;
	public final static int CHIP_STEROID = 25;
	public final static int CHIP_DOPING = 26;
	public final static int CHIP_STRETCHING = 9;
	public final static int CHIP_WARM_UP = 27;
	public final static int CHIP_REFLEXES = 28;
	public final static int CHIP_LEATHER_BOOTS = 14;
	public final static int CHIP_WINGED_BOOTS = 12;
	public final static int CHIP_SEVEN_LEAGUE_BOOTS = 13;
	public final static int CHIP_MOTIVATION = 15;
	public final static int CHIP_ADRENALINE = 16;
	public final static int CHIP_RAGE = 17;
	public final static int CHIP_LIBERATION = 34;
	public final static int CHIP_TELEPORTATION = 59;
	public final static int CHIP_ARMORING = 67;
	public final static int CHIP_INVERSION = 68;
	public final static int CHIP_PUNY_BULB = 73;
	public final static int CHIP_FIRE_BULB = 74;
	public final static int CHIP_HEALER_BULB = 75;
	public final static int CHIP_ROCKY_BULB = 76;
	public final static int CHIP_ICED_BULB = 77;
	public final static int CHIP_LIGHTNING_BULB = 78;
	public final static int CHIP_METALLIC_BULB = 79;
	public final static int CHIP_REMISSION = 80;
	public final static int CHIP_CARAPACE = 81;
	public final static int CHIP_RESURRECTION = 84;
	public final static int CHIP_DEVIL_STRIKE = 85;
	public final static int CHIP_WHIP = 88;
	public final static int CHIP_LOAM = 89;
	public final static int CHIP_FERTILIZER = 90;
	public final static int CHIP_ACCELERATION = 91;
	public final static int CHIP_SLOW_DOWN = 92;
	public final static int CHIP_BALL_AND_CHAIN = 93;
	public final static int CHIP_TRANQUILIZER = 94;
	public final static int CHIP_SOPORIFIC = 95;
	public final static int CHIP_SOLIDIFICATION = 96;
	public final static int CHIP_VENOM = 97;
	public final static int CHIP_TOXIN = 98;
	public final static int CHIP_PLAGUE = 99;
	public final static int CHIP_THORN = 100;
	public final static int CHIP_MIRROR = 101;
	public final static int CHIP_FEROCITY = 102;
	public final static int CHIP_COLLAR = 103;
	public final static int CHIP_BARK = 104;
	public final static int CHIP_BURNING = 105;
	public final static int CHIP_FRACTURE = 106;
	public final static int CHIP_ANTIDOTE = 110;
	
	public final static int MAP_NEXUS = 1;
	public final static int MAP_FACTORY = 2;
	public final static int MAP_DESERT = 3;
	public final static int MAP_FOREST = 4;
	public final static int MAP_GLACIER = 5;
	public final static int MAP_BEACH = 6;

	public final static int FIGHT_TYPE_SOLO = Fight.TYPE_SOLO;
	public final static int FIGHT_TYPE_FARMER = Fight.TYPE_FARMER;
	public final static int FIGHT_TYPE_TEAM = Fight.TYPE_TEAM;
	public final static int FIGHT_TYPE_BATTLE_ROYALE = Fight.TYPE_BATTLE_ROYALE;

	public final static int FIGHT_CONTEXT_TEST = Fight.CONTEXT_TEST;
	public final static int FIGHT_CONTEXT_GARDEN = Fight.CONTEXT_GARDEN;
	public final static int FIGHT_CONTEXT_CHALLENGE = Fight.CONTEXT_CHALLENGE;
	public final static int FIGHT_CONTEXT_TOURNAMENT = Fight.CONTEXT_TOURNAMENT;
	public final static int FIGHT_CONTEXT_BATTLE_ROYALE = Fight.CONTEXT_BATTLE_ROYALE;

	public static int getType(String constant) {

		if (constant.equals("FIGHT_CONTEXT_TEST")
				|| constant.equals("FIGHT_CONTEXT_GARDEN")
				|| constant.equals("FIGHT_CONTEXT_CHALLENGE")
				|| constant.equals("FIGHT_CONTEXT_TOURNAMENT")
				|| constant.equals("FIGHT_CONTEXT_BATTLE_ROYALE")
				|| constant.equals("FIGHT_TYPE_SOLO")
				|| constant.equals("FIGHT_TYPE_FARMER")
				|| constant.equals("FIGHT_TYPE_TEAM")
				|| constant.equals("FIGHT_TYPE_BATTLE_ROYALE")
				|| constant.equals("INSTRUCTIONS_LIMIT")
				|| constant.equals("CELL_PLAYER")
				|| constant.equals("CELL_EMPTY")
				|| constant.equals("CELL_OBSTACLE")
				|| constant.equals("MESSAGE_CUSTOM")
				|| constant.equals("MESSAGE_MOVE_AWAY_CELL")
				|| constant.equals("MESSAGE_MOVE_TOWARD_CELL")
				|| constant.equals("MESSAGE_MOVE_AWAY")
				|| constant.equals("MESSAGE_MOVE_TOWARD")
				|| constant.equals("MESSAGE_BUFF_AGILITY")
				|| constant.equals("MESSAGE_BUFF_FORCE")
				|| constant.equals("MESSAGE_BUFF_STRENGTH")
				|| constant.equals("MESSAGE_BUFF_TP")
				|| constant.equals("MESSAGE_BUFF_MP")
				|| constant.equals("MESSAGE_SHIELD")
				|| constant.equals("MESSAGE_HEAL")
				|| constant.equals("MESSAGE_ATTACK")
				|| constant.equals("MESSAGE_DEBUFF")
				||
				// Effect Target
				constant.equals("EFFECT_TARGET_SUMMONS")
				|| constant.equals("EFFECT_TARGET_ALLIES")
				|| constant.equals("EFFECT_TARGET_ENEMIES")
				|| constant.equals("EFFECT_TARGET_CASTER")
				|| constant.equals("EFFECT_TARGET_NOT_CASTER")
				|| constant.equals("EFFECT_TARGET_NON_SUMMONS")
				||
				// Effets
				constant.equals("EFFECT_POISON")
				|| constant.equals("EFFECT_BOOST_MAX_LIFE")
				|| constant.equals("EFFECT_DAMAGE")
				|| constant.equals("EFFECT_HEAL")
				|| constant.equals("EFFECT_FORCE")
				|| constant.equals("EFFECT_AGILITY")
				|| constant.equals("EFFECT_ABSOLUTE_SHIELD")
				|| constant.equals("EFFECT_RELATIVE_SHIELD")
				|| constant.equals("EFFECT_MP")
				|| constant.equals("EFFECT_TP")
				|| constant.equals("EFFECT_DEBUFF")
				|| constant.equals("USE_TOO_MUCH_SUMMONS")
				|| constant.equals("USE_TOO_MANY_SUMMONS")
				|| constant.equals("USE_INVALID_COOLDOWN")
				|| constant.equals("USE_INVALID_POSITION")
				|| constant.equals("USE_NOT_ENOUGH_TP")
				|| constant.equals("USE_INVALID_TARGET")
				|| constant.equals("USE_FAILED")
				|| constant.equals("USE_CRITICAL")
				|| constant.equals("USE_SUCCESS")
				|| constant.equals("USE_RESURRECT_INVALID_ENTIITY")
				|| constant.equals("EFFECT_TELEPORT")
				|| constant.equals("EFFECT_SUMMON")
				||

				// Summon
				constant.equals("CHIP_DEVIL_STRIKE")
				|| constant.equals("CHIP_CARAPACE")
				|| constant.equals("CHIP_REMISSION")
				|| constant.equals("CHIP_PUNY_BULB")
				|| constant.equals("CHIP_FIRE_BULB")
				|| constant.equals("CHIP_HEALER_BULB")
				|| constant.equals("CHIP_LIGHTNING_BULB")
				|| constant.equals("CHIP_METALLIC_BULB")
				|| constant.equals("CHIP_ICED_BULB")
				|| constant.equals("CHIP_ROCKY_BULB")
				||

				// Type entity
				constant.equals("ENTITY_LEEK")
				|| constant.equals("ENTITY_BULB")
				|| constant.equals("OPERATIONS_LIMIT")
				||

				// Effect buff
				constant.equals("EFFECT_RESURRECT")
				|| constant.equals("EFFECT_INVERT")
				|| constant.equals("EFFECT_BUFF_STRENGTH")
				|| constant.equals("EFFECT_BUFF_DAMAGE")
				|| constant.equals("EFFECT_BUFF_HEAL")
				|| constant.equals("EFFECT_BUFF_FORCE")
				|| constant.equals("EFFECT_BUFF_AGILITY")
				|| constant.equals("EFFECT_BUFF_ABSOLUTE_SHIELD")
				|| constant.equals("EFFECT_BUFF_RELATIVE_SHIELD")
				|| constant.equals("EFFECT_BUFF_MP")
				|| constant.equals("EFFECT_BUFF_TP")
				|| constant.equals("EFFECT_DEBUFF")
				|| constant.equals("EFFECT_KILL")
				|| constant.equals("EFFECT_SHACKLE_MP")
				|| constant.equals("EFFECT_SHACKLE_TP")
				|| constant.equals("EFFECT_SHACKLE_STRENGTH")
				|| constant.equals("EFFECT_DAMAGE_RETURN")
				|| constant.equals("EFFECT_BUFF_RESISTANCE")
				|| constant.equals("EFFECT_BUFF_WISDOM")
				|| constant.equals("EFFECT_SHACKLE_MAGIC")
				|| constant.equals("EFFECT_ANTIDOTE")
				|| constant.equals("EFFECT_AFTEREFFECT")
				|| constant.equals("EFFECT_VULNERABILITY")
				||
				// Area
				constant.equals("AREA_POINT")
				|| constant.equals("AREA_LASER_LINE")
				|| constant.equals("AREA_CIRCLE_1")
				|| constant.equals("AREA_CIRCLE_2")
				|| constant.equals("AREA_CIRCLE_3")
				||
				// Sort

				constant.equals("MAX_TURNS") || constant.equals("WEAPON_PISTOL") || constant.equals("WEAPON_MACHINE_GUN")
				|| constant.equals("WEAPON_DOUBLE_GUN") || constant.equals("WEAPON_SHOTGUN") || constant.equals("WEAPON_MAGNUM") || constant.equals("WEAPON_LASER")
				|| constant.equals("WEAPON_GRENADE_LAUNCHER") || constant.equals("WEAPON_FLAME_THROWER") || constant.equals("WEAPON_DESTROYER") || constant.equals("WEAPON_GAZOR")
				|| constant.equals("WEAPON_ELECTRISOR") || constant.equals("WEAPON_M_LASER") || constant.equals("CHIP_BANDAGE") || constant.equals("CHIP_CURE") || constant.equals("CHIP_DRIP")
				|| constant.equals("CHIP_RESURRECTION") || constant.equals("CHIP_VACCINE") || constant.equals("CHIP_SHOCK") || constant.equals("CHIP_FLASH") || constant.equals("CHIP_LIGHTNING")
				|| constant.equals("CHIP_SPARK") || constant.equals("CHIP_FLAME") || constant.equals("CHIP_METEORITE") || constant.equals("CHIP_PEBBLE") || constant.equals("CHIP_ROCK")
				|| constant.equals("CHIP_ROCKFALL") || constant.equals("CHIP_ICE") || constant.equals("CHIP_STALACTITE") || constant.equals("CHIP_ICEBERG") || constant.equals("CHIP_SHIELD")
				|| constant.equals("CHIP_HELMET") || constant.equals("CHIP_ARMOR") || constant.equals("CHIP_WALL") || constant.equals("CHIP_RAMPART") || constant.equals("CHIP_FORTRESS")
				|| constant.equals("CHIP_PROTEIN") || constant.equals("CHIP_STEROID") || constant.equals("CHIP_DOPING") || constant.equals("CHIP_STRETCHING") || constant.equals("CHIP_WARM_UP")
				|| constant.equals("CHIP_REFLEXES") || constant.equals("CHIP_LEATHER_BOOTS") || constant.equals("CHIP_WINGED_BOOTS") || constant.equals("CHIP_SEVEN_LEAGUE_BOOTS")
				|| constant.equals("CHIP_MOTIVATION") || constant.equals("CHIP_ADRENALINE") || constant.equals("CHIP_RAGE") || constant.equals("CHIP_LIBERATION")
				|| constant.equals("CHIP_TELEPORTATION") || constant.equals("WEAPON_B_LASER") || constant.equals("CHIP_ARMORING") || constant.equals("CHIP_INVERSION")
				|| constant.equals("CHIP_REGENERATION") || constant.equals("CHIP_WHIP") || constant.equals("CHIP_LOAM") || constant.equals("CHIP_ACCELERATION") || constant.equals("CHIP_FERTILIZER")
				|| constant.equals("CHIP_SLOW_DOWN") || constant.equals("CHIP_BALL_AND_CHAIN") || constant.equals("CHIP_TRANQUILIZER") || constant.equals("CHIP_SOPORIFIC")
				|| constant.equals("CHIP_SOLIDIFICATION") || constant.equals("CHIP_VENOM") || constant.equals("CHIP_TOXIN") || constant.equals("CHIP_PLAGUE") || constant.equals("CHIP_THORN")
				|| constant.equals("CHIP_MIRROR") || constant.equals("CHIP_FEROCITY") || constant.equals("CHIP_COLLAR") || constant.equals("CHIP_BARK") || constant.equals("CHIP_BURNING")
				|| constant.equals("CHIP_FRACTURE") || constant.equals("CHIP_ANTIDOTE") || constant.equals("WEAPON_AXE")
				|| constant.equals("WEAPON_BROADSWORD") || constant.equals("WEAPON_KATANA")
				||

				// Map
				constant.equals("MAP_NEXUS") || constant.equals("MAP_FACTORY") || constant.equals("MAP_DESERT") || constant.equals("MAP_FOREST") || constant.equals("MAP_GLACIER")
				|| constant.equals("MAP_BEACH")

		) {
			return LeekFunctions.INT;
		}
		return 0;
	}
}
