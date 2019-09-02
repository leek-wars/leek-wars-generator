package com.leekwars.generator.attack;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.Generator;
import com.leekwars.generator.attack.area.Area;
import com.leekwars.generator.attack.effect.Effect;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.Summon;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;

public class Attack {

	// Attack use result constants
	public final static int USE_CRITICAL = 2;
	public final static int USE_SUCCESS = 1;
	public final static int USE_FAILED = 0;
	public final static int USE_INVALID_TARGET = -1;
	public final static int USE_NOT_ENOUGH_TP = -2;
	public final static int USE_INVALID_COOLDOWN = -3;
	public final static int USE_INVALID_POSITION = -4;
	public final static int USE_TOO_MANY_SUMMONS = -5;
	public final static int USE_RESURRECT_INVALID_ENTIITY = -6;

	// Launch types
	public final static int LAUNCH_TYPE_LINE = 0;
	public final static int LAUNCH_TYPE_CIRCLE = 1;

	// Attack types
	public final static int TYPE_WEAPON = 1;
	public final static int TYPE_CHIP = 2;

	// Attack characteristics
	private final int minRange;
	private final int maxRange;
	private final boolean los;
	private final byte launchType;
	private final Area area;
	private int healAttack = 0;
	private int dammageAttack = 0;
	private final int attackType;
	private final int attackID;
	private final int areaID;
	private final List<EffectParameters> effects = new ArrayList<EffectParameters>();

	public Attack(int minRange, int maxRange, byte launchType, byte area, boolean los, JSONArray effects, int attackType, int attackID) {

		this.minRange = minRange;
		this.maxRange = maxRange;
		this.launchType = launchType;
		this.los = los;
		this.attackType = attackType;
		this.attackID = attackID;

		areaID = area;
		this.area = Area.getArea(this, area);

		// On charge ensuite la liste des effets
		for (Object e : effects) {
			JSONObject effect = (JSONObject) e;
			int type = effect.getIntValue("id");
			double value1 = effect.getDoubleValue("value1");
			double value2 = effect.getDoubleValue("value2");
			int turns = effect.getIntValue("turns");
			int targets = effect.getIntValue("targets");
			if (type == Effect.TYPE_HEAL) {
				healAttack |= targets;
			}
			if (type == Effect.TYPE_DAMAGE) {
				dammageAttack |= targets;
			}
			this.effects.add(new EffectParameters(type, value1, value2, turns, targets));
		}
	}

	public int getArea() {
		return areaID;
	}

	public int getRadius() {
		return area.getRadius();
	}

	public boolean canLaunch(Leek caster, Cell target) {
		return false;
	}

	public List<Cell> getTargetCells(Leek caster, Cell target) {
		// On récupère les cases cibles
		return area.getArea(caster.getCell(), target);
	}

	public List<Cell> getTargetCells(Cell cast_cell, Cell target) {
		// On récupère les cases cibles
		return area.getArea(cast_cell, target);
	}

	public List<Entity> getWeaponTargets(Fight fight, Entity caster, Cell target) {

		List<Entity> returnEntities = new ArrayList<Entity>();

		// On suppose que l'autorisation de lancer le sort (minScope, maxScope,
		// launchType) a été vérifiée avant l'appel

		// On récupère les cases cibles
		List<Cell> targetCells = area.getArea(caster.getCell(), target);

		// On trouve les poireaux sur ces cellules
		List<Entity> targetEntities = new ArrayList<Entity>();

		for (Cell cell : targetCells) {
			if (cell.getPlayer() != null) {
				targetEntities.add(cell.getPlayer());
			}
		}

		// Puis on applique les effets
		for (EffectParameters parameters : effects) {
			for (Entity targetLeek : targetEntities) {

				if (targetLeek.isDead()) {
					continue;
				}
				if (!filterTarget(parameters.getTargets(), caster, targetLeek)) {
					continue;
				}

				if (!returnEntities.contains(targetLeek))
					returnEntities.add(targetLeek);
			}
			// if ((parameters.getTargets() & Effect.TARGET_CASTER) != 0 &&
			// !returnEntities.contains(caster)) {
			// returnEntities.add(caster);
			// }
		}
		return returnEntities;
	}

