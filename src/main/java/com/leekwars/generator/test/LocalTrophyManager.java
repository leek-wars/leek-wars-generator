package com.leekwars.generator.test;

import com.leekwars.generator.fight.TrophyManager;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.maps.Cell;

public class LocalTrophyManager implements TrophyManager {

	@Override
	public void say(Entity entity, String message) {}

	@Override
	public void teleportation(Entity entity, Entity caster, Cell start, Cell end) {}

	@Override
	public void lama(Entity entity) {}

	@Override
	public void characteristics(Entity entity) {}

	@Override
	public void tooMuchOperations(Entity entity) {
	}

	@Override
	public void stackOverflow(Entity entity) {}

	@Override
	public void damage(Entity entity, Entity attacker, int damage, boolean direct) {}

	@Override
	public void summon(Entity entity, Entity summon) {}
}
