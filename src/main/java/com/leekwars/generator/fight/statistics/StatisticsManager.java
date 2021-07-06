package com.leekwars.generator.fight.statistics;

import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.maps.Cell;

public interface StatisticsManager {

	public void say(Entity entity, String message);
	public void teleportation(Entity entity, Entity caster, Cell start, Cell end);
	public void lama(Entity entity);
	public void characteristics(Entity entity);
	public void tooMuchOperations(Entity entity);
	public void stackOverflow(Entity entity);
	public void damage(Entity entity, Entity attacker, int damage, boolean direct);
	public void summon(Entity entity, Entity summon);
}
