package com.leekwars.generator.maps;

public class MaskAreaCell {

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
}
