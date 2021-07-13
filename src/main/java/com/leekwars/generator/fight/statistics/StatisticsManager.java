package com.leekwars.generator.fight.statistics;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.maps.Cell;

public interface StatisticsManager {

	public void setGeneratorFight(Fight fight);
	public void say(Entity entity, String message);
	public void teleportation(Entity entity, Entity caster, Cell start, Cell end);
	public void lama(Entity entity);
	public void characteristics(Entity entity);
	public void tooMuchOperations(Entity entity);
	public void stackOverflow(Entity entity);
	public void damage(Entity entity, Entity attacker, int damage, boolean direct);
	public void summon(Entity entity, Entity summon);
	public void useTP(int tp);
	public void heal(Entity healer, int pv);
	public void damageReturn(Entity returner, Entity attacker, int returnDamage);
	public void poison(Entity caster, Entity target, int damages);
	public void error(Entity entity);
	public void useChip(Entity caster, Chip chip, List<Entity> targets);
	public void useWeapon(Entity caster, Weapon weapon, List<Entity> targets);
	public void kill(Entity killer, Entity entity);
	public void critical(Entity launcher);
	public void endFight(Collection<Entity> values);
	public void initializeEntities(Collection<Entity> values);
	public void addTimes(Entity current, long time, long operations);
	public void move(Entity mover, Entity entity, Cell start, List<Cell> path);
	public void resurrect(Entity caster, Entity target);
	public Map<Integer, Long> getOperationsByEntity();
	public int getKills();
	public int getBullets();
	public long getUsedChips();
	public long getSummons();
	public long getDamage();
	public long getHeal();
	public long getDistance();
	public long getStackOverflow();
	public long getErrors();
	public long getResurrects();
	public long getDamagePoison();
	public long getDamageReturn();
	public long getCriticalHits();
	public long getTPUsed();
	public long getMPUsed();
	public long getOperations();
	public long getSays();
	public long getSaysLength();
	public void tooMuchDebug(int farmer);
	public void show(Entity mEntity, int cell_id);
	public void slide(Entity entity, Entity caster, Cell start, Cell cell);
	public void useInvalidPosition(Entity caster, Attack attack, Cell target);
}
