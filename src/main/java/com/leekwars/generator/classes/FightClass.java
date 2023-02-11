package com.leekwars.generator.classes;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.attack.effect.Effect;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.attack.weapons.Weapons;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.bulbs.Bulbs;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;

public class FightClass {

	// ---- Fonctions Fight ----
	public static long getNearestEnemy(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getCell() == null)
			return -1;
		List<Entity> entities = ai.getFight().getEnemiesEntities(ai.getEntity().getTeam());

		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(ai.getEntity().getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? -1 : nearest.getFId();
	}

	public static long getFarestEnemy(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getCell() == null)
			return -1;
		List<Entity> entities = ai.getFight().getEnemiesEntities(ai.getEntity().getTeam());

		int dist = -1;
		Entity farest = null;
		for (Entity l : entities) {
			if (l.isDead() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(ai.getEntity().getCell(), l.getCell());
			if (d > dist || dist == -1) {
				dist = d;
				farest = l;
			}
		}
		return farest == null ? -1 : farest.getFId();
	}

	public static long getFarthestEnemy(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getCell() == null)
			return -1;
		List<Entity> entities = ai.getFight().getEnemiesEntities(ai.getEntity().getTeam());

		int dist = -1;
		Entity farest = null;
		for (Entity l : entities) {
			if (l.isDead() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(ai.getEntity().getCell(), l.getCell());
			if (d > dist || dist == -1) {
				dist = d;
				farest = l;
			}
		}
		return farest == null ? -1 : farest.getFId();
	}

	public static long getTurn(EntityAI ai) {
		return ai.getFight().getTurn();
	}

	public static LegacyArrayLeekValue getAllEffects_v1_3(EntityAI ai) throws LeekRunException {
		var retour = new LegacyArrayLeekValue();
		for (long i = 1; i <= Effect.effects.length; ++i) {
			retour.push(ai, i);
		}
		return retour;
	}

	public static ArrayLeekValue getAllEffects(EntityAI ai) throws LeekRunException {
		var retour = new ArrayLeekValue(ai, Effect.effects.length);
		for (long i = 1; i <= Effect.effects.length; ++i) {
			retour.push(ai, i);
		}
		return retour;
	}

	public static LegacyArrayLeekValue getAliveEnemies_v1_3(EntityAI ai) throws LeekRunException {
		var result = new LegacyArrayLeekValue();
		for (Entity e : ai.getFight().getAllEntities(false)) {
			if (e.getTeam() != ai.getEntity().getTeam()) {
				result.push(ai, (long) e.getFId());
			}
		}
		return result;
	}

	public static ArrayLeekValue getAliveEnemies(EntityAI ai) throws LeekRunException {
		var result = new ArrayLeekValue(ai);
		for (Entity e : ai.getFight().getAllEntities(false)) {
			if (e.getTeam() != ai.getEntity().getTeam()) {
				result.push(ai, (long) e.getFId());
			}
		}
		return result;
	}

	public static long getAliveEnemiesCount(EntityAI ai) throws LeekRunException {
		long count = 0;
		for (Entity e : ai.getFight().getAllEntities(false)) {
			if (e.getTeam() != ai.getEntity().getTeam()) {
				count++;
			}
		}
		return count;
	}

	public static LegacyArrayLeekValue getDeadEnemies_v1_3(EntityAI ai) throws LeekRunException {
		var result = new LegacyArrayLeekValue();
		for (Entity e : ai.getFight().getAllEntities(true)) {
			if (e.getTeam() != ai.getEntity().getTeam() && e.isDead()) {
				result.push(ai, (long) e.getFId());
			}
		}
		return result;
	}

	public static ArrayLeekValue getDeadEnemies(EntityAI ai) throws LeekRunException {
		var result = new ArrayLeekValue(ai);
		for (Entity e : ai.getFight().getAllEntities(true)) {
			if (e.getTeam() != ai.getEntity().getTeam() && e.isDead()) {
				result.push(ai, (long) e.getFId());
			}
		}
		return result;
	}

	public static long getDeadEnemiesCount(EntityAI ai) throws LeekRunException {
		long count = 0;
		for (Entity e : ai.getFight().getAllEntities(true)) {
			if (e.getTeam() != ai.getEntity().getTeam() && e.isDead()) {
				count++;
			}
		}
		return count;
	}

	public static LegacyArrayLeekValue getEnemies_v1_3(EntityAI ai) throws LeekRunException {
		var result = new LegacyArrayLeekValue();
		for (Entity e : ai.getFight().getEnemiesEntities(ai.getEntity().getTeam(), true)) {
			result.push(ai, (long) e.getFId());
		}
		return result;
	}

