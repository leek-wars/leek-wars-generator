package com.leekwars.generator.classes;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;

public class FieldClass {

	public static double getDistance(EntityAI ai, long c1, long c2) {
		Cell cell1 = ai.getFight().getMap().getCell((int) c1);
		if (cell1 == null)
			return -1;
		Cell cell2 = ai.getFight().getMap().getCell((int) c2);
		if (cell2 == null)
			return -1;
		return Pathfinding.getDistance(cell1, cell2);
	}

	public static long getCellDistance(EntityAI ai, long c1, long c2) {
		Cell cell1 = ai.getFight().getMap().getCell((int) c1);
		if (cell1 == null)
			return -1;
		Cell cell2 = ai.getFight().getMap().getCell((int) c2);
		if (cell2 == null)
			return -1;
		return Pathfinding.getCaseDistance(cell1, cell2);
	}

	public static Object getPathLength(EntityAI ai, long c1, long c2) throws LeekRunException {
		return getPathLength(ai, c1, c2, null);
	}

	public static Object getPathLength(EntityAI ai, long c1, long c2, Object leeks_to_ignore) throws LeekRunException {

		ai.ops(100);

		Cell cell1 = ai.getFight().getMap().getCell((int) c1);
		if (cell1 == null) {
			return null;
		}
		Cell cell2 = ai.getFight().getMap().getCell((int) c2);
		if (cell2 == null) {
			return null;
		}
		if (cell1 == cell2) {
			return 0l;
		}

		// Opérations
		var distance = Pathfinding.getCaseDistance(cell1, cell2);
		ai.ops(distance * distance * 20);

		List<Cell> ignore = new ArrayList<Cell>();

		if (leeks_to_ignore != null && leeks_to_ignore instanceof GenericArrayLeekValue) {
			ai.putCells(ignore, (GenericArrayLeekValue) leeks_to_ignore);
		}

		var path = ai.getFight().getMap().getPathBeetween(ai, cell1, cell2, ignore);
		if (path == null) {
			return null;
		}
		return (long) path.size();
	}

	public static LegacyArrayLeekValue getPath_v1_3(EntityAI ai, long c1, long c2) throws LeekRunException {
		return getPath_v1_3(ai, c1, c2, null);
	}

	public static ArrayLeekValue getPath(EntityAI ai, long c1, long c2) throws LeekRunException {
		return getPath(ai, c1, c2, null);
	}

	public static LegacyArrayLeekValue getPath_v1_3(EntityAI ai, long c1, long c2, Object leeks_to_ignore) throws LeekRunException {

		Cell cell1 = ai.getFight().getMap().getCell((int) c1);
		if (cell1 == null)
			return null;
		Cell cell2 = ai.getFight().getMap().getCell((int) c2);
		if (cell2 == null)
			return null;

		if (cell1 == cell2)
			return new LegacyArrayLeekValue();

		// Opérations
		var distance = Pathfinding.getCaseDistance(cell1, cell2);
		ai.ops(distance * distance * 20);

		List<Cell> ignore = new ArrayList<Cell>();

		if (leeks_to_ignore instanceof GenericArrayLeekValue) {
			ai.putCells(ignore, (GenericArrayLeekValue) leeks_to_ignore);
		} else if (leeks_to_ignore instanceof Number) {
			ai.getLogs().addLog(FarmerLog.WARNING,
					"Attention, la fonction getPath(Cell start, Cell end, Leek leek_to_ignore) va disparaitre, il faut désormais utiliser un tableau de cellules à ignorer.");
			Entity l = ai.getFight().getEntity(ai.integer(leeks_to_ignore));
			if (l != null && l.getCell() != null) {
				ignore.add(l.getCell());
			}
		}
		List<Cell> path = ai.getFight().getMap().getPathBeetween(ai, cell1, cell2, ignore);
		if (path == null)
			return null;
		var retour = new LegacyArrayLeekValue();
		for (int i = 0; i < path.size(); i++) {
			retour.push(ai, (long) path.get(i).getId());
		}
		return retour;
	}

	public static ArrayLeekValue getPath(EntityAI ai, long c1, long c2, Object leeks_to_ignore) throws LeekRunException {

		Cell cell1 = ai.getFight().getMap().getCell((int) c1);
		if (cell1 == null)
			return null;
		Cell cell2 = ai.getFight().getMap().getCell((int) c2);
		if (cell2 == null)
			return null;

		if (cell1 == cell2)
			return new ArrayLeekValue(ai);

		// Opérations
		var distance = Pathfinding.getCaseDistance(cell1, cell2);
		ai.ops(distance * distance * 20);

		List<Cell> ignore = new ArrayList<Cell>();

		if (leeks_to_ignore instanceof GenericArrayLeekValue) {
			ai.putCells(ignore, (GenericArrayLeekValue) leeks_to_ignore);
		} else if (leeks_to_ignore instanceof Number) {
			ai.getLogs().addLog(FarmerLog.WARNING,
					"Attention, la fonction getPath(Cell start, Cell end, Leek leek_to_ignore) va disparaitre, il faut désormais utiliser un tableau de cellules à ignorer.");
			Entity l = ai.getFight().getEntity(ai.integer(leeks_to_ignore));
			if (l != null && l.getCell() != null) {
				ignore.add(l.getCell());
			}
		}
		List<Cell> path = ai.getFight().getMap().getPathBeetween(ai, cell1, cell2, ignore);
		if (path == null)
			return null;
		var retour = new ArrayLeekValue(ai, path.size());
		for (int i = 0; i < path.size(); i++) {
			retour.push(ai, (long) path.get(i).getId());
		}
		return retour;
	}

