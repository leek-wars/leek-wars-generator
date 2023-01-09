package com.leekwars.generator.maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.area.Area;
import com.leekwars.generator.fight.entity.EntityAI;

public class Pathfinding {

	public final static byte NORTH = 0;// NE
	public final static byte EAST = 1;// SE
	public final static byte SOUTH = 2;// SO
	public final static byte WEST = 3;// NO

	private final static boolean DEBUG = false;

	public static boolean canUseAttack(Cell caster, Cell target, Attack attack) {
		// Portée
		if (!verifyRange(caster, target, attack)) {
			return false;
		}
		// Ligne de vue
		return verifyLoS(caster, target, attack, caster);
	}

	public static boolean verifyRange(Cell caster, Cell target, Attack attack) {

		if (target == null || caster == null) {
			return false;
		}
		int dx = caster.getX() - target.getX();
		int dy = caster.getY() - target.getY();
		int distance = Math.abs(dx) + Math.abs(dy);

		// Pour tous les types : vérification de la distance
		if (distance > attack.getMaxRange() || distance < attack.getMinRange()) {
			return false;
		}
		// Même cellule, OK
		if (caster == target) return true;

		// Vérification de chaque type de lancé
		if ((attack.getLaunchType() & 1) == 0 && (dx == 0 || dy == 0)) return false; // Ligne
		if ((attack.getLaunchType() & 2) == 0 && Math.abs(dx) == Math.abs(dy)) return false; // Diagonale
		if ((attack.getLaunchType() & 4) == 0 && Math.abs(dx) != Math.abs(dy) && dx != 0 && dy != 0) return false; // Reste

		return true;
	}

	public static List<Cell> getValidCellsAroundObstacle(Cell cell) {
		Map map = cell.getMap();
		List<Cell> retour = new ArrayList<Cell>();
		int size = 1;
		List<Cell> close = new ArrayList<Cell>();
		close.add(cell);

		for (int i = 1; i <= size; i++) {
			boolean stop = true;
			for (int j = 0; j < i; j++) {
				stop = addValidCell(retour, close, map.getCell(cell.getX() + j, cell.getY() + (i - j)), cell) && stop;
				stop = addValidCell(retour, close, map.getCell(cell.getX() - j, cell.getY() - (i - j)), cell) && stop;
				stop = addValidCell(retour, close, map.getCell(cell.getX() + i - j, cell.getY() - j), cell) && stop;
				stop = addValidCell(retour, close, map.getCell(cell.getX() - i + j, cell.getY() + j), cell) && stop;
			}
			if (!stop && size < 5)
				size++;
		}
		return retour;
	}

	private static boolean addValidCell(List<Cell> retour, List<Cell> close, Cell c, Cell center) {
		if (c == null) {
			return true;
		}
		int dx = (int) Math.signum(center.getX() - c.getX());
		int dy = (int) Math.signum(center.getY() - c.getY());

		Cell c1 = c.getMap().getCell(c.getX() + dx, c.getY());
		Cell c2 = c.getMap().getCell(c.getX(), c.getY() + dy);

		if (!c.isWalkable()) {
			if ((c1 != null && !c1.isWalkable() && close.contains(c1)) || (c2 != null && !c2.isWalkable() && close.contains(c2))) {
				close.add(c);
				return false;
			}
		} else {
			if ((c1 != null && !c1.isWalkable() && close.contains(c1)) || (c2 != null && !c2.isWalkable() && close.contains(c2))) {
				retour.add(c);
			}
		}
		return true;
	}