	public static ArrayLeekValue getEnemies(EntityAI ai) throws LeekRunException {
		var result = new ArrayLeekValue(ai);
		for (Entity e : ai.getFight().getEnemiesEntities(ai.getEntity().getTeam(), true)) {
			result.push(ai, (long) e.getFId());
		}
		return result;
	}

	public static long getEnemiesLife(EntityAI ai) {
		int life = 0;
		for (Entity l : ai.getFight().getEnemiesEntities(ai.getEntity().getTeam())) {
			life += l.getLife();
		}
		return life;
	}

	public static long getEnemiesCount(EntityAI ai) throws LeekRunException {
		return ai.getFight().getEnemiesEntities(ai.getEntity().getTeam(), true).size();
	}

	public static Object getAlliedTurret(EntityAI ai) {
		if (ai.getFight().getType() == Fight.TYPE_TEAM) {
			for (Entity e : ai.getFight().getTeamEntities(ai.getEntity().getTeam(), true)) {
				if (e.getType() == Entity.TYPE_TURRET) return (long) e.getFId();
			}
		}
		return null;
	}

	public static Object getEnemyTurret(EntityAI ai) {
		if (ai.getFight().getType() == Fight.TYPE_TEAM) {
			for (Entity e : ai.getFight().getEnemiesEntities(ai.getEntity().getTeam(), true)) {
				if (e.getType() == Entity.TYPE_TURRET) return (long) e.getFId();
			}
		}
		return null;
	}

	public static long getNearestAlly(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getCell() == null)
			return -1;
		List<Entity> entities = ai.getFight().getTeamEntities(ai.getEntity().getTeam());
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead() || l == ai.getEntity() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(ai.getEntity().getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? -1 : nearest.getFId();
	}

	public static long getFarestAlly(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getCell() == null) return -1;
		List<Entity> entities = ai.getFight().getTeamEntities(ai.getEntity().getTeam());
		int dist = -1;
		Entity farest = null;
		for (Entity l : entities) {
			if (l.isDead() || l == ai.getEntity() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(ai.getEntity().getCell(), l.getCell());
			if (d > dist || dist == -1) {
				dist = d;
				farest = l;
			}
		}
		return farest == null ? -1 : farest.getFId();
	}

	public static long getFarthestAlly(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getCell() == null) return -1;
		List<Entity> entities = ai.getFight().getTeamEntities(ai.getEntity().getTeam());
		int dist = -1;
		Entity farest = null;
		for (Entity l : entities) {
			if (l.isDead() || l == ai.getEntity() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(ai.getEntity().getCell(), l.getCell());
			if (d > dist || dist == -1) {
				dist = d;
				farest = l;
			}
		}
		return farest == null ? -1 : farest.getFId();
	}

