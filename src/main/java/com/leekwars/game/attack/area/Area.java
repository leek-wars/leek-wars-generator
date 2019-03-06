package com.leekwars.game.attack.area;

import java.util.List;

import com.leekwars.game.attack.Attack;
import com.leekwars.game.maps.Cell;

public abstract class Area {

	// Area types
	public final static int TYPE_SINGLE_CELL = 1;
	public final static int TYPE_LASER_LINE = 2;
	public final static int TYPE_CIRCLE1 = 3;
	public final static int TYPE_CIRCLE2 = 4;
	public final static int TYPE_CIRCLE3 = 5;

	protected int mId;
	protected Attack mAttack;

	public Area(Attack attack) {
		mAttack = attack;
	}

	public abstract List<Cell> getArea(Cell launchCell, Cell targetCell);

	protected boolean isAvailable(Cell c, List<Cell> cells_to_ignore) {
		if (c.isWalkable())
			return true;
		if (cells_to_ignore == null)
			return false;
		return cells_to_ignore.contains(c);
	}

	public static Area getArea(Attack attack, byte type) {
		if (type == Area.TYPE_SINGLE_CELL)
			return new AreaSingleCell(attack);
		else if (type == Area.TYPE_LASER_LINE)
			return new AreaLaserLine(attack);
		else if (type == Area.TYPE_CIRCLE1)
			return new AreaCircle1(attack);
		else if (type == Area.TYPE_CIRCLE2)
			return new AreaCircle2(attack);
		else if (type == Area.TYPE_CIRCLE3)
			return new AreaCircle3(attack);
		return null;
	}

	public abstract int getRadius();
}
