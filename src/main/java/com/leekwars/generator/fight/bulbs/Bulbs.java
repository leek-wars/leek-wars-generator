package com.leekwars.generator.fight.bulbs;

import java.util.TreeMap;

public class Bulbs {

	private static TreeMap<Integer, BulbTemplate> sTemplates = new TreeMap<Integer, BulbTemplate>();

	public static BulbTemplate getInvocationTemplate(int id) {
		return sTemplates.get(id);
	}

	public static void addInvocationTemplate(BulbTemplate invocation) {
		sTemplates.put(invocation.getId(), invocation);
	}
}