	public static long getLeekOnCell(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return -1;
		return cell.getPlayer() != null ? cell.getPlayer().getFId() : -1;
	}

	public static long getEntityOnCell(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return -1;
		return cell.getPlayer() != null ? cell.getPlayer().getFId() : -1;
	}

	public static long getCellContent(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return -1;
		return !cell.isWalkable() ? 2 : (cell.getPlayer() != null ? 1 : 0);
	}

	public static Object getCellFromXY(EntityAI ai, long x, long y) {
		Cell cell = ai.getFight().getMap().getCell((int) x + ai.getFight().getMap().getWidth() - 1, (int) y);
		if (cell == null)
			return null;
		return (long) cell.getId();
	}

	public static Object getCellX(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return null;
		return (long) cell.getX() - ai.getFight().getMap().getWidth() + 1;
	}

	public static Object getCellY(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return null;
		return (long) cell.getY();
	}

	public static boolean isEmptyCell(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return false;
		return cell.available();
	}

	public static boolean isObstacle(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return true;
		return !cell.isWalkable();
	}

	public static boolean isLeek(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return false;
		return cell.getPlayer() != null;
	}

	public static boolean isEntity(EntityAI ai, long c) {
		Cell cell = ai.getFight().getMap().getCell((int) c);
		if (cell == null)
			return false;
		return cell.getPlayer() != null;
	}

	public static boolean isOnSameLine(EntityAI ai, long c1, long c2) {
		Cell cell1 = ai.getFight().getMap().getCell((int) c1);
		if (cell1 == null)
			return false;
		Cell cell2 = ai.getFight().getMap().getCell((int) c2);
		if (cell2 == null)
			return false;
		return cell1.getX() == cell2.getX() || cell1.getY() == cell2.getY();
	}

	public static Object lineOfSight(EntityAI ai, long start, long end) throws LeekRunException {
		return lineOfSight(ai, start, end, null);
	}

	public static Object lineOfSight(EntityAI ai, long start, long end, Object ignore) throws LeekRunException {

		Cell s = ai.getFight().getMap().getCell((int) start);
		Cell e = ai.getFight().getMap().getCell((int) end);

		if (s == null || e == null)
			return null;

		if (ignore instanceof Number) {

			Entity l = ai.getFight().getEntity(ai.integer(ignore));
			List<Cell> cells = new ArrayList<Cell>();
			if (l != null && l.getCell() != null)
				cells.add(l.getCell());
			return Pathfinding.verifyLoS(s, e, null, cells);

		} else if (ignore instanceof LegacyArrayLeekValue) {

			List<Cell> cells = new ArrayList<Cell>();
			if (ai.getEntity().getCell() != null)
				cells.add(ai.getEntity().getCell());
			for (var value : (LegacyArrayLeekValue) ignore) {
				if (value.getValue() instanceof Number) {
					Entity l = ai.getFight().getEntity(ai.integer(value.getValue()));
					if (l != null && l.getCell() != null) {
						cells.add(l.getCell());
					}
				}
			}
			return Pathfinding.verifyLoS(s, e, null, cells);

		} else if (ignore instanceof ArrayLeekValue) {

			List<Cell> cells = new ArrayList<Cell>();
			if (ai.getEntity().getCell() != null)
				cells.add(ai.getEntity().getCell());
			for (var value : (ArrayLeekValue) ignore) {
				if (value instanceof Number) {
					Entity l = ai.getFight().getEntity(ai.integer(value));
					if (l != null && l.getCell() != null) {
						cells.add(l.getCell());
					}
				}
			}
			return Pathfinding.verifyLoS(s, e, null, cells);

		} else {

			List<Cell> cells = new ArrayList<Cell>();
			cells.add(ai.getEntity().getCell());
			return Pathfinding.verifyLoS(s, e, null, cells);
		}
	}

	public static LegacyArrayLeekValue getObstacles_v1_3(EntityAI ai) throws LeekRunException {
		Cell[] cells = ai.getFight().getMap().getObstacles();
		// On ajoute les cases
		var retour = new LegacyArrayLeekValue();
		if (cells == null) return retour;
		for (Cell c : cells)
			retour.push(ai, (long) c.getId());
		return retour;
	}

	public static ArrayLeekValue getObstacles(EntityAI ai) throws LeekRunException {
		Cell[] cells = ai.getFight().getMap().getObstacles();
		// On ajoute les cases
		var retour = new ArrayLeekValue(ai);
		if (cells == null) return retour;
		for (Cell c : cells)
			retour.push(ai, (long) c.getId());
		return retour;
	}

	public static long getMapType(EntityAI ai) {
		// Nexus (the first map) is -1 so it's + 2
		return ai.getFight().getMap().getType() + 2;
	}
}
