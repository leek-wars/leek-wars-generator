package com.leekwars.generator.classes;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.attack.weapons.Weapons;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.items.Items;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;

import leekscript.common.Error;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;

public class WeaponClass {

	// ----- Fonctions Weapon -----
	public static long useWeapon(EntityAI ai, long leek_id) throws LeekRunException {
		int success = -1;
		Entity target = ai.getFight().getEntity(leek_id);
		if (target != null && target != ai.getEntity() && !target.isDead()) {
			if (ai.getEntity().getWeapon() == null) {
				ai.addSystemLog(FarmerLog.WARNING, FarmerLog.NO_WEAPON_EQUIPPED);
			}
			success = ai.getFight().useWeapon(ai.getEntity(), target.getCell());
		}
		// Mort pendant le lancement, on arrête l'IA
		if (ai.getEntity().isDead()) {
			throw new LeekRunException(Error.ENTITY_DIED);
		}
		return success;
	}

	public static long useWeaponOnCell(EntityAI ai, long cell_id) throws LeekRunException {
		int success = -1;
		Cell target = ai.getFight().getMap().getCell((int) cell_id);
		if (target != null && target != ai.getEntity().getCell()) {
			if (ai.getEntity().getWeapon() == null) {
				ai.addSystemLog(FarmerLog.WARNING, FarmerLog.NO_WEAPON_EQUIPPED);
			}
			success = ai.getFight().useWeapon(ai.getEntity(), target);
		}
		// Mort pendant le lancement, on arrête l'IA
		if (ai.getEntity().isDead()) {
			throw new LeekRunException(Error.ENTITY_DIED);
		}
		return success;
	}

	public static long getWeaponMinScope(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMinRange();
	}

	public static long getWeaponMinScope(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMinRange();
	}

	public static long getWeaponMinRange(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMinRange();
	}

	public static long getWeaponMinRange(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMinRange();
	}

	// Deprecated : always 0
	public static long getWeaponFailure(EntityAI ai) {
		return 0l;
	}
	public static long getWeaponFailure(EntityAI ai, long id) {
		return 0l;
	}

	public static long getWeaponMaxScope(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMaxRange();
	}

	public static long getWeaponMaxScope(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMaxRange();
	}

	public static long getWeaponMaxRange(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMaxRange();
	}

