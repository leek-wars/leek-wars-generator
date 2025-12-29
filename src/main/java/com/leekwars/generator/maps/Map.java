package com.leekwars.generator.maps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.Comparator;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.area.Area;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;
import com.leekwars.generator.state.Team;

import leekscript.ErrorManager;

public class Map {

	public final static byte NORTH = 0;// NE
	public final static byte EAST = 1;// SE
	public final static byte SOUTH = 2;// SO
	public final static byte WEST = 3;// NO

	private final static boolean DEBUG = false;

	private final int id;
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
	private ObjectNode custom_map;
	private HashMap<Entity, Cell> cellByEntity = new HashMap<>();
	private HashMap<Cell, Entity> entityByCell = new HashMap<>();
	private ArrayNode pattern;
	private State state;

	public static Map generateMap(State state, int context, int width, int height, int obstacles_count, List<Team> teams, ObjectNode custom_map) {

		boolean valid = false;
		int nb = 0;
		Map map = null;

		if (custom_map != null) {

			int mapId = custom_map.hasNonNull("id") ? custom_map.get("id").intValue() : 0;
			map = new Map(width, height, mapId);
			map.custom_map = custom_map;
			map.pattern = (ArrayNode) custom_map.get("pattern");
			map.state = state;

			ObjectNode obstacles = (ObjectNode) custom_map.get("obstacles");
			ArrayNode team1 = (ArrayNode) custom_map.get("team1");
			ArrayNode team2 = (ArrayNode) custom_map.get("team2");

			// Obstacles
			for (var c : obstacles.properties()) {
				try {
					int cell_id = Integer.parseInt(c.getKey());
					Cell cell = map.getCell(cell_id);
					if (cell.available(map)) {
						if (c.getValue().isBoolean()) {
							cell.setObstacle(1, 1);
						} else {
							int id = c.getValue().intValue();
							ObstacleInfo info = ObstacleInfo.get(id);
							if (info.size == 1) {
								cell.setObstacle(id, info.size);
							} else if (info.size == 2) {
								cell.setObstacle(id, info.size);
								Cell c2 = map.getCellByDir(cell, Pathfinding.EAST);
								Cell c3 = map.getCellByDir(cell, Pathfinding.SOUTH);
								Cell c4 = map.getCellByDir(c3, Pathfinding.EAST);
								c2.setObstacle(0, -1);
								c3.setObstacle(0, -2);
								c4.setObstacle(0, -3);
							} else if (info.size == 3) {
								cell.setObstacle(id, info.size);
								for (int x = -1; x <= 1; ++x) {
									for (int y = -1; y <= 1; ++y) {
										if (x != 0 || y != 0)
											map.getNextCell(cell, x, y).setObstacle(0, -1);
									}
								}
							} else if (info.size == 4) {
								cell.setObstacle(id, info.size);
								map.getNextCell(cell, -3, 0).setObstacle(0, -1);
							} else if (info.size == 5) {
								cell.setObstacle(id, info.size);
								// [[0, -1], [0, 0], [0, 3], [2, -1], [2, 0], [2, 3]]
								map.getNextCell(cell, 0, -1).setObstacle(0, -1);
								map.getNextCell(cell, 0, 3).setObstacle(0, -1);
								map.getNextCell(cell, 2, -1).setObstacle(0, -1);
								map.getNextCell(cell, 2, 0).setObstacle(0, -1);
								map.getNextCell(cell, 2, 3).setObstacle(0, -1);
							}
						}
					}
				} catch (Exception e) {
					ErrorManager.exception(e);
				}
			}

			// Set entities positions
			for (int t = 0; t < teams.size(); ++t) {
				int pos = 0;
				for (Entity l : teams.get(t).getEntities()) {
					if (l.isDead()) continue;
					// Random cell
					Cell c;
					if (map.id != 0 && l.getInitialCell() != null) {
						c = map.getCell(l.getInitialCell());
					} else {
						if (teams.size() == 2) { // 2 teams : 2 sides
							c = map.getRandomCell(state, t == 0 ? 1 : 4);
						} else { // 2+ teams : random
							c = map.getRandomCell(state);
						}
						// User custom cell?
						if (t < 2) {
							ArrayNode team = t == 0 ? team1 : team2;
							if (team != null) {
								if (pos < team.size()) {
									int cell_id = team.get(pos++).intValue();
									if (cell_id >= 0 || cell_id < map.nb_cells) {
										c = map.getCell(cell_id);
									}
								}
							}
						}
					}
					if (c != null) {
						map.setEntity(l, c);
					}
				}
			}

			map.computeComposantes();

		} else {

			while (!valid && nb++ < 63) {

				map = new Map(width, height);
				map.state = state;

				for (int i = 0; i < obstacles_count; i++) {
					Cell c = map.getCell(state.getRandom().getInt(0, map.getNbCell()));
					if (c != null && c.available(map)) {
						int size = state.getRandom().getInt(1, 2);
						int type = state.getRandom().getInt(0, 2);
						if (size == 2) {
							Cell c2 = map.getCellByDir(c, Pathfinding.EAST);
							Cell c3 = map.getCellByDir(c, Pathfinding.SOUTH);
							Cell c4 = map.getCellByDir(c3, Pathfinding.EAST);
							if (c2 == null || c3 == null || c4 == null || !c2.available(map) || !c3.available(map) || !c4.available(map))
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
						if (state.getType() == State.TYPE_BATTLE_ROYALE) { // BR : random

							c = map.getRandomCell(state);

						} else { // 2 sides

							if (l.getType() == Entity.TYPE_CHEST) {
								c = map.getCellEqualDistance(state);
							} else {
								c = map.getRandomCell(state, t == 0 ? 1 : 4);
							}
						}
						if (c == null) continue;

						map.setEntity(l, c);
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
					int composante = map.getEntityCell(leeks.get(0)).getComposante();
					for (int i = 1; i < leeks.size(); i++) {
						if (composante != map.getEntityCell(leeks.get(i)).getComposante()) {
							valid = false;
							break;
						}
					}
				}
			}
		}

		// Generate type
		map.setType(state.getRandom().getInt(0, 4));

		if (context == State.CONTEXT_TEST) {
			map.setType(-1); // Nexus
		} else if (context == State.CONTEXT_TOURNAMENT) {
			map.setType(5); // Arena
		} else if (custom_map != null && custom_map.has("type")) {
			map.setType(custom_map.get("type").intValue());
		}
		// map.drawMap();
		return map;
	}

	public Map(int width, int height) {
		this(width, height, 0);
	}

	public Map(int width, int height, int id) {

		this.width = width;
		this.height = height;
		this.id = id;

		nb_cells = (width * 2 - 1) * height - (width - 1);

		cells = new ArrayList<Cell>(nb_cells);
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

	public Map(Map map, State state) {
		this.id = map.id;
		this.width = map.width;
		this.height = map.height;
		this.nb_cells = map.nb_cells;
		this.cells = map.cells;
		this.coord = map.coord;
		this.min_x = map.min_x;
		this.max_x = map.max_x;
		this.min_y = map.min_y;
		this.max_y = map.max_y;
		for (var entry : map.entityByCell.entrySet()) {
			this.entityByCell.put(entry.getKey(), state.getEntity(entry.getValue().getFId()));
		}
		for (var entry : map.cellByEntity.entrySet()) {
			this.cellByEntity.put(state.getEntity(entry.getKey().getFId()), entry.getValue());
		}
	}

	public void setEntity(Entity entity, Cell cell) {
		this.entityByCell.put(cell, entity);
		this.cellByEntity.put(entity, cell);
		entity.setCell(cell);
	}

	public void moveEntity(Entity entity, Cell cell) {
		var oldCell = this.cellByEntity.remove(entity);
		this.entityByCell.remove(oldCell);
		this.entityByCell.put(cell, entity);
		this.cellByEntity.put(entity, cell);
		entity.setCell(cell);
	}

	public void removeEntity(Entity entity) {
		var cell = this.cellByEntity.remove(entity);
		this.entityByCell.remove(cell);
		entity.setCell(null);
	}

	public void invertEntities(Entity entity1, Entity entity2) {
		var cell1 = this.cellByEntity.get(entity1);
		var cell2 = this.cellByEntity.get(entity2);
		this.cellByEntity.put(entity1, cell2);
		this.cellByEntity.put(entity2, cell1);
		this.entityByCell.put(cell1, entity2);
		this.entityByCell.put(cell2, entity1);
		entity1.setCell(cell2);
		entity2.setCell(cell1);
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

	public Cell getNextCell(Cell cell, int dx, int dy) {
		var x = cell.x + dx;
		var y = cell.y + dy;
		if (x < this.min_x || y < this.min_y || x > this.max_x || y > this.max_y) {
			return null;
		}
		return this.coord[x - this.min_x][y - this.min_y];
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

	public Cell getRandomCell(State state) {
		Cell retour = null;
		int nb = 0;
		while (retour == null || !retour.available(this)) {
			retour = getCell(state.getRandom().getInt(0, nb_cells));
			if (nb++ > 64) break;
		}
		return retour;
	}

	public Cell getRandomCell(State state, int part) {
		Cell retour = null;
		int nb = 0;
		while (retour == null || !retour.available(this)) {
			int y = state.getRandom().getInt(0, height - 1);
			int x = state.getRandom().getInt(0, width / 4);
			int cellid = y * (width * 2 - 1);
			cellid += (part - 1) * width / 4 + x;
			retour = getCell(cellid);
			if (nb++ > 64) break;
		}
		return retour;
	}

	public Cell getCellEqualDistance(State state) {
		// Cellule à distance éguale des deux équipes
		var possible = new ArrayList<Cell>();
		for (var cell : cells) {
			if (cell.available(this) && Math.abs(getDistanceWithTeam(state, 0, cell) - getDistanceWithTeam(state, 1, cell)) < 2) {
				possible.add(cell);
			}
		}
		if (possible.size() > 0) {
			int i = state.getRandom().getInt(0, possible.size() - 1);
			return possible.get(i);
		}
		return getRandomCell(state);
	}

	public List<Cell> getCellsEqualDistance(Cell cell1, Cell cell2) {
		var result = new ArrayList<Cell>();
		for (var cell : cells) {
			if (cell.isWalkable() && Math.abs(Pathfinding.getCaseDistance(cell, cell1) - Pathfinding.getCaseDistance(cell, cell2)) < 2) {
				result.add(cell);
			}
		}
		return result;
	}

	public int getDistanceWithTeam(State state, int team, Cell cell) {
		int min = Integer.MAX_VALUE;
		for (var entity : state.getTeamEntities(team)) {
			int d = Pathfinding.getCaseDistance(entity.getCell(), cell);
			if (d < min) {
				min = d;
			}
		}
		return min;
	}

	public Cell getTeamBarycenter(State state, int team) {
		int tx = 0;
		int ty = 0;
		var entities = state.getTeamEntities(team);
		for (var entity : entities) {
			tx += entity.getCell().x;
			ty += entity.getCell().y;
		}
		return getCell(tx / entities.size(), ty / entities.size());
	}

	public Cell getRandomCellAtDistance(Cell cell1, int distance) {
		var result = new ArrayList<Cell>();
		for (var cell : cells) {
			if (cell.isWalkable() && Pathfinding.getCaseDistance(cell, cell1) == distance) {
				result.add(cell);
			}
		}
		if (result.size() == 0) return null;
		return result.get((int) (result.size() * Math.random()));
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

	public void drawMap(List<Cell> area) {

		int WIDTH = 1300;
		int HEIGHT = 1300;

		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) img.getGraphics();
		var f = new Font("Roboto", Font.BOLD, 12);
		g2d.setFont(f);
		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, WIDTH, HEIGHT);

		int larg = WIDTH / (max_x - min_x + 1);
		int lng = HEIGHT / (max_y - min_y + 1);

		for (int x = 0; x <= (max_x - min_x); x++) {
			g2d.drawLine(x * larg, 0, x * larg, WIDTH);
		}
		for (int y = 0; y <= (max_y - min_y); y++) {
			g2d.drawLine(0, y * lng, HEIGHT, y * lng);
		}
		for (int x = 0; x <= (max_x - min_x); x++) {
			for (int y = 0; y <= (max_y - min_y); y++) {
				Cell c = getCell(x + min_x, y + min_y);
				if (c != null) {
					var textColor = Color.BLACK;
					if (c.getPlayer(this) != null) {
						textColor = Color.WHITE;
						if (c.getPlayer(this).getTeam() == 0) {
							g2d.setColor(Color.BLUE);
						} else {
							g2d.setColor(Color.RED);
						}
					} else if (area.contains(c)) {
						if (c.available(this)) {
							g2d.setColor(Color.GREEN);
						} else {
							g2d.setColor(Color.ORANGE);
						}
					} else if (!c.available(this)) {
						g2d.setColor(Color.GRAY);
					} else {
						g2d.setColor(Color.LIGHT_GRAY);
					}
					g2d.fillRect(x * larg + 1, y * lng + 1, larg - 1, lng - 1);

					g2d.setColor(textColor);
					// g2d.drawString((c.getX() - 17) + "," + c.getY(), x * larg + 2, y * lng + 15);
					g2d.drawString(c.getId() + " ", x * larg + 6, y * lng + 24);
					// g2d.drawString(c.getComposante() + "", x * larg + 2, y * lng + 30);
				}
			}
		}

		try {
			ImageIO.write(img, "png", new File("Img.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		JFrame frame = new JFrame("Leek Wars map");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				System.exit(0);
			}
			@Override
			public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyTyped(KeyEvent arg0) {}
		});
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

	public List<Cell> getPathBeetween(Cell start, Cell end, List<Cell> cells_to_ignore) {
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
		List<Cell> r = getAStarPath(start, new Cell[] { end }, cells_to_ignore);
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
				Cell c2 = getCellByDir(cell, Pathfinding.EAST);
				Cell c3 = getCellByDir(cell, Pathfinding.SOUTH);
				Cell c4 = getCellByDir(c3, Pathfinding.EAST);
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

	public boolean isCustom() {
		return custom_map != null;
	}

	public Entity getEntity(Cell cell) {
		return this.entityByCell.get(cell);
	}

	public Cell getEntityCell(Entity entity) {
		return this.cellByEntity.get(entity);
	}

	public Cell getCellByDir(Cell c, byte dir) {

		if (c == null)
			return null;

		if (dir == NORTH && c.hasNorth())
			return getCell(c.getId() - width + 1);
		else if (dir == WEST && c.hasWest())
			return getCell(c.getId() - width);
		else if (dir == EAST && c.hasEast())
			return getCell(c.getId() + width);
		else if (dir == SOUTH && c.hasSouth())
			return getCell(c.getId() + width - 1);
		return null;
	}

	public boolean verifyLoS(Cell start, Cell end, Attack attack) {

		List<Cell> ignoredCells = new ArrayList<Cell>();
		ignoredCells.add(start);

		// Ignore first entity in area for Area first in line
		if (attack.getArea() == Area.TYPE_FIRST_IN_LINE) {
			Cell cell = getFirstEntity(start, end, attack.getMinRange(), attack.getMaxRange());
			if (cell == end) return false;
			if (cell != null) {
				ignoredCells.add(cell);
			}
		}
		return verifyLoS(start, end, attack, ignoredCells);
	}

	public boolean verifyLoS(Cell start, Cell end, Attack attack, List<Cell> ignoredCells) {

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

				Cell cell = getCell(start.getX() + (p / 2) * dx, start.getY() + (path.get(p) + i) * dy);

				if (cell == null)
					return false;

				if (needLos) {
					if (!cell.isWalkable()) {
						return false;
					}
					if (!cell.available(this)) {
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


	public Cell[] getCellsAround(Cell c) {
		return new Cell[] { getCellByDir(c, SOUTH), getCellByDir(c, WEST), getCellByDir(c, NORTH), getCellByDir(c, EAST) };
	}

	public List<Cell> getPathTowardLine(Cell start, Cell linecell1, Cell linecell2) {
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
			curent = getCell(curent.getX() + dx, curent.getY() + dy);
		}
		curent = getCell(linecell1.getX() - dx, linecell1.getY() - dy);
		while (curent != null) {
			line_cell.add(curent);
			curent = getCell(curent.getX() - dx, curent.getY() - dy);
		}
		// Puis on crée un path qui va vers de ces cellules
		return getAStarPath(start, line_cell);
	}

	public List<Cell> getPathAwayFromLine(Cell start, Cell linecell1, Cell linecell2, int max_distance) {
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
			current = getCell(current.getX() + dx, current.getY() + dy);
		}
		current = getCell(linecell1.getX() - dx, linecell1.getY() - dy);
		while (current != null) {
			line_cell.add(current);
			current = getCell(current.getX() - dx, current.getY() - dy);
		}
		// Puis on crée un path qui va loin de ces cellules

		List<Cell> cells = getPathAway(start, line_cell, max_distance);

		return cells;
	}

	public List<Cell> getPathAway(Cell start, List<Cell> bad_cells, int max_distance) {
		if (start == null)
			return null;
		int curent_distance = getDistance2(start, bad_cells);
		List<CellDistance> potential_targets = new ArrayList<CellDistance>();
		int[][] cells = MaskAreaCell.generateCircleMask(1, max_distance);
		if (cells == null)
			return null;
		int x = start.getX(), y = start.getY();
		for (int i = 0; i < cells.length; i++) {
			Cell c = getCell(x + cells[i][0], y + cells[i][1]);
			if (c == null || !c.available(this))
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
			path = getAStarPath(start, new Cell[] { c.getCell() });
			if (path != null && path.size() <= max_distance)
				break;
			else
				path = null;
		}
		// if (DEBUG)
		// 	System.out.println("Time : " + (System.currentTimeMillis() - startt));
		return path;
	}

	public int getDistance2(Cell c1, List<Cell> cells) {
		int dist = -1;
		for (Cell c2 : cells) {
			int d = (c1.getX() - c2.getX()) * (c1.getX() - c2.getX()) + (c1.getY() - c2.getY()) * (c1.getY() - c2.getY());
			if (dist == -1 || d < dist)
				dist = d;
		}
		return dist;
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

	public List<Cell> getPathAwayMin(Map map, Cell start, List<Cell> bad_cells, int max_distance) {
		long startt;
		if (DEBUG)
			startt = System.currentTimeMillis();
		int curent_distance = getDistance2(start, bad_cells);
		List<CellDistance> potential_targets = new ArrayList<CellDistance>();
		int[][] cells = MaskAreaCell.generateCircleMask(1, max_distance);
		int x = start.getX(), y = start.getY();
		for (int i = 0; i < cells.length; i++) {
			Cell c = map.getCell(x + cells[i][0], y + cells[i][1]);
			if (c == null || !c.available(map))
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
			path = getAStarPath(start, new Cell[] { c.getCell() });
			if (path != null && path.size() <= max_distance)
				break;
			else
				path = null;
		}
		if (DEBUG)
			System.out.println("Time : " + (System.currentTimeMillis() - startt));
		return path;
	}

	public boolean available(Cell c, List<Cell> cells_to_ignore) {
		if (c == null)
			return false;
		if (c.available(this))
			return true;
		if (cells_to_ignore != null && cells_to_ignore.contains(c))
			return true;
		return false;
	}

	public List<Cell> getAStarPath(Cell c1, Cell[] cell, List<Cell> cells_to_ignore) {
		return getAStarPath(c1, Arrays.asList(cell), cells_to_ignore);
	}

	public List<Cell> getAStarPath(Cell c1, Cell[] cell) {
		return getAStarPath(c1, Arrays.asList(cell), null);
	}

	public List<Cell> getAStarPath(Cell c1, List<Cell> endCells) {
		return getAStarPath(c1, endCells, null);
	}

	public List<Cell> getAStarPath(Cell c1, List<Cell> endCells, List<Cell> cells_to_ignore) {
		if (c1 == null || endCells == null || endCells.isEmpty())
			return null;
		if (endCells.contains(c1))
			return null;

		for (Cell c : getCells()) {
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
				if (last.getPlayer(this) != null && (cells_to_ignore == null || !cells_to_ignore.contains(last))) {
					result.remove(result.size() - 1);
				}
				return result;
			}

			for (Cell c : getCellsAround(u)) {
				if (c == null || c.closed || !c.isWalkable()) continue;
				if (c.getPlayer(this) != null && (cells_to_ignore == null || !cells_to_ignore.contains(c)) && !endCells.contains(c)) continue;

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


	public List<Cell> getPossibleCastCellsForTarget(Attack attack, Cell target, List<Cell> cells_to_ignore) {

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
						c = getCell(x + i * dirs[dir][0], y + i * dirs[dir][1]);
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
					cell = getCell(x + mask_cell[0], y + mask_cell[1]);
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


	public Cell getFirstEntity(Cell from, Cell target, int minRange, int maxRange) {
		int dx = (int) Math.signum(target.x - from.x);
		int dy = (int) Math.signum(target.y - from.y);
		Cell current = from.next(this, dx, dy);
		int range = 1;
		while (current != null && current.isWalkable() && range <= maxRange) {
			if (range >= minRange && getEntity(current) != null) {
				return current;
			}
			current = current.next(this, dx, dy);
			range++;
		}
		return null;
	}

	public Cell getPushLastAvailableCell(Cell entity, Cell target, Cell caster) {
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
			Cell next = current.next(this, dx, dy);
			if (!next.available(this)) {
				return current;
			}
			current = next;
		}
		return current;
	}

	public Cell getAttractLastAvailableCell(Cell entity, Cell target, Cell caster) {
		// Delta caster --> entity
		int cdx = (int) Math.signum(entity.x - caster.x);
		int cdy = (int) Math.signum(entity.y - caster.y);
		// Delta entity --> target
		int dx = (int) Math.signum(target.x - entity.x);
		int dy = (int) Math.signum(target.y - entity.y);
		// Check deltas (must be attracted in the correct direction)
		if (cdx != -dx || cdy != -dy) return entity; // no change
		Cell current = entity;
		while (current != target) {
			Cell next = current.next(this, dx, dy);
			if (!next.available(this)) {
				return current;
			}
			current = next;
		}
		return current;
	}


	public boolean canUseAttack(Cell caster, Cell target, Attack attack) {
		// Portée
		if (!verifyRange(caster, target, attack)) {
			return false;
		}
		return verifyLoS(caster, target, attack);
	}

	public boolean verifyRange(Cell caster, Cell target, Attack attack) {

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

	public List<Cell> getValidCellsAroundObstacle(Cell cell) {
		List<Cell> retour = new ArrayList<Cell>();
		int size = 1;
		List<Cell> close = new ArrayList<Cell>();
		close.add(cell);

		for (int i = 1; i <= size; i++) {
			boolean stop = true;
			for (int j = 0; j < i; j++) {
				stop = addValidCell(retour, close, getCell(cell.getX() + j, cell.getY() + (i - j)), cell) && stop;
				stop = addValidCell(retour, close, getCell(cell.getX() - j, cell.getY() - (i - j)), cell) && stop;
				stop = addValidCell(retour, close, getCell(cell.getX() + i - j, cell.getY() - j), cell) && stop;
				stop = addValidCell(retour, close, getCell(cell.getX() - i + j, cell.getY() + j), cell) && stop;
			}
			if (!stop && size < 5)
				size++;
		}
		return retour;
	}

	private boolean addValidCell(List<Cell> retour, List<Cell> close, Cell c, Cell center) {
		if (c == null) {
			return true;
		}
		int dx = (int) Math.signum(center.getX() - c.getX());
		int dy = (int) Math.signum(center.getY() - c.getY());

		Cell c1 = getCell(c.getX() + dx, c.getY());
		Cell c2 = getCell(c.getX(), c.getY() + dy);

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

	public static double getDistance(Cell c1, Cell c2) {
		return Math.sqrt(getDistance2(c1, c2));
	}

	public static int getDistance2(Cell c1, Cell c2) {
		return (c1.getX() - c2.getX()) * (c1.getX() - c2.getX()) + (c1.getY() - c2.getY()) * (c1.getY() - c2.getY());
	}

	public int getId() {
		return id;
	}

	public ArrayNode getPattern() {
		return pattern;
	}

	public State getState() {
		return state;
	}

	public HashMap<Entity, Cell> getEntities() {
		return cellByEntity;
	}
}