	public List<Entity> applyOnCell(Fight fight, Entity caster, Cell target, boolean critical) {

		List<Entity> returnEntities = new ArrayList<Entity>();

		if (effects.size() > 0 && effects.get(0).getId() == Effect.TYPE_TELEPORT) {

			caster.setHasMoved(true);
			Cell start = caster.getCell();
			caster.setCell(null);

			start.setPlayer(null);
			target.setPlayer(caster);

			returnEntities.add(caster);

			if (start.getComposante() != target.getComposante()) {
				fight.getTrophyManager().stashed(caster);
			}

			return returnEntities;
		}

		// On suppose que l'autorisation de lancer le sort (minRange, maxRange,
		// launchType) a été vérifiée avant l'appel

		// On récupère les cases cibles
		List<Cell> targetCells = area.getArea(caster.getCell(), target);

		// On trouve les poireaux sur ces cellules
		List<Entity> targetEntities = new ArrayList<Entity>();

		for (Cell cell : targetCells) {
			if (cell.getPlayer() != null) {
				targetEntities.add(cell.getPlayer());
			}
		}

		// On défini le jet
		double jet = Generator.getRandom().getDouble();

		// Apply effects
		for (EffectParameters parameters : effects) {

			for (Entity targetEntity : targetEntities) {

				if (targetEntity.isDead()) {
					continue;
				}
				if (!filterTarget(parameters.getTargets(), caster, targetEntity)) {
					continue;
				}

				if (!returnEntities.contains(targetEntity))
					returnEntities.add(targetEntity);

				double power = getPowerForCell(caster.getCell(), target, targetEntity.getCell());

				Effect.createEffect(fight, parameters.getId(), parameters.getTurns(), power, parameters.getValue1(), parameters.getValue2(), critical, targetEntity, caster, attackType, attackID, jet);
			}
		}
		return returnEntities;
	}

	private boolean filterTarget(int targets, Entity caster, Entity target) {

		// Enemies
		if ((targets & Effect.TARGET_ENEMIES) == 0 && caster.getTeam() != target.getTeam()) {
			return false;
		}

		// Allies
		if ((targets & Effect.TARGET_ALLIES) == 0 && caster.getTeam() == target.getTeam()) {
			return false;
		}

		// Caster
		if ((targets & Effect.TARGET_CASTER) == 0 && caster == target) {
			return false;
		}

		// Non-Summons
		if ((targets & Effect.TARGET_NON_SUMMONS) == 0 && !(target instanceof Summon)) {
			return false;
		}

		// Summons
		if ((targets & Effect.TARGET_SUMMONS) == 0 && target instanceof Summon) {
			return false;
		}

		return true;
	}

	// Compute the area effect attenuation : 100% at center, 50% on the border
	public double getPowerForCell(Cell launch_cell, Cell target_cell, Cell curent_cell) {

		if (area.getRadius() == 0) {
			return 1.0;
		}
		double dist = Pathfinding.getCaseDistance(target_cell, curent_cell);
		return (area.getRadius() - dist) / area.getRadius() * 0.5 + 0.5;
	}

	public int getMinRange() {
		return this.minRange;
	}

	public int getMaxRange() {
		return maxRange;
	}

	public byte getLaunchType() {
		return launchType;
	}

	public boolean needLos() {
		return los;
	}

	public List<EffectParameters> getEffects() {
		return effects;
	}

	public EffectParameters getEffectParametersByType(int type) {
		for (EffectParameters ep : effects) {
			if (ep.getId() == type)
				return ep;
		}
		return null;
	}

	public boolean isHealAttack(int target) {
		return (healAttack & target) != 0;
	}

	public boolean isDamageAttack(int target) {
		return (dammageAttack & target) != 0;
	}

}
