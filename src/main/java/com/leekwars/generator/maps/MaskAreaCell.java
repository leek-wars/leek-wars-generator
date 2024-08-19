package com.leekwars.generator.maps;

import java.util.ArrayList;

public class MaskAreaCell {

	public static ArrayList<int[]> generateMask(int launchType, int min, int max) {

		if (min > max)
			return new ArrayList<>();

		var cells = new ArrayList<int[]>();
		int len = (launchType == 9 || launchType == 10) ? max : ((launchType & 1) != 0 ? max : ((launchType & 4) != 0 ? max - 1 : max / 2));
		for (int i = 0; i < len * 2 + 1; ++i) {
			for (int j = 0; j < len * 2 + 1; ++j) {
				int x = i - len;
				int y = j - len;
				boolean in_range = Math.abs(x) + Math.abs(y) <= max && Math.abs(x) + Math.abs(y) >= min;
				boolean condition = (((launchType & 1) != 0) && (x == 0 || y == 0))
					|| (((launchType & 2) != 0) && Math.abs(x) == Math.abs(y))
					|| (((launchType & 4) != 0) && ((x == 0 && y == 0) || (Math.abs(x) != Math.abs(y) && x != 0 && y != 0)));
				if (in_range && condition) {
					cells.add(new int[] { x, y });
				}
			}
		}
		return cells;
	}

	public static int[][] generateCircleMask(int min, int max) {

		if (min > max)
			return null;

		int nbCells = 2 * (min + max) * (max - min + 1);
		if (min == 0) {
			nbCells += 1;
		}
		int[][] retour = new int[nbCells][2];

		int index = 0;
		if (min == 0) {
			// Center first
			retour[index++] = new int[] { 0, 0 };
		}

		// Go from cells closer to the center to the farther ones
		for (int size = (min < 1 ? 1 : min); size <= max; size++) {
			// Add cells counter-clockwise
			for (int i = 0; i < size; i++) {
				retour[index++] = new int[] { size - i, -i };
			}
			for (int i = 0; i < size; i++) {
				retour[index++] = new int[] { -i, -(size - i) };
			}
			for (int i = 0; i < size; i++) {
				retour[index++] = new int[] { -(size - i), i };
			}
			for (int i = 0; i < size; i++) {
				retour[index++] = new int[] { i, size - i };
			}
		}
		return retour;
	}

	public static int[][] generatePlusMask(int radius) {

		int nbCells = 1 + radius * 4;
		int[][] retour = new int[nbCells][2];

		// Center first
		retour[0] = new int[] { 0, 0 };

		int index = 1;
		// Go from cells closer to the center to the farther ones
		for (int size = 1; size <= radius; size++) {
			// Add cells counter-clockwise
			retour[index++] = new int[] { size, 0 };
			retour[index++] = new int[] { 0, -size };
			retour[index++] = new int[] { -size, 0 };
			retour[index++] = new int[] { 0, size };
		}
		return retour;
	}

	public static int[][] generateXMask(int radius) {

		int nbCells = 1 + radius * 4;
		int[][] retour = new int[nbCells][2];

		// Center first
		retour[0] = new int[] { 0, 0 };

		int index = 1;
		// Go from cells closer to the center to the farther ones
		for (int size = 1; size <= radius; size++) {
			// Add cells counter-clockwise
			retour[index++] = new int[] { size, -size };
			retour[index++] = new int[] { -size, -size };
			retour[index++] = new int[] { -size, size };
			retour[index++] = new int[] { size, size };
		}
		return retour;
	}

	public static int[][] generateSquareMask(int radius) {

		int nbCells = (1 + 2 * radius) * (1 + 2 * radius);
		int[][] retour = new int[nbCells][2];

		// Go from cells closer to the center to the farther ones
		// First, add cells in the inscribed circle
		int index = 0;
		for (int[] cell : generateCircleMask(0, radius)) {
			retour[index++] = cell;
		}
		// Then, the corners
		for (int d = 0; d < radius; d++) {
			// Add cells counter-clockwise
			for (int i = 1; i <= radius - d; i++) {
				retour[index++] = new int[] { radius + 1 - i, -(d + i) };
			}
			for (int i = 1; i <= radius - d; i++) {
				retour[index++] = new int[] { -(d + i), -(radius + 1 - i) };
			}
			for (int i = 1; i <= radius - d; i++) {
				retour[index++] = new int[] { -(radius + 1 - i), d + i };
			}
			for (int i = 1; i <= radius - d; i++) {
				retour[index++] = new int[] { d + i, radius + 1 - i };
			}
		}
		return retour;
	}
}