	public static List<Cell> getPossibleCastCellsForTarget(Attack attack, Cell target, List<Cell> cells_to_ignore) {

		long start = 0;
		if (DEBUG) {
			start = System.currentTimeMillis();
		}
		if (target == null) {
			return null;
		}
		List<Cell> possible = new ArrayList<Cell>();

		if (target.isWalkable()) {

			if (attack.getLaunchType() == Attack.LAUNCH_TYPE_LINE) {
				var line = new boolean[] { true, true, true, true };
				int x = target.getX(), y = target.getY();
				var dirs = new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
				Cell c;
				for (int i = 0; i <= attack.getMaxRange(); i++) {
					for (int dir = 0; dir < 4; dir++) {
						if (!line[dir])
							continue;
						c = target.getMap().getCell(x + i * dirs[dir][0], y + i * dirs[dir][1]);
						if (c == null)
							line[dir] = false;
						else {
							if (attack.needLos() && !available(c, cells_to_ignore) && i > 0)
								line[dir] = false;
							else if (attack.needLos() && !c.isWalkable())
								line[dir] = false;
							else if (attack.getMinRange() <= i && available(c, cells_to_ignore))
								possible.add(c);
						}
					}
				}
			} else {
				var mask = MaskAreaCell.generateMask(attack.getLaunchType(), attack.getMinRange(), attack.getMaxRange());
				int x = target.getX();
				int y = target.getY();
				Cell cell;
				for (var mask_cell : mask) {
					cell = target.getMap().getCell(x + mask_cell[0], y + mask_cell[1]);
					if (cell == null || !available(cell, cells_to_ignore))
						continue;
					if (!verifyLoS(cell, target, attack, cells_to_ignore))
						continue;
					possible.add(cell);
				}
			}
		}
		if (DEBUG) {
			System.out.println("Time : " + (System.currentTimeMillis() - start));
		}
		return possible;
	}

	public static boolean verifyLoS(Cell start, Cell end, Attack attack, Cell leek_cell) {

		List<Cell> ignoredCells = new ArrayList<Cell>();
		ignoredCells.add(leek_cell);

		// Ignore first entity in area for Area first in line
		if (attack.getArea() == Area.TYPE_FIRST_IN_LINE) {
			Cell cell = Pathfinding.getFirstEntity(start, end, attack.getMinRange(), attack.getMaxRange());
			if (cell != null) {
				ignoredCells.add(cell);
			}
		}
		return verifyLoS(start, end, attack, ignoredCells);
	}

