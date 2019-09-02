package com.leekwars.generator.fight.entity;

import com.leekwars.generator.leek.LeekLog;

public class LeekEntityAI extends EntityAI {

	public LeekEntityAI(Entity leek) {
		super(leek, new LeekLog(leek));
	}
}
