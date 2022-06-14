package com.leekwars.generator;

import java.util.HashMap;
import java.util.Map;

import leekscript.common.Type;
import leekscript.runner.CallableVersion;
import leekscript.runner.LeekFunctions;

public class FightFunctions {

	private static HashMap<String, LeekFunctions> functions = new HashMap<>();

	static {

		method("getLife", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getForce", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getStrength", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getAgility", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getScience", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getWisdom", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getResistance", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getMagic", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getCell", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY),
		});
		method("getWeapon", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getName", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.STRING),
		});
		method("getTP", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getMP", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getTotalTP", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getTotalMP", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getTotalLife", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getPower", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getAbsoluteShield", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getRelativeShield", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getDamageReturn", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("setWeapon", "Entity", true, Type.BOOL, new Type[] { Type.INT });
		method("say", "Entity", true, Type.BOOL, new Type[] { Type.STRING });
		method("lama", "Entity", true, Type.VOID, new Type[0]);
		method("listen", "Entity", true, Type.ARRAY, new Type[0]);
		method("getWeapons", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ARRAY),
		});
		method("isEnemy", "Entity", true, Type.BOOL, new Type[] { Type.INT });
		method("isAlly", "Entity", true, Type.BOOL, new Type[] { Type.INT });
		method("isAlive", "Entity", true, Type.BOOL, new Type[] { Type.INT });
		method("isDead", "Entity", true, Type.BOOL, new Type[] { Type.INT });
		method("getLeek", "Entity", true, Type.INT, new Type[0]).setMaxVersion(3);
		method("getEntity", "Entity", true, Type.INT, new Type[0]);
		method("getChips", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ARRAY),
		});
		method("getEffects", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ARRAY),
		});
		method("getLaunchedEffects", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getPassiveEffects", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getLevel", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getFrequency", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getCores", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getTeamName", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getFarmerName", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getFarmerCountry", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getTeamID", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getFarmerID", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getAIName", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.STRING),
		});
		method("getAIID", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getSummons", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ARRAY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getType", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("isSummon", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.BOOL),
		});
		method("getSummoner", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.INT),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("isStatic", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getBirthTurn", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});
		method("getEntityTurnOrder", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY })
		});
		method("getLeekID", "Entity", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT),
		});

		/**
		 * Weapon
		 */
		method("useWeapon", "Weapon", true, Type.INT, new Type[] { Type.INT });
		method("useWeaponOnCell", "Weapon", true, Type.INT, new Type[] { Type.INT });
		method("getWeaponMinScope", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.INT),
			new CallableVersion(Type.INT, new Type[] { Type.INT })
		}).setMaxVersion(3);
		method("getWeaponMaxScope", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.INT),
			new CallableVersion(Type.INT, new Type[] { Type.INT })
		}).setMaxVersion(3);
		method("getWeaponMinRange", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.INT),
			new CallableVersion(Type.INT, new Type[] { Type.INT })
		});
		method("getWeaponMaxRange", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.INT),
			new CallableVersion(Type.INT, new Type[] { Type.INT })
		});
		method("getWeaponLaunchType", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.INT })
		});
		method("getWeaponCost", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.INT),
			new CallableVersion(Type.INT, new Type[] { Type.INT })
		});
		method("getWeaponEffects", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.INT })
		});
		method("getWeaponPassiveEffects", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.ANY),
			new CallableVersion(Type.ANY, new Type[] { Type.INT })
		});
		method("getWeaponName", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.STRING),
			new CallableVersion(Type.STRING, new Type[] { Type.INT })
		});
		method("isInlineWeapon", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.BOOL),
			new CallableVersion(Type.BOOL, new Type[] { Type.INT })
		});
		method("weaponNeedLos", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.BOOL),
			new CallableVersion(Type.BOOL, new Type[] { Type.INT })
		});
		method("canUseWeapon", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY }),
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY, Type.ANY })
		});
		method("canUseWeaponOnCell", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY }),
			new CallableVersion(Type.BOOL, new Type[] { Type.ANY, Type.ANY })
		});
		method("getWeaponTargets", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY })
		});
		method("getWeaponFailure", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { }),
			new CallableVersion(Type.INT, new Type[] { Type.INT })
		});
		method("isWeapon", "Weapon", true, Type.BOOL, new Type[] { Type.INT });
		method("getWeaponArea", "Weapon", true, Type.ANY, new Type[] { Type.INT });
		method("getWeaponEffectiveArea", "Weapon", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
		});
		method("getAllWeapons", "Weapon", true, Type.ARRAY, new Type[0]);

		/**
		 * Chip
		 */
		method("getCurrentCooldown", "Chip", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		}).setMaxVersion(3);
		method("getCooldown", "Chip", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY }),
		});
		method("useChip", "Chip", true, Type.INT, new Type[] { Type.INT, Type.INT });
		method("useChipOnCell", "Chip", true, Type.INT, new Type[] { Type.INT, Type.INT });
		method("getChipName", "Chip", true, Type.STRING, new Type[] { Type.INT });
		method("getChipMinScope", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("getChipMaxScope", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("getChipMinRange", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("getChipMaxRange", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("getChipCost", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("getChipEffects", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("isInlineChip", "Chip", true, Type.BOOL, new Type[] { Type.INT });
		method("chipNeedLos", "Chip", true, Type.BOOL, new Type[] { Type.INT });
		method("getChipCooldown", "Chip", true, Type.INT, new Type[] { Type.INT });
		method("canUseChip", "Chip", true, Type.BOOL, new Type[] { Type.INT, Type.INT });
		method("canUseChipOnCell", "Chip", true, Type.BOOL, new Type[] { Type.INT, Type.INT });
		method("getChipTargets", "Chip", true, Type.ANY, new Type[] { Type.INT, Type.INT });
		method("getChipFailure", "Chip", true, Type.INT, new Type[] { Type.INT });
		method("isChip", "Chip", true, Type.BOOL, new Type[] { Type.INT });
		method("getChipLaunchType", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("getChipArea", "Chip", true, Type.ANY, new Type[] { Type.INT });
		method("resurrect", "Chip", true, Type.INT, new Type[] { Type.ANY, Type.ANY });
		method("summon", "Chip", true, Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY });
		method("getChipEffectiveArea", "Chip", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY })
		});
		method("getAllChips", "Chip", true, Type.ARRAY, new Type[0]);

		/**
		 * Field
		 */
		method("getDistance", "Field", true, Type.REAL, new Type[] { Type.INT, Type.INT });
		method("getCellDistance", "Field", true, Type.INT, new Type[] { Type.INT, Type.INT });
		method("getPathLength", "Field", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY })
		});
		method("getPath", "Field", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.ANY, new Type[] { Type.INT, Type.INT, Type.ANY })
		});
		method("getLeekOnCell", "Field", true, Type.INT, new Type[] { Type.INT }).setMaxVersion(3);
		method("getEntityOnCell", "Field", true, Type.INT, new Type[] { Type.INT });
		method("getCellContent", "Field", true, Type.INT, new Type[] { Type.INT });
		method("isEmptyCell", "Field", true, Type.BOOL, new Type[] { Type.INT });
		method("isObstacle", "Field", true, Type.BOOL, new Type[] { Type.INT });
		method("isOnSameLine", "Field", true, Type.BOOL, new Type[] { Type.INT, Type.INT });
		method("isLeek", "Field", true, Type.BOOL, new Type[] { Type.INT }).setMaxVersion(3);
		method("isEntity", "Field", true, Type.BOOL, new Type[] { Type.INT });
		method("getCellX", "Field", true, Type.ANY, new Type[] { Type.INT });
		method("getCellY", "Field", true, Type.ANY, new Type[] { Type.INT });
		method("getCellFromXY", "Field", true, Type.ANY, new Type[] { Type.INT, Type.INT });
		method("lineOfSight", "Field", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getObstacles", "Field", true, Type.ARRAY, new Type[0]);
		method("getMapType", "Field", true, Type.INT, new Type[0]);

		/**
		 * Fight / Combat
		 */
		method("getBulbChips", "Fight", true, Type.ARRAY, new Type[] { Type.ANY });
		method("getNearestEnemy", "Fight", true, Type.INT, new Type[0]);
		method("getFarestEnemy", "Fight", true, Type.INT, new Type[0]).setMaxVersion(3);
		method("getFarthestEnemy", "Fight", true, Type.INT, new Type[0]);
		method("getTurn", "Fight", true, Type.INT, new Type[0]);
		method("getAliveEnemies", "Fight", true, Type.ARRAY, new Type[0]);
		method("getAliveEnemiesCount", "Fight", true, Type.INT, new Type[0]);
		method("getAlliedTurret", "Fight", true, Type.ANY, new Type[0]);
		method("getAllEffects", "Fight", true, Type.ARRAY, new Type[0]);
		method("getEnemyTurret", "Fight", true, Type.ANY, new Type[0]);
		method("getDeadEnemies", "Fight", true, Type.ARRAY, new Type[0]);
		method("getDeadEnemiesCount", "Fight", true, Type.ANY, new Type[0]);
		method("getEnemies", "Fight", true, Type.ARRAY, new Type[0]);
		method("getAllies", "Fight", true, Type.ARRAY, new Type[0]);
		method("getEnemiesCount", "Fight", true, Type.ANY, new Type[0]);
		method("getNearestAlly", "Fight", true, Type.ANY, new Type[0]);
		method("getFarestAlly", "Fight", true, Type.ANY, new Type[0]).setMaxVersion(3);
		method("getFarthestAlly", "Fight", true, Type.ANY, new Type[0]);
		method("getAliveAllies", "Fight", true, Type.ARRAY, new Type[0]);
		method("getDeadAllies", "Fight", true, Type.ARRAY, new Type[0]);
		method("getAlliesCount", "Fight", true, Type.ANY, new Type[0]);
		method("getNextPlayer", "Fight", true, Type.ANY, new Type[0]);
		method("getPreviousPlayer", "Fight", true, Type.ANY, new Type[0]);
		method("getCellToUseWeapon", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY })
		});
		method("getCellToUseWeaponOnCell", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY })
		});
		method("getCellToUseChip", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY })
		});
		method("getCellToUseChipOnCell", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY })
		});
		method("getEnemiesLife", "Fight", true, Type.INT, new Type[0]);
		method("getAlliesLife", "Fight", true, Type.INT, new Type[0]);
		method("moveToward", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveTowardCell", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveTowardLeeks", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveTowardEntities", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT })
		});
		method("moveTowardCells", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveAwayFrom", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveAwayFromCell", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.INT, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.INT }),
		});
		method("moveAwayFromCells", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
		});
		method("moveAwayFromLeeks", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT })
		});
		method("moveAwayFromEntities", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY }),
			new CallableVersion(Type.INT, new Type[] { Type.ARRAY, Type.INT })
		});
		method("moveAwayFromLine", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
		});
		method("moveTowardLine", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.INT, new Type[] { Type.ANY, Type.ANY, Type.ANY })
		});
		method("getFightID", "Fight", true, Type.INT, new Type[0]);
		method("getFightType", "Fight", true, Type.INT, new Type[0]);
		method("getFightContext", "Fight", true, Type.INT, new Type[0]);
		method("getCellsToUseWeapon", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("getCellsToUseWeaponOnCell", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
		});
		method("getCellsToUseChip", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getCellsToUseChipOnCell", "Fight", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("getNearestEnemyTo", "Fight", true, Type.ANY, new Type[] { Type.INT });
		method("getNearestEnemyToCell", "Fight", true, Type.INT, new Type[] { Type.INT });
		method("getNearestAllyToCell", "Fight", true, Type.INT, new Type[] { Type.INT });
		method("getNearestAllyTo", "Fight", true, Type.INT, new Type[] { Type.INT });

		/**
		 * Network / Réseau
		 */
		method("sendTo", "Network", true, Type.BOOL, new Type[] { Type.INT, Type.INT, Type.ANY });
		method("sendAll", "Network", true, Type.VOID, new Type[] { Type.INT, Type.ANY });
		method("getMessages", "Network", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.INT }),
			new CallableVersion(Type.ANY, new Type[0]),
		});
		method("getMessageAuthor", "Network", true, Type.ANY, new Type[] { Type.ARRAY });
		method("getMessageType", "Network", true, Type.ANY, new Type[] { Type.ARRAY });
		method("getMessageParams", "Network", true, Type.ANY, new Type[] { Type.ARRAY });

		/**
		 * Util
		 */
		method("getDate", "Util", true, Type.STRING, new Type[0]);
		method("getTime", "Util", true, Type.STRING, new Type[0]);
		method("getTimestamp", "Util", true, Type.INT, new Type[0]);
		method("mark", "Util", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
		});
		method("markText", "Util", true,  new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY, Type.ANY, Type.ANY }),
		});
		method("clearMarks", "Util", true, Type.VOID, new Type[0]);
		method("show", "Util", true, new CallableVersion[] {
			new CallableVersion(Type.ANY, new Type[] { Type.ANY }),
			new CallableVersion(Type.ANY, new Type[] { Type.ANY, Type.ANY }),
		});
		method("pause", "Util", true, Type.VOID, new Type[0]);
		method("getRegisters", "Util", true, Type.ARRAY, new Type[0]);
		method("getRegister", "Util", true, Type.ANY, new Type[] { Type.STRING });
		method("setRegister", "Util", true, Type.VOID, new Type[] { Type.STRING, Type.ANY });
		method("deleteRegister", "Util", true, Type.VOID, new Type[] { Type.STRING });
	}

	private int mArguments;
	private int mArgumentsMin;
	private int mOperations = 1;
	private int[] parameters = null;

	public static final int DOUBLE = 1;
	public static final int INT = 2;
	public static final int BOOLEAN = 3;
	public static final int STRING = 4;
	public static final int NULL = 5;
	public static final int ARRAY = 6;
	public static final int NUMBER = 7;
	public static final int FUNCTION = 8;


	private static LeekFunctions method(String name, String clazz, Type return_type, Type[] arguments) {
		return method(name, clazz, 0, false, return_type, arguments);
	}
	private static LeekFunctions method(String name, String clazz, boolean isStatic, Type return_type, Type[] arguments) {
		return method(name, clazz, 0, isStatic, return_type, arguments);
	}
	private static LeekFunctions method(String name, String clazz, int operations, boolean isStatic, Type return_type, Type[] arguments) {
		return method(name, clazz, 0, isStatic, new CallableVersion[] { new CallableVersion(return_type, arguments) });
	}
	private static LeekFunctions method(String name, String clazz, CallableVersion[] versions) {
		return method(name, clazz, 0, false, versions);
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
