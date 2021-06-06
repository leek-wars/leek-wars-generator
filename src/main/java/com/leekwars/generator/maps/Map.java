package com.leekwars.generator.maps;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.Team;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;

public class Map {

	private final List<Cell> cells;
	private final int height;
	private final int width;
	private final int nb_cells;
	private int type;
	private final Cell[][] coord;
	private Cell[] mObstacles = null;
	private int min_x = -1;
	private int max_x = -1;
	private int min_y = -1;
	private int max_y = -1;

	public static Map generateMap(Fight fight, int context, int width, int height, int obstacles_count, List<Team> teams, JSONObject custom_map) {

		boolean valid = false;
		int nb = 0;
		Map map = null;

		if (custom_map != null) {

			map = new Map(width, height);

			JSONObject obstacles = custom_map.getJSONObject("obstacles");
			JSONArray team1 = custom_map.getJSONArray("team1");
			JSONArray team2 = custom_map.getJSONArray("team2");

			// Set entities positions
			for (int t = 0; t < teams.size(); ++t) {
				int pos = 0;
				for (Entity l : teams.get(t).getEntities()) {
					// Random cell
					Cell c;
					if (teams.size() == 2) { // 2 teams : 2 sides
						c = map.getRandomCell(fight, t == 0 ? 1 : 4);
					} else { // 2+ teams : random
						c = map.getRandomCell(fight);
					}
					// User custom cell?
					if (t < 2) {
						JSONArray team = t == 0 ? team1 : team2;
						if (team != null) {
							if (pos < team.size()) {
								int cell_id = team.getIntValue(pos++);
								if (cell_id >= 0 || cell_id < map.nb_cells) {
									c = map.getCell(cell_id);
								}
							}
						}
					}
					c.setPlayer(l);
				}
			}

			// Obstacles
			for (String c : obstacles.keySet()) {
				try {
					int cell_id = Integer.parseInt(c);
					Cell cell = map.getCell(cell_id);
					if (cell.available()) {
						cell.setObstacle(0, 1);
					}
				} catch (Exception e) {}
			}
			map.computeComposantes();

		} else {

			while (!valid && nb < 63) {

				map = new Map(width, height);

				for (int i = 0; i < obstacles_count; i++) {
					Cell c = map.getCell(fight.getRandom().getInt(0, map.getNbCell()));
					if (c != null && c.available()) {
						int size = fight.getRandom().getInt(1, 2);
						int type = fight.getRandom().getInt(0, 2);
						if (size == 2) {
							Cell c2 = Pathfinding.getCellByDir(c, Pathfinding.EAST);
							Cell c3 = Pathfinding.getCellByDir(c, Pathfinding.SOUTH);
							Cell c4 = Pathfinding.getCellByDir(c3, Pathfinding.EAST);
							if (c2 == null || c3 == null || c4 == null || !c2.available() || !c3.available() || !c4.available())
								size = 1;
							else {
								c2.setObstacle(0, -1);
								c3.setObstacle(0, -2);
								c4.setObstacle(0, -3);
							}
						}
						c.setObstacle(type, size);
					}
				}
				map.computeComposantes();
				ArrayList<Entity> leeks = new ArrayList<Entity>();

				// Set entities positions
				for (int t = 0; t < teams.size(); ++t) {

					for (Entity l : teams.get(t).getEntities()) {

						Cell c;
						if (teams.size() == 2) { // 2 teams : 2 sides

							c = map.getRandomCell(fight, t == 0 ? 1 : 4);

						} else { // 2+ teams : random

							c = map.getRandomCell(fight);
						}

						c.setPlayer(l);
						leeks.add(l);

						// If turret, remove obstacles 5 cells around
						if (l.getType() == Entity.TYPE_TURRET) {
							for (Cell cell : map.getCellsInCircle(c, 5)) {
								map.removeObstacle(cell);
							}
						}
					}
				}

				// Check paths
				valid = true;
				if (leeks.size() > 0) {
					int composante = leeks.get(0).getCell().getComposante();
					for (int i = 1; i < leeks.size(); i++) {
						if (composante != leeks.get(i).getCell().getComposante()) {
							valid = false;
							break;
						}
					}
				}
				nb++;
			}
		}

		// Generate type
		map.setType(fight.getRandom().getInt(0, 4));

		if (context == Fight.CONTEXT_TEST) {
			map.setType(-1); // Nexus
		} else if (context == Fight.CONTEXT_TOURNAMENT) {
			map.setType(5); // Arena
		}
		// map.drawMap();
		return map;
	}

