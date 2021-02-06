package com.leekwars.generator.attack.area;

import java.util.List;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.maps.Cell;

public abstract class Area {

	// Area types
	public final static int TYPE_SINGLE_CELL = 1;
	public final static int TYPE_LASER_LINE = 2;
	public final static int TYPE_CIRCLE1 = 3;
	public final static int TYPE_CIRCLE2 = 4;
	public final static int TYPE_CIRCLE3 = 5;
	public final static int TYPE_AREA_PLUS_1 = 3; // Equals to CIRCLE_1
	public final static int TYPE_AREA_PLUS_2 = 6;
	public final static int TYPE_AREA_PLUS_3 = 7;
	public final static int TYPE_X_1 = 8;
	public final static int TYPE_X_2 = 9;
	public final static int TYPE_X_3 = 10;
	public final static int TYPE_SQUARE_1 = 11;
	public final static int TYPE_SQUARE_2 = 12;
	public final static int TYPE_FIRST_IN_LINE = 13;

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
		else if (type == Area.TYPE_CIRCLE1 || type == Area.TYPE_AREA_PLUS_1)
			return new AreaCircle1(attack);
		else if (type == Area.TYPE_CIRCLE2)
			return new AreaCircle2(attack);
		else if (type == Area.TYPE_CIRCLE3)
			return new AreaCircle3(attack);
		else if (type == Area.TYPE_AREA_PLUS_2)
			return new AreaPlus2(attack);
		else if (type == Area.TYPE_AREA_PLUS_3)
			return new AreaPlus3(attack);
		else if (type == Area.TYPE_X_1)
			return new AreaX1(attack);
		else if (type == Area.TYPE_X_2)
			return new AreaX2(attack);
		else if (type == Area.TYPE_X_3)
			return new AreaX3(attack);
		else if (type == Area.TYPE_SQUARE_1)
			return new AreaSquare1(attack);
		else if (type == Area.TYPE_SQUARE_2)
			return new AreaSquare2(attack);
		else if (type == Area.TYPE_FIRST_IN_LINE)
			return new AreaFirstInLine(attack);
		return null;
	}
}
