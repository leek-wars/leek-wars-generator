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
		int cellsMin = 0;
		if (min > 0) {
			if (min > 1)
				cellsMin = 1 + (min - 1) * (4 + 4 * (min - 1)) / 2;
			else
				cellsMin = 1;
		}
		int cellsMax = 1;
		if (max > 0)
			cellsMax = max * (4 + 4 * max) / 2 + 1;

		int nbCells = cellsMax - cellsMin;
		int[][] retour = new int[nbCells][2];

		int index = 0;
		if (min == 0) {
			retour[0] = new int[] { 0, 0 };
			index++;
		}

		for (int size = (min < 1 ? 1 : min); size <= max; size++) {
			for (int i = 0; i < size; i++) {
				retour[index] = new int[] { i, size - i };
				retour[index + 1] = new int[] { -i, -(size - i) };
				retour[index + 2] = new int[] { size - i, -i };
				retour[index + 3] = new int[] { -(size - i), i };
				index += 4;
			}
		}
		return retour;
	}

	public static int[][] generatePlusMask(int radius) {

		int nbCells = 1 + radius * 4;
		int[][] retour = new int[nbCells][2];

		retour[0] = new int[] { 0, 0 };

		int index = 1;
		for (int size = 1; size <= radius; size++) {
			retour[index] = new int[] { size, 0 };
			retour[index + 1] = new int[] { 0, -size };
			retour[index + 2] = new int[] { -size, 0 };
			retour[index + 3] = new int[] { 0, size };
			index += 4;
		}
		return retour;
	}

	public static int[][] generateXMask(int radius) {

		int nbCells = 1 + radius * 4;
		int[][] retour = new int[nbCells][2];

		retour[0] = new int[] { 0, 0 };

		int index = 1;
		for (int size = 1; size <= radius; size++) {
			retour[index] = new int[] { size, size };
			retour[index + 1] = new int[] { size, -size };
			retour[index + 2] = new int[] { -size, size };
			retour[index + 3] = new int[] { -size, -size };
			index += 4;
		}
		return retour;
	}

	public static int[][] generateSquareMask(int radius) {

		int nbCells = (1 + 2 * radius) * (1 + 2 * radius);
		int[][] retour = new int[nbCells][2];

		int index = 0;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				retour[index++] = new int[] { x, y };
			}
		}
		return retour;
	}
}
