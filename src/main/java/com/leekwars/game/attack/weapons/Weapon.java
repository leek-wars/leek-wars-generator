package com.leekwars.game.attack.weapons;

public class Weapon {

	private final int mId;
	private final WeaponTemplate mWeaponTemplate;

	public Weapon(int id, WeaponTemplate template) {
		mId = id;
		mWeaponTemplate = template;
	}

	public int getId() {
		return mId;
	}

	public WeaponTemplate getWeaponTemplate() {
		return mWeaponTemplate;
	}
}
