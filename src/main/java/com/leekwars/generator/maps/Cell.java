package com.leekwars.generator.maps;

import com.leekwars.generator.fight.entity.Entity;

public class Cell {

	private final int id;
	private final Map map;

	private Entity player;

	private boolean walkable;
	private int obstacle;
	private int size;

	private boolean north = true;
	private boolean west = true;
	private boolean east = true;
	private boolean south = true;

	int x, y;
	int composante;
	boolean visited = false;
	boolean closed = false;
	short cost = 0;
	float weight = 0;
	Cell parent = null;

	public Cell(Map map, int id) {

		this.id = id;
		this.map = map;
		this.walkable = true;
		this.obstacle = 0;
		int x = id % (map.getWidth() * 2 - 1);
		int y = id / (map.getWidth() * 2 - 1);
		if (y == 0 && x < map.getWidth()) {
			north = false;
			west = false;
		} else if (y + 1 == map.getHeight() && x >= map.getWidth()) {
			east = false;
			south = false;
		}
		if (x == 0) {
			south = false;
			west = false;
		} else if (x + 1 == map.getWidth()) {
			north = false;
			east = false;
		}

		// On calcule Y
		this.y = y - x % map.getWidth();
		this.x = (id - (map.getWidth() - 1) * this.y) / map.getWidth();
	}

	public boolean hasNorth() {
		return north;
	}

	public boolean hasSouth() {
		return south;
	}

	public boolean hasWest() {
		return west;
	}

	public boolean hasEast() {
		return east;
	}

	public boolean isWalkable() {
		return walkable;
	}

	public int getObstacle() {
		return obstacle;
	}

	public int getObstacleSize() {
		return size;
	}

	public void setWalkable(boolean walkable) {
		this.walkable = walkable;
	}

	public void setObstacle(int id, int size) {
		this.walkable = false;
		this.obstacle = id;
		this.size = size;
	}

	public Map getMap() {
		return map;
	}

	public int getId() {
		return id;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public Entity getPlayer() {
		return player;
	}

	public void setPlayer(Entity entity) {
		if (this.player == entity)
			return;
		// this.map.positionChanged();
		if (entity == null) {
			this.player = null;
			return;
		}
		if (entity.getCell() != null) {
			entity.getCell().setPlayer(null);
		}
		entity.setCell(this);
		player = entity;
	}

	public void setCellPlayer(Entity leek) {
		player = leek;
	}

	public boolean available() {
		return walkable && player == null;
	}

	public boolean available(Cell l) {
		return walkable && (player == null || this == l);
	}

	public int getComposante() {
		return composante;
	}

	public Cell next(int dx, int dy) {
		return map.getCell(this.x + dx, this.y + dy);
	}

	@Override
	public String toString() {
		return "<Cell " + id + ">";
	}
}
