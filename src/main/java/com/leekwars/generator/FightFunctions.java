package com.leekwars.generator;

import java.util.HashMap;
import java.util.Map;

import leekscript.common.Type;
import leekscript.runner.CallableVersion;
import leekscript.runner.LeekFunctions;

public class FightFunctions {

	private static HashMap<String, LeekFunctions> functions = new HashMap<>();

	static {

		method("getLife", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getForce", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getStrength", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getAgility", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getScience", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getWisdom", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getResistance", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getMagic", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getCell", "Entity", 5, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getWeapon", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getName", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.STRING),
		});
		method("getTP", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getMP", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getTotalTP", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getTotalMP", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getTotalLife", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getPower", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getAbsoluteShield", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getRelativeShield", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getDamageReturn", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("setWeapon", "Entity", 15, true, Type.BOOL, new Type[] { Type.INT });
		method("say", "Entity", 30, true, Type.BOOL, new Type[] { Type.STRING });
		method("lama", "Entity", 30, true, Type.VOID, new Type[0]);
		method("listen", "Entity", 78, true, Type.ARRAY, new Type[0]);
		method("getWeapons", "Entity", 50, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ARRAY),
		});
		method("isEnemy", "Entity", 15, true, Type.BOOL, new Type[] { Type.INT });
		method("isAlly", "Entity", 15, true, Type.BOOL, new Type[] { Type.INT });
		method("isAlive", "Entity", 15, true, Type.BOOL, new Type[] { Type.INT });
		method("isDead", "Entity", 15, true, Type.BOOL, new Type[] { Type.INT });
		method("getLeek", "Entity", 5, true, Type.INT, new Type[0]).setMaxVersion(3);
		method("getEntity", "Entity", 5, true, Type.INT, new Type[0]);
		method("getChips", "Entity", 40, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ARRAY),
		});
		method("getEffects", "Entity", 25, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ARRAY),
		});
		method("getLaunchedEffects", "Entity", 25, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getPassiveEffects", "Entity", 125, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getLevel", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getFrequency", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getCores", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getTeamName", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getFarmerName", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getFarmerCountry", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getTeamID", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getFarmerID", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getAIName", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.STRING),
		});
		method("getAIID", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getSummons", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ARRAY),
		});
		method("getType", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("isSummon", "Entity", 10, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.BOOL),
		});
		method("getSummoner", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("isStatic", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getBirthTurn", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getEntityTurnOrder", "Entity", 30, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getLeekID", "Entity", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});

		/**
		 * Weapon
		 */
		method("useWeapon", "Weapon", 3000, true, Type.INT, new Type[] { Type.INT });
		method("useWeaponOnCell", "Weapon", 3000, true, Type.INT, new Type[] { Type.INT });
		method("getWeaponMinScope", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT),
		}).setMaxVersion(3);
		method("getWeaponMaxScope", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT),
		}).setMaxVersion(3);
		method("getWeaponMinRange", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT),
		});
		method("getWeaponMaxRange", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT),
		});
		method("getWeaponLaunchType", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.INT }),
			new CallableVersion(Type.ANY),
		});
		method("getWeaponCost", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT),
		});
		method("getWeaponEffects", "Weapon", 125, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.INT }),
			new CallableVersion(Type.ANY),
		});
		method("getWeaponPassiveEffects", "Weapon", 125, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.INT }),
			new CallableVersion(Type.ANY),
		});
		method("getWeaponName", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.STRING, new Type[] { Type.INT }),
			new CallableVersion(Type.STRING),
		});
		method("isInlineWeapon", "Weapon", 10, true, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.INT }),
			new CallableVersion(Type.BOOL),
		});
		method("weaponNeedLos", "Weapon", 10, true, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.INT }),
			new CallableVersion(Type.BOOL),
		});
		method("canUseWeapon", "Weapon", 45, true, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY }),
		});
		method("canUseWeaponOnCell", "Weapon", 45, true, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY }),
		});
		method("getWeaponTargets", "Weapon", 40, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("getWeaponFailure", "Weapon", 15, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
			new CallableVersion(Type.INT, new Type[] { }),
		});
		method("isWeapon", "Weapon", 15, true, Type.BOOL, new Type[] { Type.INT });
		method("getWeaponArea", "Weapon", 15, true, Type.ANY, new Type[] { Type.INT });
		method("getWeaponEffectiveArea", "Weapon", 78, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("getAllWeapons", "Weapon", 200, true, Type.ARRAY, new Type[0]);

		/**
		 * Chip
		 */
		method("getCurrentCooldown", "Chip", 30, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		}).setMaxVersion(3);
		method("getCooldown", "Chip", 30, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY }),
		});
		method("useChip", "Chip", 3000, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("useChipOnCell", "Chip", 3000, true, Type.INT, new Type[] { Type.INT, Type.INT });
		method("getChipName", "Chip", 15, true, Type.STRING, new Type[] { Type.INT });
		method("getChipMinScope", "Chip", 15, true, Type.ANY, new Type[] { Type.INT });
		method("getChipMaxScope", "Chip", 15, true, Type.ANY, new Type[] { Type.INT });
		method("getChipMinRange", "Chip", 15, true, Type.ANY, new Type[] { Type.INT });
		method("getChipMaxRange", "Chip", 15, true, Type.ANY, new Type[] { Type.INT });
		method("getChipCost", "Chip", 15, true, Type.ANY, new Type[] { Type.INT });
		method("getChipEffects", "Chip", 125, true, Type.ANY, new Type[] { Type.INT });
		method("isInlineChip", "Chip", 10, true, Type.BOOL, new Type[] { Type.INT });
		method("chipNeedLos", "Chip", 10, true, Type.BOOL, new Type[] { Type.INT });
		method("getChipCooldown", "Chip", 15, true, Type.INT, new Type[] { Type.INT });
		method("canUseChip", "Chip", 45, true, Type.BOOL, new Type[] { Type.INT, Type.INT });
		method("canUseChipOnCell", "Chip", 45, true, Type.BOOL, new Type[] { Type.INT, Type.INT });
		method("getChipTargets", "Chip", 40, true, Type.ANY, new Type[] { Type.INT, Type.INT });
		method("getChipFailure", "Chip", 15, true, Type.INT, new Type[] { Type.INT });
		method("isChip", "Chip", 10, true, Type.BOOL, new Type[] { Type.INT });
		method("getChipLaunchType", "Chip", 15, true, Type.ANY, new Type[] { Type.INT });
		method("getChipArea", "Chip", 15, true, Type.ANY, new Type[] { Type.INT });
		method("resurrect", "Chip", 500, true, Type.INT, new Type[] { Type.ANY, Type.ANY });
		method("summon", "Chip", 1750, true, Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY });
		method("getChipEffectiveArea", "Chip", 78, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getAllChips", "Chip", 200, true, Type.ARRAY, new Type[0]);

		/**
		 * Field
		 */
		method("getDistance", "Field", 15, true, Type.REAL, new Type[] { Type.INT, Type.INT });
		method("getCellDistance", "Field", 15, true, Type.INT, new Type[] { Type.INT, Type.INT });
		method("getPathLength", "Field", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getPath", "Field", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.INT, Type.INT, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.INT, Type.INT }),
		});
		method("getLeekOnCell", "Field", 15, true, Type.INT, new Type[] { Type.INT }).setMaxVersion(3);
		method("getEntityOnCell", "Field", 15, true, Type.INT, new Type[] { Type.INT });
		method("getCellContent", "Field", 6, true, Type.INT, new Type[] { Type.INT });
		method("isEmptyCell", "Field", 10, true, Type.BOOL, new Type[] { Type.INT });
		method("isObstacle", "Field", 10, true, Type.BOOL, new Type[] { Type.INT });
		method("isOnSameLine", "Field", 15, true, Type.BOOL, new Type[] { Type.INT, Type.INT });
		method("isLeek", "Field", 10, true, Type.BOOL, new Type[] { Type.INT }).setMaxVersion(3);
		method("isEntity", "Field", 10, true, Type.BOOL, new Type[] { Type.INT });
		method("getCellX", "Field", 5, true, Type.ANY, new Type[] { Type.INT });
		method("getCellY", "Field", 5, true, Type.ANY, new Type[] { Type.INT });
		method("getCellFromXY", "Field", 5, true, Type.ANY, new Type[] { Type.INT, Type.INT });
		method("lineOfSight", "Field", 31, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getObstacles", "Field", 85, true, Type.ARRAY, new Type[0]);
		method("getMapType", "Field", 5, true, Type.INT, new Type[0]);

		/**
		 * Fight / Combat
		 */
		method("getBulbChips", "Fight", 40, true, Type.ANY, new Type[] { Type.ANY });
		method("getNearestEnemy", "Fight", 25, true, Type.INT, new Type[0]);
		method("getFarestEnemy", "Fight", 31, true, Type.INT, new Type[0]).setMaxVersion(3);
		method("getFarthestEnemy", "Fight", 31, true, Type.INT, new Type[0]);
		method("getTurn", "Fight", 15, true, Type.INT, new Type[0]);
		method("getAliveEnemies", "Fight", 100, true, Type.ARRAY, new Type[0]);
		method("getAliveEnemiesCount", "Fight", 25, true, Type.INT, new Type[0]);
		method("getAlliedTurret", "Fight", 15, true, Type.ANY, new Type[0]);
		method("getAllEffects", "Fight", 200, true, Type.ARRAY, new Type[0]);
		method("getEnemyTurret", "Fight", 15, true, Type.ANY, new Type[0]);
		method("getDeadEnemies", "Fight", 100, true, Type.ARRAY, new Type[0]);
		method("getDeadEnemiesCount", "Fight", 25, true, Type.ANY, new Type[0]);
		method("getEnemies", "Fight", 100, true, Type.ARRAY, new Type[0]);
		method("getAllies", "Fight", 100, true, Type.ARRAY, new Type[0]);
		method("getEnemiesCount", "Fight", 25, true, Type.ANY, new Type[0]);
		method("getNearestAlly", "Fight", 25, true, Type.ANY, new Type[0]);
		method("getFarestAlly", "Fight", 31, true, Type.ANY, new Type[0]).setMaxVersion(3);
		method("getFarthestAlly", "Fight", 31, true, Type.ANY, new Type[0]);
		method("getAliveAllies", "Fight", 100, true, Type.ARRAY, new Type[0]);
		method("getDeadAllies", "Fight", 100, true, Type.ARRAY, new Type[0]);
		method("getAlliesCount", "Fight", 25, true, Type.ANY, new Type[0]);
		method("getNextPlayer", "Fight", 20, true, Type.ANY, new Type[0]);
		method("getPreviousPlayer", "Fight", 20, true, Type.ANY, new Type[0]);
		method("getCellToUseWeapon", "Fight", 38080, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY }),
		});
		method("getCellToUseWeaponOnCell", "Fight", 38080, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY }),
		});
		method("getCellToUseChip", "Fight", 38080, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getCellToUseChipOnCell", "Fight", 38080, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getEnemiesLife", "Fight", 50, true, Type.INT, new Type[0]);
		method("getAlliesLife", "Fight", 50, true, Type.INT, new Type[0]);
		method("moveToward", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveTowardCell", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveTowardLeeks", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveTowardEntities", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveTowardCells", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveAwayFrom", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveAwayFromCell", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveAwayFromCells", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveAwayFromLeeks", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveAwayFromEntities", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveAwayFromLine", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
		});
		method("moveTowardLine", "Fight", 500, true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
		});
		method("getFightID", "Fight", 5, true, Type.INT, new Type[0]);
		method("getFightType", "Fight", 10, true, Type.INT, new Type[0]);
		method("getFightContext", "Fight", 10, true, Type.INT, new Type[0]);
		method("getCellsToUseWeapon", "Fight", 25834, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("getCellsToUseWeaponOnCell", "Fight", 25834, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("getCellsToUseChip", "Fight", 25834, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getCellsToUseChipOnCell", "Fight", 25834, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getNearestEnemyTo", "Fight", 35, true, Type.ANY, new Type[] { Type.INT });
		method("getNearestEnemyToCell", "Fight", 35, true, Type.ANY, new Type[] { Type.INT });
		method("getNearestAllyToCell", "Fight", 35, true, Type.ANY, new Type[] { Type.INT });
		method("getNearestAllyTo", "Fight", 35, true, Type.ANY, new Type[] { Type.INT });

		/**
		 * Network / Réseau
		 */
		method("sendTo", "Network", 15, true, Type.BOOL, new Type[] { Type.INT, Type.INT, Type.ANY });
		method("sendAll", "Network", 40, true, Type.VOID, new Type[] { Type.INT, Type.ANY });
		method("getMessages", "Network", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.INT }),
			new CallableVersion(Type.ANY, new Type[0]),
		});
		method("getMessageAuthor", "Network", 5, true, Type.ANY, new Type[] { Type.ARRAY });
		method("getMessageType", "Network", 5, true, Type.ANY, new Type[] { Type.ARRAY });
		method("getMessageParams", "Network", 5, true, Type.ANY, new Type[] { Type.ARRAY });

		/**
		 * Util
		 */
		method("getDate", "Util", 50, true, Type.STRING, new Type[0]);
		method("getTime", "Util", 50, true, Type.STRING, new Type[0]);
		method("getTimestamp", "Util", 5, true, Type.INT, new Type[0]);
		method("mark", "Util", 164, true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("markText", "Util", 164, true,  new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("clearMarks", "Util", 15, true, Type.VOID, new Type[0]);
		method("show", "Util", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("pause", "Util", 30, true, Type.VOID, new Type[0]);
		method("getRegisters", "Util", 25, true, Type.ARRAY, new Type[0]);
		method("getRegister", "Util", 15, true, Type.ANY, new Type[] { Type.STRING });
		method("setRegister", "Util", 50, true, Type.VOID, new Type[] { Type.STRING, Type.ANY });
		method("deleteRegister", "Util", 16, true, Type.VOID, new Type[] { Type.STRING });
	}

	private static LeekFunctions method(String name, String clazz, int operations, boolean isStatic, Type return_type, Type[] arguments) {
		return method(name, clazz, 0, isStatic, new CallableVersion[] { new CallableVersion(return_type, arguments) });
	}

	private static LeekFunctions method(String name, String clazz, boolean isStatic, CallableVersion[] versions) {
		return method(name, clazz, 0, isStatic, versions);
	}

	private static LeekFunctions method(String name, String clazz, int operations, boolean isStatic, CallableVersion[] versions) {
		var function = new LeekFunctions(clazz, name, operations, isStatic, versions);
		functions.put(name, function);
		return function;
	}

	public String getNamespace() {
		return "com.leekwars.generator.FightFunctions";
	}

	public static Map<String, LeekFunctions> getFunctions() {
		return functions;
	}
}