	public static boolean verifyLoS(Cell start, Cell end, Attack attack, List<Cell> ignoredCells) {

		boolean needLos = attack == null ? true : attack.needLos();
		if (!needLos) {
			return true;
		}

		int a = Math.abs(start.getY() - end.getY());
		int b = Math.abs(start.getX() - end.getX());
		int dx = start.getX() > end.getX() ? -1 : 1;
		int dy = start.getY() < end.getY() ? 1 : -1;
		List<Integer> path = new ArrayList<Integer>((b + 1) * 2);

		if (b == 0) {
			path.add(0);
			path.add(a + 1);
		} else {
			double d = (double) a / (double) b / 2.0;
			int h = 0;
			for (int i = 0; i < b; ++i) {
				double y = 0.5 + (i * 2 + 1) * d;
				path.add(h);
				path.add((int) Math.ceil(y - 0.00001) - h);
				h = (int) Math.floor(y + 0.00001);
			}
			path.add(h);
			path.add(a + 1 - h);
		}

		for (int p = 0; p < path.size(); p += 2) {
			for (int i = 0; i < path.get(p + 1); ++i) {

				Cell cell = start.getMap().getCell(start.getX() + (p / 2) * dx, start.getY() + (path.get(p) + i) * dy);

				if (cell == null)
					return false;

				if (needLos) {
					if (!cell.isWalkable()) {
						return false;
					}
					if (!cell.available()) {
						// Première ou dernière cellule occupée par quelqu'un,
						// c'est OK
						if (cell.getId() == start.getId()) {
							continue;
						}
						if (cell.getId() == end.getId()) {
							return true;
						}
						if (!ignoredCells.contains(cell)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public static boolean inLine(Cell c1, Cell c2) {
		return c1.getX() == c2.getX() || c1.getY() == c2.getY();
	}

	public static int getAverageDistance2(Cell c1, List<Cell> cells) {
		int dist = 0;
		for (Cell c2 : cells) {
			dist += (c1.getX() - c2.getX()) * (c1.getX() - c2.getX()) + (c1.getY() - c2.getY()) * (c1.getY() - c2.getY());
		}
		return dist / cells.size();
	}

	public static int getDistance2(Cell c1, List<Cell> cells) {
		int dist = -1;
		for (Cell c2 : cells) {
			int d = (c1.getX() - c2.getX()) * (c1.getX() - c2.getX()) + (c1.getY() - c2.getY()) * (c1.getY() - c2.getY());
			if (dist == -1 || d < dist)
				dist = d;
		}
		return dist;
	}

	public static int getDistance2(Cell c1, Cell c2) {
		return (c1.getX() - c2.getX()) * (c1.getX() - c2.getX()) + (c1.getY() - c2.getY()) * (c1.getY() - c2.getY());
	}

	public static double getDistance(Cell c1, Cell c2) {
		return Math.sqrt(getDistance2(c1, c2));
	}

	public static int getCaseDistance(Cell c1, Cell c2) {
		return Math.abs(c1.getX() - c2.getX()) + Math.abs(c1.getY() - c2.getY());
	}

	public static int getCaseDistance(Cell c1, List<Cell> cells) {
		int dist = -1;
		for (Cell c2 : cells) {
			int d = Math.abs(c1.getX() - c2.getX()) + Math.abs(c1.getY() - c2.getY());
			if (dist == -1 || d < dist)
				dist = d;
		}
		return dist;
	}

	public static Cell getCellByDir(Cell c, byte dir) {

		if (c == null)
			return null;

		if (dir == NORTH && c.hasNorth())
			return c.getMap().getCell(c.getId() - c.getMap().getWidth() + 1);
		else if (dir == WEST && c.hasWest())
			return c.getMap().getCell(c.getId() - c.getMap().getWidth());
		else if (dir == EAST && c.hasEast())
			return c.getMap().getCell(c.getId() + c.getMap().getWidth());
		else if (dir == SOUTH && c.hasSouth())
			return c.getMap().getCell(c.getId() + c.getMap().getWidth() - 1);
		return null;
	}

	public static Cell[] getCellsAround(Cell c) {
		return new Cell[] { getCellByDir(c, SOUTH), getCellByDir(c, WEST), getCellByDir(c, NORTH), getCellByDir(c, EAST) };
	}

	public static List<Cell> getPathTowardLine(EntityAI ai, Cell start, Cell linecell1, Cell linecell2) {
		// Crée un path pour aller plus près de la ligne partant de linecell1 et
		// passant par linecell2

		// On crée la liste des cellules à rejoindre
		List<Cell> line_cell = new ArrayList<Cell>();
		// On trouve la ligne
		int dx = (int) Math.signum(linecell2.getX() - linecell1.getX());
		int dy = (int) Math.signum(linecell2.getY() - linecell1.getY());
		if (dx == 0 && dy == 0)
			return null;
		// On prolonge la ligne
		Cell curent = linecell1;
		while (curent != null) {
			line_cell.add(curent);
			curent = start.getMap().getCell(curent.getX() + dx, curent.getY() + dy);
		}
		curent = start.getMap().getCell(linecell1.getX() - dx, linecell1.getY() - dy);
		while (curent != null) {
			line_cell.add(curent);
			curent = start.getMap().getCell(curent.getX() - dx, curent.getY() - dy);
		}
		// Puis on crée un path qui va vers de ces cellules
		return getAStarPath(ai, start, line_cell);
	}

	public static List<Cell> getPathAwayFromLine(EntityAI ai, Cell start, Cell linecell1, Cell linecell2, int max_distance) {
		// Crée un path pour partir loin de la ligne partant de linecell1 et
		// passant par linecell2

		if (start == null) {
			return null;
		}

		// On crée la liste des cellules à fuir
		List<Cell> line_cell = new ArrayList<Cell>();
		// On trouve la ligne
		int dx = (int) Math.signum(linecell2.getX() - linecell1.getX());
		int dy = (int) Math.signum(linecell2.getY() - linecell1.getY());
		if (dx == 0 && dy == 0)
			return null;
		// On prolonge la ligne
		Cell current = linecell1;
		while (current != null) {
			line_cell.add(current);
			current = start.getMap().getCell(current.getX() + dx, current.getY() + dy);
		}
		current = start.getMap().getCell(linecell1.getX() - dx, linecell1.getY() - dy);
		while (current != null) {
			line_cell.add(current);
			current = start.getMap().getCell(current.getX() - dx, current.getY() - dy);
		}
		// Puis on crée un path qui va loin de ces cellules

		List<Cell> cells = getPathAway(ai, start, line_cell, max_distance);

		return cells;
	}

	public static List<Cell> getPathAway(EntityAI ai, Cell start, List<Cell> bad_cells, int max_distance) {
		long startt;
		if (DEBUG)
			startt = System.currentTimeMillis();
		if (start == null)
			return null;
		int curent_distance = getDistance2(start, bad_cells);
		List<CellDistance> potential_targets = new ArrayList<CellDistance>();
		int[][] cells = MaskAreaCell.generateCircleMask(1, max_distance);
		if (cells == null)
			return null;
		int x = start.getX(), y = start.getY();
		for (int i = 0; i < cells.length; i++) {
			Cell c = start.getMap().getCell(x + cells[i][0], y + cells[i][1]);
			if (c == null || !c.available())
				continue;
			int distance = getDistance2(c, bad_cells);
			if (distance > curent_distance)
				potential_targets.add(new CellDistance(c, distance));
		}
		if (potential_targets.size() == 0)
			return null;
		Collections.sort(potential_targets, new CellDistanceComparator());
		List<Cell> path = null;
		for (CellDistance c : potential_targets) {
			// Calcule des path
			path = getAStarPath(ai, start, new Cell[] { c.getCell() });
			if (path != null && path.size() <= max_distance)
				break;
			else
				path = null;
		}
		if (DEBUG)
			System.out.println("Time : " + (System.currentTimeMillis() - startt));
		return path;
	}

	public static List<Cell> getPathAwayMin(EntityAI ai, Cell start, List<Cell> bad_cells, int max_distance) {
		long startt;
		if (DEBUG)
			startt = System.currentTimeMillis();
		int curent_distance = getDistance2(start, bad_cells);
		List<CellDistance> potential_targets = new ArrayList<CellDistance>();
		int[][] cells = MaskAreaCell.generateCircleMask(1, max_distance);
		int x = start.getX(), y = start.getY();
		for (int i = 0; i < cells.length; i++) {
			Cell c = start.getMap().getCell(x + cells[i][0], y + cells[i][1]);
			if (c == null || !c.available())
				continue;
			int distance = getDistance2(c, bad_cells);
			if (distance > curent_distance)
				potential_targets.add(new CellDistance(c, distance));
		}
		if (potential_targets.size() == 0)
			return null;
		Collections.sort(potential_targets, new CellDistanceComparator());
		List<Cell> path = null;
		for (CellDistance c : potential_targets) {
			// Calcule des path
			path = getAStarPath(ai, start, new Cell[] { c.getCell() });
			if (path != null && path.size() <= max_distance)
				break;
			else
				path = null;
		}
		if (DEBUG)
			System.out.println("Time : " + (System.currentTimeMillis() - startt));
		return path;
	}

	public static boolean available(Cell c, List<Cell> cells_to_ignore) {
		if (c == null)
			return false;
		if (c.available())
			return true;
		if (cells_to_ignore != null && cells_to_ignore.contains(c))
			return true;
		return false;
	}

	public static List<Cell> getAStarPath(EntityAI ai, Cell c1, Cell[] cell, List<Cell> cells_to_ignore) {
		return getAStarPath(ai, c1, Arrays.asList(cell), cells_to_ignore);
	}

	public static List<Cell> getAStarPath(EntityAI ai, Cell c1, Cell[] cell) {
		return getAStarPath(ai, c1, Arrays.asList(cell), null);
	}

	public static List<Cell> getAStarPath(EntityAI ai, Cell c1, List<Cell> endCells) {
		return getAStarPath(ai, c1, endCells, null);
	}

	public static List<Cell> getAStarPath(EntityAI ai, Cell c1, List<Cell> endCells, List<Cell> cells_to_ignore) {
		if (c1 == null || endCells == null || endCells.isEmpty())
			return null;
		if (endCells.contains(c1))
			return null;

		for (Cell c : c1.getMap().getCells()) {
			c.visited = false;
			c.closed = false;
			c.cost = Short.MAX_VALUE;
		}

		TreeSet<Cell> open = new TreeSet<>(new Comparator<Cell>() {
			@Override
			public int compare(Cell o1, Cell o2) {
				return o1.weight > o2.weight ? 1 : -1;
			}
		});
		c1.cost = 0;
		c1.weight = 0;
		c1.visited = true;
		open.add(c1);

		while (open.size() > 0) {
			Cell u = open.pollFirst();
			u.closed = true;

			if (endCells.contains(u)) {
				List<Cell> result = new ArrayList<>();
				int s = u.cost;
				while (s-- >= 1) {
					result.add(u);
					u = u.parent;
				}
				Collections.reverse(result);
				Cell last = result.get(result.size() - 1);
				if (last.getPlayer() != null && (cells_to_ignore == null || !cells_to_ignore.contains(last))) {
					result.remove(result.size() - 1);
				}
				return result;
			}

			for (Cell c : getCellsAround(u)) {
				if (c == null || c.closed || !c.isWalkable()) continue;
				if (c.getPlayer() != null && (cells_to_ignore == null || !cells_to_ignore.contains(c)) && !endCells.contains(c)) continue;

				if (!c.visited || u.cost + 1 < c.cost) {
					c.cost = (short) (u.cost + 1);
					c.weight = c.cost + (float) getDistance(c, endCells.get(0));
					c.parent = u;
					if (!c.visited) {
						open.add(c);
						c.visited = true;
					}
				}
			}
		}
		// System.out.println("No path found!");
		return null;
	}

	public static List<Cell> getOldAStarPath(Cell c1, List<Cell> endCells, List<Cell> cells_to_ignore) {
		long start;
		if (DEBUG)
			start = System.currentTimeMillis();
		if (c1 == null || endCells == null || endCells.isEmpty())
			return null;
		TreeMap<Integer, Node> closed_list = new TreeMap<Integer, Node>();
		Cell[] cells, cs;
		if (endCells.contains(c1))
			return null;
		Node curent_node = null;
		Node node;
		Node antecedant;
		Cell c_1, c_2, c;
		closed_list.put(c1.getId(), new Node(c1, getDistance2(c1, endCells)));
		List<Node> analysed = new ArrayList<Node>();
		int count = 0;
		boolean stop = false;
		Node end;
		while (count < 1000 && !stop) {
			count++;
			// Recherche du curent node
			int poid = 9999999;
			curent_node = null;
			for (Node n : closed_list.values()) {
				if (n.getPoid() < poid && !analysed.contains(n)) {
					poid = n.getPoid();
					curent_node = n;
				}
			}
			if (curent_node == null)
				break;
			analysed.add(curent_node);
			cells = getCellsAround(curent_node.getCell());
			for (int i = 0; i < 4; i++) {
				if (cells[i] == null)
					continue;
				if (endCells.contains(cells[i]))
					stop = true;
				if (!available(cells[i], cells_to_ignore) && !stop)
					continue;
				node = closed_list.get(cells[i].getId());
				if (node == null) {
					Node new_node = new Node(cells[i], getDistance2(cells[i], endCells));
					new_node.setParent(curent_node, curent_node.getParcouru() + 1);
					closed_list.put(cells[i].getId(), new_node);
					if (endCells.contains(cells[i]))
						stop = true;
				} else {
					if (node.getParcouru() > curent_node.getParcouru() + 1) {
						node.setParent(curent_node, curent_node.getParcouru() + 1);
						analysed.remove(node);
					}
				}
			}
			// On regarde les diagonales
			if (!stop) {
				for (int i = 0; i < 4; i++) {
					c_1 = cells[i];
					c_2 = cells[(i + 1) % 4];
					if (c_1 == null && c_2 == null)
						continue;
					if ((c_1 == null || !available(c_1, cells_to_ignore)) && (c_2 == null || !available(c_2, cells_to_ignore)))
						continue;
					c = getCellByDir(c_1, (byte) ((i + 1) % 4));
					if (c == null)
						continue;
					if (endCells.contains(c))
						stop = true;
					if (!stop && !available(c, cells_to_ignore))
						continue;

					antecedant = null;
					if (c_1 != null && available(c_1, cells_to_ignore))
						antecedant = closed_list.get(c_1.getId());
					if (antecedant == null || (c_2 != null && available(c_2, cells_to_ignore) && antecedant.getPoid() > closed_list.get(c_2.getId()).getPoid())) {
						antecedant = closed_list.get(c_2.getId());
					}
					if (antecedant == null)
						continue;
					node = closed_list.get(c.getId());
					if (node == null) {
						Node new_node = new Node(c, getDistance2(c, endCells));
						new_node.setParent(antecedant, antecedant.getParcouru() + 1);
						closed_list.put(c.getId(), new_node);
					} else {
						if (node.getParcouru() > antecedant.getParcouru() + 1) {
							node.setParent(antecedant, antecedant.getParcouru() + 1);
							analysed.remove(node);
						}
					}
				}
			}
		}
		end = null;
		for (Cell cell : endCells) {
			end = closed_list.get(cell.getId());
			if (end != null)
				break;
		}
		if (end == null)
			return null;
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.ensureCapacity(end.getParcouru() + 1);
		while (end != null) {
			nodes.add(0, end);
			end = end.getParent();
		}

		for (Node n : nodes) {
			cs = getCellsAround(n.getCell());
			for (int i = 0; i < 4; i++) {
				for (Node n2 : nodes) {
					if (n2 == n)
						continue;
					if (cs[i] != n2.getCell())
						continue;
					if (n2.getParent() == n || n.getParent() == n2)
						continue;
					if (n2.getParcouru() + 1 < n.getParcouru())
						n.setParent(n2, n2.getParcouru() + 1);
				}
			}
		}

		ArrayList<Cell> retour = new ArrayList<Cell>();
		retour.ensureCapacity(nodes.size() - 1);
		end = nodes.get(nodes.size() - 1);
		if (!available(end.getCell(), cells_to_ignore))
			end = end.getParent();
		while (end.getParent() != null) {
			retour.add(0, end.getCell());
			end = end.getParent();
		}
		if (DEBUG)
			System.out.println("Time : " + (System.currentTimeMillis() - start) + " It :" + count);
		return retour;
	}

	private static class CellDistanceComparator implements Comparator<CellDistance> {
		@Override
		public int compare(CellDistance cell1, CellDistance cell2) {
			if (cell1.getDistance() > cell2.getDistance())
				return -1;
			else if (cell1.getDistance() == cell2.getDistance())
				return 0;
			return 1;
		}

	}

	private static class CellDistance {

		private final Cell mCell;
		private final int mDistance;

		public CellDistance(Cell cell, int distance) {
			mCell = cell;
			mDistance = distance;
		}

		public Cell getCell() {
			return mCell;
		}

		public int getDistance() {
			return mDistance;
		}
	}

	private static class Node {

		private final Cell cell;
		private Node parent;
		private final double distance;
		private int parcouru;
		private int poid;

		public Node(Cell cell, double distance) {
			this.cell = cell;
			this.distance = (int) (distance);
			this.poid = (int) distance * 5;
		}

		public void setParent(Node parent, int parcouru) {
			this.parcouru = parcouru;
			this.parent = parent;
			this.poid = (int) distance * 5 + parcouru;
		}

		public Cell getCell() {
			return cell;
		}

		public int getParcouru() {
			return parcouru;
		}

		public int getPoid() {
			return poid;
		}

		public Node getParent() {
			return parent;
		}
	}

	public static Cell getFirstEntity(Cell from, Cell target, int minRange, int maxRange) {
		int dx = (int) Math.signum(target.x - from.x);
		int dy = (int) Math.signum(target.y - from.y);
		Cell current = from.next(dx, dy);
		int range = 1;
		while (current != null && current.isWalkable() && range <= maxRange) {
			if (range >= minRange && current.getPlayer() != null) {
				return current;
			}
			current = current.next(dx, dy);
			range++;
		}
		return null;
	}

	public static Cell getPushLastAvailableCell(Cell entity, Cell target, Cell caster) {
		// Delta caster --> entity
		int cdx = (int) Math.signum(entity.x - caster.x);
		int cdy = (int) Math.signum(entity.y - caster.y);
		// Delta entity --> target
		int dx = (int) Math.signum(target.x - entity.x);
		int dy = (int) Math.signum(target.y - entity.y);
		// Check deltas (must be pushed in the correct direction)
		if (cdx != dx || cdy != dy) return entity; // no change
		Cell current = entity;
		while (current != target) {
			Cell next = current.next(dx, dy);
			if (!next.available()) {
				return current;
			}
			current = next;
		}
		return current;
	}
}
