package com.leekwars.game.attack.chips;

import com.leekwars.game.attack.Attack;
import com.leekwars.game.items.ItemTemplate;
import com.leekwars.game.items.Items;

public class ChipTemplate {

	private final int id;
	private final int cost;
	private final Attack attack;
	private final int cooldown;
	private final boolean teamCooldown;
	private final int initialCooldown;
	private final ItemTemplate template;
	private final String name;

	public ChipTemplate(int id, int cost, int minRange, int maxRange, String effects, byte launchType, byte area, boolean los, int cooldown, boolean teamCooldown, int initialCooldown) {

		this.id = id;
		this.cost = cost;
		this.cooldown = cooldown;
		this.teamCooldown = teamCooldown;
		this.initialCooldown = initialCooldown;
		template = Items.getChipItemTemplate(id);
		attack = new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_CHIP, template.getId());
		name = template.getName().substring(5);
	}

	public ItemTemplate getTemplate() {
		return template;
	}

	public int getId() {
		return id;
	}

	public int getCooldown() {
		return cooldown;
	}

	public boolean isTeamCooldown() {
		return teamCooldown;
	}

	public int getInitialCooldown() {
		return initialCooldown;
	}

	public int getCost() {
		return cost;
	}

	public Attack getAttack() {
		return attack;
	}

	public String getName() {
		return name;
	}
}