	public Map(int width, int height) {

		this.width = width;
		this.height = height;

		nb_cells = (width * 2 - 1) * height - (width - 1);

		cells = new ArrayList<Cell>();
		for (int i = 0; i < nb_cells; i++) {
			Cell c = new Cell(this, i);
			cells.add(c);
			if (min_x == -1 || c.getX() < min_x)
				min_x = c.getX();
			if (max_x == -1 || c.getX() > max_x)
				max_x = c.getX();
			if (min_y == -1 || c.getY() < min_y)
				min_y = c.getY();
			if (max_y == -1 || c.getY() > max_y)
				max_y = c.getY();

		}
		int sx = max_x - min_x + 1;
		int sy = max_y - min_y + 1;
		coord = new Cell[sx][sy];
		for (int i = 0; i < nb_cells; i++) {
			Cell c = cells.get(i);
			coord[c.getX() - min_x][c.getY() - min_y] = c;
		}
	}

	public int getNbCell() {
		return nb_cells;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Cell getCell(int id) {
		if (id < 0 || id >= cells.size())
			return null;
		return cells.get(id);
	}

	public List<Cell> getCells() {
		return cells;
	}

	public Cell getCell(int x, int y) {
		try {
			return coord[x - min_x][y - min_y];
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	public Cell[] getObstacles() {
		if (mObstacles == null) {
			ArrayList<Cell> obstacles = new ArrayList<Cell>();
			for (Cell c : cells) {
				if (!c.isWalkable())
					obstacles.add(c);
			}
			mObstacles = new Cell[obstacles.size()];
			for (int i = 0; i < obstacles.size(); i++) {
				mObstacles[i] = obstacles.get(i);
			}
		}
		return mObstacles;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void clear() {
		for (Cell c : cells) {
			c.setObstacle(0, 0);
			c.setWalkable(true);
		}
	}

	public Cell getRandomCell(Fight fight) {
		Cell retour = null;
		while (retour == null || !retour.available()) {
			retour = getCell(fight.getRandom().getInt(0, nb_cells));
		}
		return retour;
	}

	public Cell getRandomCell(Fight fight, int part) {
		Cell retour = null;
		while (retour == null || !retour.available()) {
			int y = fight.getRandom().getInt(0, height - 1);
			int x = fight.getRandom().getInt(0, width / 4);
			int cellid = y * (width * 2 - 1);
			cellid += (part - 1) * width / 4 + x;
			retour = getCell(cellid);
		}
		return retour;
	}

	public void computeComposantes() {
		var connexe = new int[this.coord.length][this.coord[0].length];
		int x, y, x2, y2, ni = 1;
		for (x = 0; x < connexe.length; x++) {
			for (y = 0; y < connexe[x].length; y++)
				connexe[x][y] = -1;
		}

		// On cherche les composantes connexes
		for (x = 0; x < connexe.length; x++) {
			for (y = 0; y < connexe[x].length; y++) {
				Cell c = this.coord[x][y];
				if (c == null) {
					continue;
				}
				int cur_number = 0;
				if (x > 0 && this.coord[x - 1][y] != null && this.coord[x - 1][y].isWalkable() == c.isWalkable())
					cur_number = connexe[x - 1][y];

				if (y > 0 && this.coord[x][y - 1] != null && this.coord[x][y - 1].isWalkable() == c.isWalkable()) {
					if (cur_number == 0)
						cur_number = connexe[x][y - 1];
					else if (cur_number != connexe[x][y - 1]) {
						int target_number = connexe[x][y - 1];
						for (x2 = 0; x2 < connexe.length; x2++) {
							for (y2 = 0; y2 <= y; y2++) {
								if (connexe[x2][y2] == target_number)
									connexe[x2][y2] = cur_number;
							}
						}
					}
				}

				// On regarde si y'a un numéro de composante
				if (cur_number == 0) {
					// Si y'en a pas on lui en donen un
					connexe[x][y] = ni;
					ni++;
				} else {
					// Si y'en a un on le met
					connexe[x][y] = cur_number;
				}
			}
		}
		for (var cell : this.cells) {
			cell.composante = connexe[cell.getX() - this.min_x][cell.getY() - this.min_y];
		}
	}

	public void drawMap() {
		drawMap(new ArrayList<Cell>());
	}

	public void drawMap(List<Cell> path) {

		BufferedImage img = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) img.getGraphics();

		int larg = 1600 / (max_x - min_x + 1);
		int lng = 1200 / (max_y - min_y + 1);

		for (int x = 0; x <= (max_x - min_x); x++) {
			g2d.drawLine(x * larg, 0, x * larg, 1200);
		}
		for (int y = 0; y <= (max_y - min_y); y++) {
			g2d.drawLine(0, y * lng, 1600, y * lng);
		}
		for (int x = 0; x <= (max_x - min_x); x++) {
			for (int y = 0; y <= (max_y - min_y); y++) {
				Cell c = getCell(x + min_x, y + min_y);
				if (c != null) {
					if (path.contains(c)) {
						if (c.available())
							g2d.setColor(Color.YELLOW);
						else
							g2d.setColor(Color.ORANGE);
					} else if (!c.available())
						g2d.setColor(Color.BLUE);
					else
						g2d.setColor(Color.GRAY);
					g2d.fillRect(x * larg + 1, y * lng + 1, larg - 1, lng - 1);

					g2d.setColor(Color.WHITE);
					// g2d.drawString((c.getX() - 17) + "," + c.getY(), x * larg + 2, y * lng + 15);
					g2d.drawString(c.getId() + " ", x * larg + 2, y * lng + 15);

					g2d.drawString(c.getComposante() + "", x * larg + 2, y * lng + 30);
				}
			}
		}

		try {
			ImageIO.write(img, "png", new File("Img.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void drawPath(List<Cell> path, Cell start, Cell end) {
		String GREEN = "\033[0;32m";
		String END_COLOR = "\033[0m";

		System.out.print("Draw path: [");
		for (Cell c : path) System.out.print(c.getId() + " -> ");
		System.out.println("] length " + path.size());
		int sx = (width - 1) * 2 + 1;
		int sy = (width - 1) * 2 + 1;

		for (int x = 0; x < sx; ++x) {
			for (int y = 0; y < sy; ++y) {
				Cell c = coord[y][x];
				boolean inPath = path.contains(c);
				if (c == null) {
					System.out.print("  ");
				} else if (c == start) {
					System.out.print("S ");
				} else if (c == end) {
					System.out.print("E ");
				} else if (inPath) {
					System.out.print(GREEN + "▓▓" + END_COLOR);
				} else if (c.isWalkable()) {
					System.out.print("░░");
				} else {
					System.out.print("▓▓");
				}
			}
			System.out.println("");
		}
	}

	/**
	 * Les positions sur le terrain ont changé, on clear le cache de path
	 */
	public void positionChanged() {
//		mPathCache.clear();
	}

	public List<Cell> getPathAway(EntityAI ai, Cell c, List<Cell> cells, int pm) {
		if (c == null)
			return null;
		/*
		String key = c.getId() + "_" + pm + "_";
		for (Cell c1 : cells) {
			if (c1 == null)
				continue;
			key += "_" + c1.getId();
		}
		if (mPathCache.containsKey(key)) {
			return mPathCache.get(key);
		}
		*/
		List<Cell> r = Pathfinding.getPathAway(ai, c, cells, pm);
//		mPathCache.put(key, r);
		return r;
	}

	public List<Cell> getPathBeetween(EntityAI ai, Cell start, Cell end, List<Cell> cells_to_ignore) {
		if (start == null || end == null)
			return null;
		/*
		String key = start.getId() + "__" + end.getId();
		if (cells_to_ignore != null) {
			for (Cell c1 : cells_to_ignore) {
				if (c1 == null)
					continue;
				key += "_" + c1.getId();
			}
		}
		if (mPathCache.containsKey(key)) {
			return mPathCache.get(key);
		}
		*/
		List<Cell> r = Pathfinding.getAStarPath(ai, start, new Cell[] { end }, cells_to_ignore);
//		mPathCache.put(key, r);
		return r;
	}

	private List<Cell> getCellsInCircle(Cell cell, int radius) {
		List<Cell> cells = new ArrayList<>();
		for (int x = cell.x - radius; x <= cell.x + radius; ++x) {
			for (int y = cell.y - radius; y <= cell.y + radius; ++y) {
				Cell c = getCell(x, y);
				if (c != null) cells.add(c);
			}
		}
		return cells;
	}

	private void removeObstacle(Cell cell) {
		if (cell.getObstacleSize() > 0) {
			if (cell.getObstacleSize() == 2) {
				Cell c2 = Pathfinding.getCellByDir(cell, Pathfinding.EAST);
				Cell c3 = Pathfinding.getCellByDir(cell, Pathfinding.SOUTH);
				Cell c4 = Pathfinding.getCellByDir(c3, Pathfinding.EAST);
				c2.setObstacle(0, 0);
				c2.setWalkable(true);
				c3.setObstacle(0, 0);
				c3.setWalkable(true);
				c4.setObstacle(0, 0);
				c4.setWalkable(true);
			}
			cell.setObstacle(0, 0);
			cell.setWalkable(true);
		}
	}
}
