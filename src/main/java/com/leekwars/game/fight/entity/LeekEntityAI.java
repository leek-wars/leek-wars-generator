package com.leekwars.game.fight.entity;

import com.leekwars.game.leek.LeekLog;

public class LeekEntityAI extends EntityAI {

	public LeekEntityAI(Entity leek) {
		super(leek, new LeekLog());
	}
}
