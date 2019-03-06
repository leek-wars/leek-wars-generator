package com.leekwars.game.attack.weapons;

import com.leekwars.game.attack.Attack;
import com.leekwars.game.items.ItemTemplate;
import com.leekwars.game.items.Items;

public class WeaponTemplate {

	private final int id;
	private final byte type;
	private final int cost;
	private final Attack attack;
	private final String name;
	private final ItemTemplate template;

	public WeaponTemplate(int id, byte type, int cost, int minRange, int maxRange, String effects, byte launchType, byte area, boolean los) {

		this.id = id;
		this.type = type;
		this.cost = cost;
		template = Items.getWeaponItemTemplate(id);

		attack = new Attack(minRange, maxRange, launchType, area, los, effects, Attack.TYPE_WEAPON, template.getId());
		name = template.getName().substring(7);
	}

	public int getId() {
		return id;
	}

	public ItemTemplate getTemplate() {
		return template;
	}

	public byte getType() {
		return type;
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
