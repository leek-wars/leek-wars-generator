package com.leekwars.generator.fight.summons;

import java.util.TreeMap;

public class Summons {

	private static TreeMap<Integer, SummonTemplate> sTemplates = new TreeMap<Integer, SummonTemplate>();

	public static SummonTemplate getInvocationTemplate(int id) {
		return sTemplates.get(id);
	}

	public static void addInvocationTemplate(SummonTemplate invocation) {
		sTemplates.put(invocation.getId(), invocation);
	}
}
