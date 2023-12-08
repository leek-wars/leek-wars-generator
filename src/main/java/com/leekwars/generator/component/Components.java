package com.leekwars.generator.component;

import java.util.Map;
import java.util.TreeMap;

import com.leekwars.generator.items.Items;

public class Components {

	private static Map<Integer, Component> components = new TreeMap<Integer, Component>();

	public static void addComponent(Component component) {
		components.put(component.getTemplate(), component);
		Items.addComponent(component.getTemplate());
	}

	public static Component getComponent(int id) {
		return components.get(id);
	}

	public static Map<Integer, Component> getTemplates() {
		return components;
	}
}