	public static LegacyArrayLeekValue getAliveAllies_v1_3(EntityAI ai) throws LeekRunException {
		var retour = new LegacyArrayLeekValue();
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam())) {
			retour.push(ai, (long) l.getFId());
		}
		return retour;
	}

	public static ArrayLeekValue getAliveAllies(EntityAI ai) throws LeekRunException {
		var retour = new ArrayLeekValue(ai);
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam())) {
			retour.push(ai, (long) l.getFId());
		}
		return retour;
	}

	public static long getAliveAlliesCount(EntityAI ai) throws LeekRunException {
		return ai.getFight().getTeamEntities(ai.getEntity().getTeam()).size();
	}

	public static LegacyArrayLeekValue getDeadAllies_v1_3(EntityAI ai) throws LeekRunException {
		var retour = new LegacyArrayLeekValue();
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam(), true)) {
			if (l.isDead()) {
				retour.push(ai, (long) l.getFId());
			}
		}
		return retour;
	}

	public static ArrayLeekValue getDeadAllies(EntityAI ai) throws LeekRunException {
		var retour = new ArrayLeekValue(ai);
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam(), true)) {
			if (l.isDead()) {
				retour.push(ai, (long) l.getFId());
			}
		}
		return retour;
	}

	public static long getDeadAlliesCount(EntityAI ai) throws LeekRunException {
		long nb = 0;
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam(), true)) {
			if (l.isDead())
				nb++;
		}
		return nb;
	}

	public static LegacyArrayLeekValue getAllies_v1_3(EntityAI ai) throws LeekRunException {
		var retour = new LegacyArrayLeekValue();
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam(), true)) {
			retour.push(ai, (long) l.getFId());
		}
		return retour;
	}

	public static ArrayLeekValue getAllies(EntityAI ai) throws LeekRunException {
		var retour = new ArrayLeekValue(ai);
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam(), true)) {
			retour.push(ai, (long) l.getFId());
		}
		return retour;
	}

	public static long getAlliesCount(EntityAI ai) throws LeekRunException {
		return ai.getFight().getTeamEntities(ai.getEntity().getTeam(), true).size();
	}

	public static long getAlliesLife(EntityAI ai) {
		int life = 0;
		for (Entity l : ai.getFight().getTeamEntities(ai.getEntity().getTeam())) {
			life += l.getLife();
		}
		return life;
	}

	public static long getNextPlayer(EntityAI ai) {
		return ai.getFight().getOrder().getNextPlayer().getFId();
	}

	public static long getPreviousPlayer(EntityAI ai) {
		return ai.getFight().getOrder().getPreviousPlayer().getFId();
	}

	public static long getCellToUseWeapon(EntityAI ai, long value1) throws LeekRunException {
		return getCellToUseWeapon(ai, value1, null, null);
	}

	public static long getCellToUseWeapon(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getCellToUseWeapon(ai, value1, value2, null);
	}

	public static long getCellToUseWeapon(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();
		Entity target = null;

		if (value2 == null) {
			target = ai.getFight().getEntity(ai.integer(value1));
		} else {
			weapon = Weapons.getWeapon(ai.integer(value1));
			target = ai.getFight().getEntity(ai.integer(value2));
		}
		int cell = -1;
		if (target != null && target.getCell() != null && weapon != null) {

			var cells_to_ignore = new ArrayList<Cell>();
			if (value3 instanceof GenericArrayLeekValue) {
				ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
			} else {
				cells_to_ignore.add(ai.getEntity().getCell());
			}
			var possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target.getCell(), cells_to_ignore);
			if (possible != null && possible.size() > 0) {
				if (possible.contains(ai.getEntity().getCell())) {
					cell = ai.getEntity().getCell().getId();
				} else {
					List<Cell> path = Pathfinding.getAStarPath(ai, ai.getEntity().getCell(), possible, cells_to_ignore);
					if (path != null) {
						if (path.size() > 0)
							cell = path.get(path.size() - 1).getId();
						else
							cell = -1;
					}
				}
			}
		}
		return cell;
	}

	public static long getCellToUseWeaponOnCell(EntityAI ai, long value1) throws LeekRunException {
		return getCellToUseWeaponOnCell(ai, value1, null, null);
	}

	public static long getCellToUseWeaponOnCell(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getCellToUseWeaponOnCell(ai, value1, value2, null);
	}

	public static long getCellToUseWeaponOnCell(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {

		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();

		if (value2 == null) {
			target = ai.getFight().getMap().getCell((int) value1);
		} else {
			weapon = Weapons.getWeapon((int) value1);
			target = ai.getFight().getMap().getCell(ai.integer(value2));
		}
		int retour = -1;
		if (target != null && weapon != null) {

			ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
			if (value3 instanceof GenericArrayLeekValue) {
				ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
			} else
				cells_to_ignore.add(ai.getEntity().getCell());

			var possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target, cells_to_ignore);
			if (possible != null && possible.size() > 0) {
				if (possible.contains(ai.getEntity().getCell())) {
					retour = ai.getEntity().getCell().getId();
				} else {
					List<Cell> path = Pathfinding.getAStarPath(ai, ai.getEntity().getCell(), possible, cells_to_ignore);
					if (path != null) {
						if (path.size() > 0)
							retour = path.get(path.size() - 1).getId();
						else
							retour = -1;
					}
				}
			}
		}
		return retour;
	}

	public static long getCellToUseChip(EntityAI ai, Object chip, Object t) throws LeekRunException {
		return getCellToUseChip(ai, chip, t, null);
	}

	public static long getCellToUseChip(EntityAI ai, Object chip, Object t, Object value3) throws LeekRunException {

		Entity target = ai.getFight().getEntity(ai.integer(t));
		int cell = -1;
		if (target == null)
			return cell;
		Chip template = Chips.getChip(ai.integer(chip));
		if (template == null)
			return cell;
		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target.getCell(), cells_to_ignore);
		if (possible != null && possible.size() > 0) {
			if (possible.contains(ai.getEntity().getCell())) {
				cell = ai.getEntity().getCell().getId();
			} else {
				List<Cell> path = Pathfinding.getAStarPath(ai, ai.getEntity().getCell(), possible, cells_to_ignore);
				if (path != null) {
					if (path.size() > 0)
						cell = path.get(path.size() - 1).getId();
					else
						cell = ai.getEntity().getCell().getId();
				}
			}
		}
		return cell;
	}

	public static long getCellToUseChipOnCell(EntityAI ai, Object chip, Object cell) throws LeekRunException {
		return getCellToUseChipOnCell(ai, chip, cell, null);
	}

	public static long getCellToUseChipOnCell(EntityAI ai, Object chip, Object cell, Object value3) throws LeekRunException {

		int retour = -1;
		Cell target = ai.getFight().getMap().getCell(ai.integer(cell));
		if (target == null)
			return ai.integer(cell);
		Chip template = Chips.getChip(ai.integer(chip));
		if (template == null)
			return ai.integer(cell);

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());

		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target, cells_to_ignore);
		if (possible != null && possible.size() > 0) {
			if (possible.contains(ai.getEntity().getCell())) {
				retour = ai.getEntity().getCell().getId();
			} else {
				List<Cell> path = Pathfinding.getAStarPath(ai, ai.getEntity().getCell(), possible);
				if (path != null) {
					if (path.size() > 0)
						retour = path.get(path.size() - 1).getId();
					else
						retour = ai.getEntity().getCell().getId();
				}
			}
		}
		return retour;
	}

	public static long moveToward(EntityAI ai, long leek_id) throws LeekRunException {
		return moveToward(ai, leek_id, -1);
	}

	public static long moveToward(EntityAI ai, long leek_id, long pm_to_use) throws LeekRunException {

		ai.ops(2000);

		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP()) {
			pm = ai.getEntity().getMP();
		}
		long used_pm = 0;
		if (pm > 0) {
			Entity target = ai.getFight().getEntity(leek_id);
			if (target != null && !target.isDead()) {
				List<Cell> path = ai.getFight().getMap().getPathBeetween(ai, ai.getEntity().getCell(), target.getCell(), null);
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path.subList(0, Math.min(path.size(), pm)));
				}
			}
		}
		return used_pm;
	}

	public static long moveTowardCell(EntityAI ai, long cell_id) throws LeekRunException {
		return moveTowardCell(ai, cell_id, ai.getEntity().getMP());
	}

	public static long moveTowardCell(EntityAI ai, long cell_id, long pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP()) {
			pm = ai.getEntity().getMP();
		}
		long used_pm = 0;
		if (pm > 0 && ai.getEntity().getCell() != null) {
			Cell target = ai.getEntity().getCell().getMap().getCell((int) cell_id);
			if (target != null && target != ai.getEntity().getCell()) {
				List<Cell> path = null;
				if (!target.isWalkable())
					path = Pathfinding.getAStarPath(ai, ai.getEntity().getCell(), Pathfinding.getValidCellsAroundObstacle(target), null);
				else
					path = ai.getFight().getMap().getPathBeetween(ai, ai.getEntity().getCell(), target, null);

				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path.subList(0, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public static long moveTowardEntities(EntityAI ai, GenericArrayLeekValue leeks) throws LeekRunException {
		return moveTowardLeeks(ai, leeks, -1);
	}

	public static long moveTowardEntities(EntityAI ai, GenericArrayLeekValue leeks, long pm_to_use) throws LeekRunException {
		return moveTowardLeeks(ai, leeks, pm_to_use);
	}

	public static long moveTowardLeeks(EntityAI ai, GenericArrayLeekValue leeks) throws LeekRunException {
		return moveTowardLeeks(ai, leeks, -1);
	}

	public static long moveTowardLeeks(EntityAI ai, GenericArrayLeekValue leeks, long pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < leeks.size(); i++) {
				var value = ai.integer(leeks.get(ai, i));
				Entity l = ai.getFight().getEntity(value);
				if (l != null && !l.isDead())
					targets.add(l.getCell());
			}
			if (targets.size() != 0) {
				List<Cell> path = Pathfinding.getAStarPath(ai, ai.getEntity().getCell(), targets);
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path.subList(0, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public static long moveTowardCells(EntityAI ai, GenericArrayLeekValue cells) throws LeekRunException {
		return moveTowardCells(ai, cells, -1);
	}

	public static long moveTowardCells(EntityAI ai, GenericArrayLeekValue cells, long pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < cells.size(); i++) {
				var value = ai.integer(cells.get(ai, i));
				Cell c = ai.getFight().getMap().getCell(value);
				if (c != null)
					targets.add(c);
			}
			if (targets.size() != 0) {
				List<Cell> path = Pathfinding.getAStarPath(ai, ai.getEntity().getCell(), targets);
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path.subList(0, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public static long moveAwayFrom(EntityAI ai, long leek_id) throws LeekRunException {
		return moveAwayFrom(ai, leek_id, -1);
	}

	public static long moveAwayFrom(EntityAI ai, long leek_id, long pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		long used_pm = 0;
		if (pm > 0) {
			Entity target = ai.getFight().getEntity(leek_id);
			if (target != null && target.getCell() != null) {
				List<Cell> cells = new ArrayList<Cell>();
				cells.add(target.getCell());
				List<Cell> path = ai.getFight().getMap().getPathAway(ai, ai.getEntity().getCell(), cells, pm);
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path);
				}
			}
		}
		return used_pm;
	}

	public static long moveAwayFromCell(EntityAI ai, long cell_id) throws LeekRunException {
		return moveAwayFromCell(ai, cell_id, -1);
	}

	public static long moveAwayFromCell(EntityAI ai, long cell_id, long pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		int used_pm = 0;
		if (pm > 0) {
			Cell target = ai.getFight().getMap().getCell((int) cell_id);
			if (target != null) {
				List<Cell> cells = new ArrayList<Cell>();
				cells.add(target);
				List<Cell> path = ai.getFight().getMap().getPathAway(ai, ai.getEntity().getCell(), cells, pm);
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path);
				}
			}
		}
		return used_pm;
	}

	public static long moveAwayFromLeeks(EntityAI ai, GenericArrayLeekValue leeks) throws LeekRunException {
		return moveAwayFromEntities(ai, leeks, -1);
	}

	public static long moveAwayFromEntities(EntityAI ai, GenericArrayLeekValue leeks) throws LeekRunException {
		return moveAwayFromEntities(ai, leeks, -1);
	}

	public static long moveAwayFromLeeks(EntityAI ai, GenericArrayLeekValue leeks, long pm_to_use) throws LeekRunException {
		return moveAwayFromEntities(ai, leeks, pm_to_use);
	}

	public static long moveAwayFromEntities(EntityAI ai, GenericArrayLeekValue leeks, long pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < leeks.size(); i++) {
				var value = ai.integer(leeks.get(ai, i));
				Entity l = ai.getFight().getEntity(value);
				if (l != null && !l.isDead())
					targets.add(l.getCell());
			}
			if (targets.size() != 0) {
				List<Cell> path = ai.getFight().getMap().getPathAway(ai, ai.getEntity().getCell(), targets, pm);
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path);
				}
			}
		}
		return used_pm;
	}

	public static long moveAwayFromCells(EntityAI ai, GenericArrayLeekValue leeks) throws LeekRunException {
		return moveAwayFromCells(ai, leeks, -1);
	}

	public static long moveAwayFromCells(EntityAI ai, GenericArrayLeekValue leeks, long pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < leeks.size(); i++) {
				var value = ai.integer(leeks.get(ai, i));
				Cell c = ai.getFight().getMap().getCell(value);
				if (c != null)
					targets.add(c);
			}
			if (targets.size() != 0) {
				List<Cell> path = ai.getFight().getMap().getPathAway(ai, ai.getEntity().getCell(), targets, pm);
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path);
				}
			}
		}
		return used_pm;
	}

	public static long moveAwayFromLine(EntityAI ai, long cell1, long cell2) throws LeekRunException {
		return moveAwayFromLine(ai, cell1, cell2, -1);
	}

	public static long moveAwayFromLine(EntityAI ai, long cell1, long cell2, long pm_to_use) throws LeekRunException {
		// Nombre de PM à utiliser au max pour fuir comme un lache
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		// On met le nombre de PM utilisés au final (pour renvoyer à
		// l'utilisateur)
		int used_pm = 0;
		// On regarde si le nombre de PM qu'on veut utiliser est valide
		if (pm > 0) {
			// On récupère les cellules
			Cell target = ai.getFight().getMap().getCell((int) cell1);
			Cell target2 = ai.getFight().getMap().getCell((int) cell2);
			// Si il est trouvé on calcule le path
			if (target != null && target2 != null) {
				List<Cell> path = Pathfinding.getPathAwayFromLine(ai, ai.getEntity().getCell(), target, target2, pm);
				// Si un path a été trouvé on se barre !
				if (path != null) {
					int[] cells = new int[path.size()];
					for (int i = 0; i < path.size(); i++)
						cells[i] = path.get(i).getId();
					// log.addCell(ai.getEntity(), cells, 255, 1);
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path);
				}
			}
		}
		return used_pm;
	}

	public static long moveTowardLine(EntityAI ai, long cell1, long cell2) throws LeekRunException {
		return moveTowardLine(ai, cell1, cell2, -1);
	}

	public static long moveTowardLine(EntityAI ai, long cell1, long cell2, long pm_to_use) throws LeekRunException {
		// Nombre de PM à utiliser au max
		int pm = pm_to_use == -1 ? ai.getEntity().getMP() : (int) pm_to_use;
		if (pm > ai.getEntity().getMP())
			pm = ai.getEntity().getMP();
		// On met le nombre de PM utilisés au final (pour renvoyer à
		// l'utilisateur)
		int used_pm = 0;
		// On regarde si le nombre de PM qu'on veut utiliser est valide
		if (pm > 0) {
			// On récupère les cellules
			Cell target = ai.getFight().getMap().getCell((int) cell1);
			Cell target2 = ai.getFight().getMap().getCell((int) cell2);
			// Si il est trouvé on calcule le path
			if (target != null && target2 != null && ai.getEntity().getCell() != null) {
				List<Cell> path = Pathfinding.getPathTowardLine(ai, ai.getEntity().getCell(), target, target2);
				// Si un path a été trouvé on y va
				if (path != null) {
					used_pm = ai.getFight().moveEntity(ai.getEntity(), path.size() > pm ? path.subList(0, pm) : path);
				}
			}
		}
		return used_pm;
	}

	public static LegacyArrayLeekValue getBulbChips_v1_3(EntityAI ai, long id) throws LeekRunException {
		if (id > 0) {
			Chip chip = Chips.getChip((int) id);
			if (chip != null && chip.getAttack().getEffects().get(0).getId() == Effect.TYPE_SUMMON) {
				var template = Bulbs.getInvocationTemplate((int) chip.getAttack().getEffects().get(0).getValue1());
				if (template != null) {
					List<Chip> chips = template.getChips();
					var retour = new LegacyArrayLeekValue();
					for (int i = 0; i < chips.size(); i++) {
						retour.push(ai, (long) chips.get(i).getId());
					}
					return retour;
				}
			}
		}
		return null;
	}


	public static ArrayLeekValue getBulbChips(EntityAI ai, long id) throws LeekRunException {
		if (id > 0) {
			Chip chip = Chips.getChip((int) id);
			if (chip != null && chip.getAttack().getEffects().get(0).getId() == Effect.TYPE_SUMMON) {
				var template = Bulbs.getInvocationTemplate((int) chip.getAttack().getEffects().get(0).getValue1());
				if (template != null) {
					List<Chip> chips = template.getChips();
					var retour = new ArrayLeekValue(ai, chips.size());
					for (int i = 0; i < chips.size(); i++) {
						retour.push(ai, (long) chips.get(i).getId());
					}
					return retour;
				}
			}
		}
		return null;
	}

	public static Object getEntityTurnOrder(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getFight().getOrder().getEntityTurnOrder(ai.getEntity());
		if (value instanceof Number) {
			Entity l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null && !l.isDead())
				return (long) ai.getFight().getOrder().getEntityTurnOrder(l);
		}
		return null;
	}

	public static LegacyArrayLeekValue getCellsToUseWeapon_v1_3(EntityAI ai, long value1) throws LeekRunException {
		return getCellsToUseWeapon_v1_3(ai, value1, null, null);
	}

	public static ArrayLeekValue getCellsToUseWeapon(EntityAI ai, long value1) throws LeekRunException {
		return getCellsToUseWeapon(ai, value1, null, null);
	}

	public static LegacyArrayLeekValue getCellsToUseWeapon_v1_3(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getCellsToUseWeapon_v1_3(ai, value1, value2, null);
	}

	public static ArrayLeekValue getCellsToUseWeapon(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getCellsToUseWeapon(ai, value1, value2, null);
	}

	/**
	 * Renvoie la liste des cellules à partir desquelles ont peut utiliser son
	 * arme sur le poireau cible
	 *
	 * @param target_leek_id
	 *            Poireau cible
	 * @return Liste des cellules
	 * @throws LeekRunException
	 */
	public static LegacyArrayLeekValue getCellsToUseWeapon_v1_3(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();
		Entity target = null;

		if (value2 == null) {
			target = ai.getFight().getEntity(ai.integer(value1));
		} else {
			weapon = Weapons.getWeapon(ai.integer(value1));
			target = ai.getFight().getEntity(ai.integer(value2));
		}
		if (target == null || target.getCell() == null || weapon == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target.getCell(), cells_to_ignore);

		var retour = new LegacyArrayLeekValue();
		for (Cell cell : possible) {
			retour.push(ai, (long) cell.getId());
		}
		return retour;
	}

	public static ArrayLeekValue getCellsToUseWeapon(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();
		Entity target = null;

		if (value2 == null) {
			target = ai.getFight().getEntity(ai.integer(value1));
		} else {
			weapon = Weapons.getWeapon(ai.integer(value1));
			target = ai.getFight().getEntity(ai.integer(value2));
		}
		if (target == null || target.getCell() == null || weapon == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target.getCell(), cells_to_ignore);

		var retour = new ArrayLeekValue(ai, possible.size());
		for (Cell cell : possible) {
			retour.push(ai, (long) cell.getId());
		}
		return retour;
	}

	public static LegacyArrayLeekValue getCellsToUseWeaponOnCell_v1_3(EntityAI ai, long value1) throws LeekRunException {
		return getCellsToUseWeaponOnCell_v1_3(ai, value1, null, null);
	}

	public static ArrayLeekValue getCellsToUseWeaponOnCell(EntityAI ai, long value1) throws LeekRunException {
		return getCellsToUseWeaponOnCell(ai, value1, null, null);
	}

	public static LegacyArrayLeekValue getCellsToUseWeaponOnCell_v1_3(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getCellsToUseWeaponOnCell_v1_3(ai, value1, value2, null);
	}

	public static ArrayLeekValue getCellsToUseWeaponOnCell(EntityAI ai, long value1, Object value2) throws LeekRunException {
		return getCellsToUseWeaponOnCell(ai, value1, value2, null);
	}
	/**
	 * Renvoie la liste des cellules à partir desquelles on peut utiliser son
	 * arme sur la cellule cible
	 *
	 * @param target_cell_id
	 *            Cellule cible
	 * @return Liste des cellules
	 * @throws LeekRunException
	 */
	public static LegacyArrayLeekValue getCellsToUseWeaponOnCell_v1_3(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {
		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();
		if (value2 == null) {
			target = ai.getFight().getMap().getCell(ai.integer(value1));
		} else {
			weapon = Weapons.getWeapon(ai.integer(value1));
			target = ai.getFight().getMap().getCell(ai.integer(value2));
		}
		if (target == null || weapon == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target, cells_to_ignore);

		var retour = new LegacyArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(ai, (long) cell.getId());
			}
		}
		return retour;
	}

	public static ArrayLeekValue getCellsToUseWeaponOnCell(EntityAI ai, long value1, Object value2, Object value3) throws LeekRunException {
		Cell target = null;
		Weapon weapon = (ai.getEntity().getWeapon() == null) ? null : ai.getEntity().getWeapon();
		if (value2 == null) {
			target = ai.getFight().getMap().getCell(ai.integer(value1));
		} else {
			weapon = Weapons.getWeapon(ai.integer(value1));
			target = ai.getFight().getMap().getCell(ai.integer(value2));
		}
		if (target == null || weapon == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target, cells_to_ignore);

		var retour = new ArrayLeekValue(ai);
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(ai, (long) cell.getId());
			}
		}
		return retour;
	}

	public static LegacyArrayLeekValue getCellsToUseChip_v1_3(EntityAI ai, long chip_id, long target_leek_id) throws LeekRunException {
		return getCellsToUseChip_v1_3(ai, chip_id, target_leek_id, null);
	}

	public static ArrayLeekValue getCellsToUseChip(EntityAI ai, long chip_id, long target_leek_id) throws LeekRunException {
		return getCellsToUseChip(ai, chip_id, target_leek_id, null);
	}

	/**
	 * Renvoie la liste des cellules à partir desquelles on peut utiliser la
	 * puce chip_id, sur le poireau cible
	 *
	 * @param chip_id
	 *            Puce à tester
	 * @param target_leek_id
	 *            Poireau cible
	 * @return Liste des cellules
	 * @throws LeekRunException
	 */
	public static LegacyArrayLeekValue getCellsToUseChip_v1_3(EntityAI ai, long chip_id, long target_leek_id, Object value3) throws LeekRunException {

		Entity target = ai.getFight().getEntity(ai.integer(target_leek_id));
		// On récupère le sort
		Chip template = Chips.getChip(ai.integer(chip_id));
		if (target == null || target.getCell() == null || template == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else {
			cells_to_ignore.add(ai.getEntity().getCell());
		}
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target.getCell(), cells_to_ignore);

		var retour = new LegacyArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(ai, (long) cell.getId());
			}
		}
		return retour;
	}

	public static ArrayLeekValue getCellsToUseChip(EntityAI ai, long chip_id, long target_leek_id, Object value3) throws LeekRunException {

		Entity target = ai.getFight().getEntity(ai.integer(target_leek_id));
		// On récupère le sort
		Chip template = Chips.getChip(ai.integer(chip_id));
		if (target == null || target.getCell() == null || template == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else {
			cells_to_ignore.add(ai.getEntity().getCell());
		}
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target.getCell(), cells_to_ignore);

		var retour = new ArrayLeekValue(ai, possible.size());
		for (Cell cell : possible) {
			retour.push(ai, (long) cell.getId());
		}
		return retour;
	}

	public static LegacyArrayLeekValue getCellsToUseChipOnCell_v1_3(EntityAI ai, long chip_id, long target_cell_id) throws LeekRunException {
		return getCellsToUseChipOnCell_v1_3(ai, chip_id, target_cell_id, null);
	}

	public static ArrayLeekValue getCellsToUseChipOnCell(EntityAI ai, long chip_id, long target_cell_id) throws LeekRunException {
		return getCellsToUseChipOnCell(ai, chip_id, target_cell_id, null);
	}

	/**
	 * Renvoie la liste des cellules à partir desquelles on peut utiliser la
	 * puce chip_id, sur la cellule cible
	 *
	 * @param chip_id
	 *            Puce à tester
	 * @param target_cell_id
	 *            Cellule cible
	 * @return Liste des cellules
	 * @throws LeekRunException
	 */
	public static LegacyArrayLeekValue getCellsToUseChipOnCell_v1_3(EntityAI ai, long chip_id, long target_cell_id, Object value3) throws LeekRunException {

		Cell target = ai.getFight().getMap().getCell(ai.integer(target_cell_id));
		// On récupère le sort
		Chip template = Chips.getChip(ai.integer(chip_id));
		if (target == null || template == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target, cells_to_ignore);

		var retour = new LegacyArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(ai, (long) cell.getId());
			}
		}
		return retour;
	}

	public static ArrayLeekValue getCellsToUseChipOnCell(EntityAI ai, long chip_id, long target_cell_id, Object value3) throws LeekRunException {

		Cell target = ai.getFight().getMap().getCell(ai.integer(target_cell_id));
		// On récupère le sort
		Chip template = Chips.getChip(ai.integer(chip_id));
		if (target == null || template == null || ai.getEntity().getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof GenericArrayLeekValue) {
			ai.putCells(cells_to_ignore, (GenericArrayLeekValue) value3);
		} else
			cells_to_ignore.add(ai.getEntity().getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target, cells_to_ignore);

		var retour = new ArrayLeekValue(ai, possible.size());
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(ai, (long) cell.getId());
			}
		}
		return retour;
	}

	/**
	 * Retourne l'ennemi le plus proche du leek fournis en paramètre
	 *
	 * @param leek_id
	 *            Leek cible
	 * @return Ennemi le plus proche
	 */
	public static Object getNearestEnemyTo(EntityAI ai, long leek_id) {

		List<Entity> entities = ai.getFight().getEnemiesEntities(ai.getEntity().getTeam());

		Entity entity = ai.getFight().getEntity(leek_id);
		if (entity == null || entity.getCell() == null)
			return null;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead())
				continue;
			if (entity == l)
				continue;
			if (l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(entity.getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? null : (long) nearest.getFId();
	}

	/**
	 * Retourne l'ennemi le plus proche de la cellule fourni en paramètre
	 *
	 * @param cell_id
	 *            Cellule cible
	 * @return Ennemi le plus proche
	 */
	public static Object getNearestEnemyToCell(EntityAI ai, long cell_id) {

		List<Entity> entities = ai.getFight().getEnemiesEntities(ai.getEntity().getTeam());

		Cell cell = ai.getFight().getMap().getCell((int) cell_id);
		if (cell == null)
			return null;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(cell, l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? null : (long) nearest.getFId();
	}

	/**
	 * Retourne l'allié le plus proche du leek fournis en paramètre
	 *
	 * @param leek_id
	 *            Leek cible
	 * @return Allié le plus proche
	 */
	public static Object getNearestAllyTo(EntityAI ai, long leek_id) {
		List<Entity> entities = ai.getFight().getTeamEntities(ai.getEntity().getTeam() == 2 ? 2 : 1);
		Entity entity = ai.getFight().getEntity(leek_id);
		if (entity == null || entity.getCell() == null)
			return null;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead())
				continue;
			if (entity == l || l == ai.getEntity())
				continue;
			int d = Pathfinding.getDistance2(entity.getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? null : (long) nearest.getFId();
	}

	/**
	 * Retourne l'allié le plus proche de la cellule fournie en paramètre
	 *
	 * @param cell_id
	 *            Cellule cible
	 * @return C le plus proche
	 */
	public static Object getNearestAllyToCell(EntityAI ai, long cell_id) {
		List<Entity> entities = ai.getFight().getTeamEntities(ai.getEntity().getTeam() == 2 ? 2 : 1);
		Cell cell = ai.getFight().getMap().getCell((int) cell_id);
		if (cell == null)
			return null;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead())
				continue;
			if (l == ai.getEntity())
				continue;
			int d = Pathfinding.getDistance2(cell, l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}

		return nearest == null ? null : (long) nearest.getFId();
	}

	public static long getFightType(EntityAI ai) {
		return ai.getFight().getType();
	}

	public static long getFightContext(EntityAI ai) {
		return ai.getFight().getContext();
	}

	public static long getFightID(EntityAI ai) {
		return ai.getFight().getId();
	}
}
