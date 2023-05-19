package com.leekwars.generator.maps;

import java.util.List;

public class Pathfinding {

	public final static byte NORTH = 0;// NE
	public final static byte EAST = 1;// SE
	public final static byte SOUTH = 2;// SO
	public final static byte WEST = 3;// NO

	private final static boolean DEBUG = false;

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

/*
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
	*/


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

}