	public static long getWeaponMaxRange(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null)
			return -1l;
		return (long) template.getAttack().getMaxRange();
	}

	public static long getWeaponCost(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null)
			return -1l;
		return (long) template.getCost();
	}

	public static long getWeaponCost(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null)
			return -1l;
		return (long) template.getCost();
	}

	public static boolean isInlineWeapon(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null)
			return false;
		return template.getAttack().getLaunchType() == Attack.LAUNCH_TYPE_LINE;
	}

	public static boolean isInlineWeapon(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null)
			return false;
		return template.getAttack().getLaunchType() == Attack.LAUNCH_TYPE_LINE;
	}

	public static String getWeaponName(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null)
			return "";
		return template.getName();
	}

	public static String getWeaponName(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null)
			return "";
		return template.getName();
	}

	public static ArrayLeekValue getWeaponEffects(EntityAI ai) throws LeekRunException {
		return getWeaponEffects(ai, -1);
	}

	public static ArrayLeekValue getWeaponEffects(EntityAI ai, long id) throws LeekRunException {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null) {
			return null;
		}
		var result = new ArrayLeekValue(ai);
		for (var feature : template.getAttack().getEffects()) {
			result.pushNoClone(ai, feature.getFeatureArray(ai));
		}
		return result;
	}

	public static LegacyArrayLeekValue getWeaponEffects_v1_3(EntityAI ai) throws LeekRunException {
		return getWeaponEffects_v1_3(ai, -1);
	}

	public static LegacyArrayLeekValue getWeaponEffects_v1_3(EntityAI ai, long id) throws LeekRunException {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null) {
			return null;
		}
		var result = new LegacyArrayLeekValue();
		for (var feature : template.getAttack().getEffects()) {
			result.pushNoClone(ai, feature.getFeatureArray(ai));
		}
		return result;
	}

	public static ArrayLeekValue getWeaponPassiveEffects(EntityAI ai) throws LeekRunException {
		return getWeaponPassiveEffects(ai, -1);
	}

	public static ArrayLeekValue getWeaponPassiveEffects(EntityAI ai, long id) throws LeekRunException {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null) {
			return null;
		}
		var retour = new ArrayLeekValue(ai);
		for (var feature : template.getPassiveEffects()) {
			retour.pushNoClone(ai, feature.getFeatureArray(ai));
		}
		return retour;
	}

	public static LegacyArrayLeekValue getWeaponPassiveEffects_v1_3(EntityAI ai) throws LeekRunException {
		return getWeaponPassiveEffects_v1_3(ai, -1);
	}

	public static LegacyArrayLeekValue getWeaponPassiveEffects_v1_3(EntityAI ai, long id) throws LeekRunException {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null) {
			return null;
		}
		var retour = new LegacyArrayLeekValue();
		for (var feature : template.getPassiveEffects()) {
			retour.pushNoClone(ai, feature.getFeatureArray(ai));
		}
		return retour;
	}

	public static Object getWeaponLaunchType(EntityAI ai) throws LeekRunException {
		Weapon template = ai.getEntity().getWeapon();
		if (template == null)
			return null;
		return (long) template.getAttack().getLaunchType();
	}

	public static Object getWeaponLaunchType(EntityAI ai, long weapon_id) throws LeekRunException {
		Weapon template = null;
		if (weapon_id == -1) {
			template = ai.getEntity().getWeapon();
		} else {
			template = Weapons.getWeapon((int) weapon_id);
		}
		if (template == null)
			return null;
		return (long) template.getAttack().getLaunchType();
	}

	public static boolean weaponNeedLos(EntityAI ai) {
		Weapon template = ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon();
		if (template == null) {
			return false;
		}
		return template.getAttack().needLos();
	}

	public static boolean weaponNeedLos(EntityAI ai, long id) {
		Weapon template = id == -1 ? (ai.getEntity().getWeapon() == null ? null : ai.getEntity().getWeapon()) : Weapons.getWeapon((int) id);
		if (template == null) {
			return false;
		}
		return template.getAttack().needLos();
	}

	public static boolean canUseWeapon(EntityAI ai, Object value1) throws LeekRunException {
		return canUseWeapon(ai, value1, null);
	}

	public static boolean canUseWeapon(EntityAI ai, Object value1, Object value2) throws LeekRunException {
		Entity target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();
		if (value2 == null) {
			target = ai.getFight().getEntity(ai.integer(value1));
		} else {
			target = ai.getFight().getEntity(ai.integer(value2));
			weapon = Weapons.getWeapon(ai.integer(value1));
		}
		if (weapon == null)
			return false;
		if (target != null && target.getCell() != null) {
			return Pathfinding.canUseAttack(ai.getEntity().getCell(), target.getCell(), weapon.getAttack());
		}
		return false;
	}

	public static boolean canUseWeaponOnCell(EntityAI ai, Object value1) throws LeekRunException {
		return canUseWeaponOnCell(ai, value1, null);
	}

	public static boolean canUseWeaponOnCell(EntityAI ai, Object value1, Object value2) throws LeekRunException {
		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();
		if (value2 == null) {
			target = ai.getFight().getMap().getCell(ai.integer(value1));
		} else {
			target = ai.getFight().getMap().getCell(ai.integer(value2));
			weapon = Weapons.getWeapon(ai.integer(value1));
		}
		if (weapon == null)
			return false;
		if (target != null) {
			return Pathfinding.canUseAttack(ai.getEntity().getCell(), target, weapon.getAttack());
		}
		return false;
	}

	public static ArrayLeekValue getWeaponTargets(EntityAI ai, long value1) throws LeekRunException {
		return getWeaponTargets(ai, value1, null);
	}

	public static ArrayLeekValue getWeaponTargets(EntityAI ai, long value1, Object value2) throws LeekRunException {

		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();

		if (value2 == null) {
			target = ai.getFight().getMap().getCell(ai.integer(value1));
		} else {
			weapon = Weapons.getWeapon(ai.integer(value1));
			target = ai.getFight().getMap().getCell(ai.integer(value2));
		}

		if (weapon == null)
			return null;
		if (target != null && ai.getEntity().getCell() != null) {
			var retour = new ArrayLeekValue(ai);
			var leeks = weapon.getAttack().getWeaponTargets(ai.getFight(), ai.getEntity(), target);
			for (Entity l : leeks) {
				retour.push(ai, (long) l.getFId());
			}
			return retour;
		}
		return null;
	}

	public static LegacyArrayLeekValue getWeaponTargets_v1_3(EntityAI ai, long value1) throws LeekRunException {
		return getWeaponTargets_v1_3(ai, value1, null);
	}

	public static LegacyArrayLeekValue getWeaponTargets_v1_3(EntityAI ai, long value1, Object value2) throws LeekRunException {

		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();

		if (value2 == null) {
			target = ai.getFight().getMap().getCell(ai.integer(value1));
		} else {
			weapon = Weapons.getWeapon(ai.integer(value1));
			target = ai.getFight().getMap().getCell(ai.integer(value2));
		}

		if (weapon == null)
			return null;
		if (target != null && ai.getEntity().getCell() != null) {
			var retour = new LegacyArrayLeekValue();
			var leeks = weapon.getAttack().getWeaponTargets(ai.getFight(), ai.getEntity(), target);
			for (Entity l : leeks) {
				retour.push(ai, (long) l.getFId());
			}
			return retour;
		}
		return null;
	}

	public static LegacyArrayLeekValue getWeaponEffectiveArea_v1_3(EntityAI ai, long value1) throws LeekRunException {
		return getWeaponEffectiveArea_v1_3(ai, value1, null, null);
	}

	public static ArrayLeekValue getWeaponEffectiveArea(EntityAI ai, long value1) throws LeekRunException {
		return getWeaponEffectiveArea(ai, value1, null, null);
	}

	public static LegacyArrayLeekValue getWeaponEffectiveArea_v1_3(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getWeaponEffectiveArea_v1_3(ai, value1, value2, null);
	}

	public static ArrayLeekValue getWeaponEffectiveArea(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getWeaponEffectiveArea(ai, value1, value2, null);
	}

	/**
	 * Retourne la liste des cellules affectées par le tir sur la cellule target_cell
	 *
	 * @param value1
	 *            Cellule cible de l'attaque ou id d'arme
	 * @param value2
	 *            Cellule cible de l'attaque ou null
	 * @param value3
	 *            Cellule de départ de l'attaque ou null
	 * @return Array des cellules affectées
	 * @throws LeekRunException
	 */
	public static LegacyArrayLeekValue getWeaponEffectiveArea_v1_3(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {
		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();

		if (value2 == null) {
			target = ai.getFight().getMap().getCell((int) value1);
		} else {
			weapon = Weapons.getWeapon((int) value1);
			target = ai.getFight().getMap().getCell(ai.integer(value2));
		}

		Cell start_cell = ai.getEntity().getCell();
		if (value3 != null) {
			start_cell = ai.getFight().getMap().getCell(ai.integer(value3));
		}

		if (target == null)
			return null;
		// On récupère l'arme
		if (weapon == null)
			return null;
		// On vérifie que la cellule de départ existe
		if (start_cell == null)
			return null;

		var retour = new LegacyArrayLeekValue();
		// On récupère les cellules touchées
		var cells = weapon.getAttack().getTargetCells(start_cell, target);
		// On les met dans le tableau
		for (Cell cell : cells) {
			retour.push(ai, (long) cell.getId());
		}
		return retour;
	}

	public static ArrayLeekValue getWeaponEffectiveArea(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {
		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();

		if (value2 == null) {
			target = ai.getFight().getMap().getCell((int) value1);
		} else {
			weapon = Weapons.getWeapon((int) value1);
			target = ai.getFight().getMap().getCell(ai.integer(value2));
		}

		Cell start_cell = ai.getEntity().getCell();
		if (value3 != null) {
			start_cell = ai.getFight().getMap().getCell(ai.integer(value3));
		}

		if (target == null)
			return null;
		// On récupère l'arme
		if (weapon == null)
			return null;
		// On vérifie que la cellule de départ existe
		if (start_cell == null)
			return null;

		var retour = new ArrayLeekValue(ai);
		// On récupère les cellules touchées
		var cells = weapon.getAttack().getTargetCells(start_cell, target);
		// On les met dans le tableau
		for (Cell cell : cells) {
			retour.push(ai, (long) cell.getId());
		}
		return retour;
	}

	public static boolean isWeapon(EntityAI ai, long id) {
		Integer i = Items.getType((int) id);
		if (i == null) {
			return false;
		}
		return i == Items.TYPE_WEAPON;
	}

	public static Object getWeaponArea(EntityAI ai, Object value) throws LeekRunException {
		if (value instanceof Number) {
			Weapon weapon = Weapons.getWeapon(ai.integer(value));
			if (weapon != null) {
				return (long) weapon.getAttack().getArea();
			}
		}
		return null;
	}

	public static LegacyArrayLeekValue getAllWeapons_v1_3(EntityAI ai) throws LeekRunException {
		var retour = new LegacyArrayLeekValue();
		for (var weapon : Weapons.getTemplates().values()) {
			retour.push(ai, (long) weapon.getId());
		}
		return retour;
	}

	public static ArrayLeekValue getAllWeapons(EntityAI ai) throws LeekRunException {
		var retour = new ArrayLeekValue(ai, Weapons.getTemplates().size());
		for (var weapon : Weapons.getTemplates().values()) {
			retour.push(ai, (long) weapon.getId());
		}
		return retour;
	}
}
