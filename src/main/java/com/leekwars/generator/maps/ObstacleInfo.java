package com.leekwars.generator.maps;

import java.util.HashMap;

public class ObstacleInfo {

	private static HashMap<Integer, ObstacleInfo> obstacles = new HashMap<>();

	public int size;

	static {
		obstacles.put(5, new ObstacleInfo(1));
		obstacles.put(20, new ObstacleInfo(1));
		obstacles.put(21, new ObstacleInfo(1));
		obstacles.put(22, new ObstacleInfo(1));
		obstacles.put(38, new ObstacleInfo(1));
		obstacles.put(40, new ObstacleInfo(1));
		obstacles.put(41, new ObstacleInfo(1));
		obstacles.put(42, new ObstacleInfo(1));
		obstacles.put(48, new ObstacleInfo(1));
		obstacles.put(50, new ObstacleInfo(1));
		obstacles.put(63, new ObstacleInfo(1));
		obstacles.put(66, new ObstacleInfo(1));
		obstacles.put(53, new ObstacleInfo(1));
		obstacles.put(55, new ObstacleInfo(1));
		obstacles.put(57, new ObstacleInfo(1));
		obstacles.put(59, new ObstacleInfo(1));
		obstacles.put(62, new ObstacleInfo(1));

		obstacles.put(11, new ObstacleInfo(2));
		obstacles.put(17, new ObstacleInfo(2));
		obstacles.put(18, new ObstacleInfo(2));
		obstacles.put(34, new ObstacleInfo(2));
		obstacles.put(43, new ObstacleInfo(2));
		obstacles.put(44, new ObstacleInfo(2));
		obstacles.put(45, new ObstacleInfo(2));
		obstacles.put(46, new ObstacleInfo(2));
		obstacles.put(47, new ObstacleInfo(2));
		obstacles.put(49, new ObstacleInfo(2));
		obstacles.put(64, new ObstacleInfo(2));
		obstacles.put(65, new ObstacleInfo(2));
		obstacles.put(52, new ObstacleInfo(2));
		obstacles.put(54, new ObstacleInfo(2));
		obstacles.put(56, new ObstacleInfo(2));
		obstacles.put(58, new ObstacleInfo(2));
		obstacles.put(61, new ObstacleInfo(2));

		obstacles.put(51, new ObstacleInfo(3));

		obstacles.put(39, new ObstacleInfo(4));

		obstacles.put(60, new ObstacleInfo(5));
	}

	public ObstacleInfo(int size) {
		this.size = size;
	}

	public static ObstacleInfo get(int id) {
		return obstacles.get(id);
	}
}
