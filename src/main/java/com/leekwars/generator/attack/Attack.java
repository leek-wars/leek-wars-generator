package com.leekwars.generator.attack;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.leekwars.generator.area.Area;
import com.leekwars.generator.area.AreaAllies;
import com.leekwars.generator.area.AreaEnemies;
import com.leekwars.generator.area.AreaFirstInLine;
import com.leekwars.generator.area.AreaLaserLine;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.effect.EffectParameters;
import com.leekwars.generator.items.Item;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;

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
	public final static int USE_MAX_USES = -7;

	// Launch types
	public final static int LAUNCH_TYPE_LINE = 1;
	public final static int LAUNCH_TYPE_DIAGONAL = 2;
	public final static int LAUNCH_TYPE_STAR = 3;
	public final static int LAUNCH_TYPE_STAR_INVERTED = 4;
	public final static int LAUNCH_TYPE_DIAGONAL_INVERTED = 5;
	public final static int LAUNCH_TYPE_LINE_INVERTED = 6;
	public final static int LAUNCH_TYPE_CIRCLE = 7;

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
	private final int itemID;
	private Item item;
	private final int areaID;
	private final List<EffectParameters> effects = new ArrayList<EffectParameters>();
	private final int maxUses;

	public Attack(int minRange, int maxRange, byte launchType, byte area, boolean los, ArrayNode effects, int attackType, int itemID, int maxUses) {

		this.minRange = minRange;
		this.maxRange = maxRange;
		this.launchType = launchType;
		this.los = los;
		this.attackType = attackType;
		this.itemID = itemID;
		this.maxUses = maxUses;

		areaID = area;
		this.area = Area.getArea(this, area);

		// On charge ensuite la liste des effets
		for (var e : effects) {
			ObjectNode effect = (ObjectNode) e;
			int type = effect.get("id").intValue();
			double value1 = effect.get("value1").doubleValue();
			double value2 = effect.get("value2").doubleValue();
			int turns = effect.get("turns").intValue();
			int targets = effect.get("targets").intValue();
			int modifiers = effect.get("modifiers").intValue();
			if (type == Effect.TYPE_HEAL) {
				healAttack |= targets;
			}
			if (type == Effect.TYPE_DAMAGE) {
				dammageAttack |= targets;
			}
			this.effects.add(new EffectParameters(type, value1, value2, turns, targets, modifiers));
		}
	}

	public int getArea() {
		return areaID;
	}

	public List<Cell> getTargetCells(Map map, Leek caster, Cell target) {
		// On récupère les cases cibles
		return area.getArea(map, caster.getCell(), target, caster);
	}

	public List<Cell> getTargetCells(Map map, Cell cast_cell, Cell target) {
		// On récupère les cases cibles
		return area.getArea(map, cast_cell, target, null);
	}

	public List<Entity> getWeaponTargets(State state, Entity caster, Cell target) {

		List<Entity> returnEntities = new ArrayList<Entity>();

		// On suppose que l'autorisation de lancer le sort (minScope, maxScope,
		// launchType) a été vérifiée avant l'appel

		// On récupère les cases cibles
		List<Cell> targetCells = area.getArea(state.getMap(), caster.getCell(), target, caster);

		// On trouve les poireaux sur ces cellules
		List<Entity> targetEntities = new ArrayList<Entity>();

		for (Cell cell : targetCells) {
			if (cell.getPlayer(state.getMap()) != null) {
				targetEntities.add(cell.getPlayer(state.getMap()));
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
				if (!returnEntities.contains(targetLeek)) {
					returnEntities.add(targetLeek);
				}
			}
			// Always caster?
			if ((parameters.getModifiers() & Effect.MODIFIER_ON_CASTER) != 0 && !returnEntities.contains(caster)) {
				returnEntities.add(caster);
			}
		}
		return returnEntities;
	}

	/*
	 * On suppose que l'autorisation de lancer le sort (minRange, maxRange, launchType) a été vérifiée avant l'appel
	 */
	public List<Entity> applyOnCell(State state, Entity caster, Cell target, boolean critical) {

		List<Entity> returnEntities = new ArrayList<Entity>();

		// On récupère les cases cibles
		List<Cell> targetCells = area.getArea(state.getMap(), caster.getCell(), target, caster);

		// On trouve les poireaux sur ces cellules
		List<Entity> targetEntities = new ArrayList<Entity>();

		// Facteurs de zones pour chaque entité
		var areaFactors = new TreeMap<Integer, Double>();

		for (Cell cell : targetCells) {
			if (cell.getPlayer(state.getMap()) != null && cell.getPlayer(state.getMap()).isAlive()) {
				targetEntities.add(cell.getPlayer(state.getMap()));
				areaFactors.put(cell.getPlayer(state.getMap()).getFId(), getPowerForCell(target, cell));
			}
		}

		// On défini le jet
		double jet = state.getRandom().getDouble();

		// Apply effects
		int previousEffectTotalValue = 0;
		int propagate = 0;

		for (EffectParameters parameters : effects) {

			if (caster.isDead()) continue;

			if (parameters.getId() == Effect.TYPE_ATTRACT) {
				for (Entity entity : targetEntities) {
					// Attract directly to target cell
					Cell destination = state.getMap().getAttractLastAvailableCell(entity.getCell(), target, caster.getCell());
					state.slideEntity(entity, destination, caster);
				}
			} else if (parameters.getId() == Effect.TYPE_PUSH) {
				for (Entity entity : targetEntities) {
					// Find last available position to push
					Cell destination = state.getMap().getPushLastAvailableCell(entity.getCell(), target, caster.getCell());
					state.slideEntity(entity, destination, caster);
				}
			}

			if (parameters.getId() == Effect.TYPE_TELEPORT) {

				state.teleportEntity(caster, target, caster);
				returnEntities.add(caster);

			} else if (parameters.getId() == Effect.TYPE_PROPAGATION) {

				propagate = (int) parameters.getValue1();

			} else {

				int modifiers = parameters.getModifiers();
				boolean onCaster = (modifiers & Effect.MODIFIER_ON_CASTER) != 0;
				boolean stackable = (modifiers & Effect.MODIFIER_STACKABLE) != 0;
				int effectTotalValue = 0;
				boolean multiplied_by_target_count = (modifiers & Effect.MODIFIER_MULTIPLIED_BY_TARGETS) != 0;
				boolean not_replaceable = (modifiers & Effect.MODIFIER_NOT_REPLACEABLE) != 0;
				List<Entity> effectTargetEntities = new ArrayList<Entity>();

				for (Entity targetEntity : targetEntities) {
					if (targetEntity.isDead()) continue;
					if (!filterTarget(parameters.getTargets(), caster, targetEntity)) {
						continue;
					}
					if (onCaster && targetEntity == caster) {
						continue;
					}
					// L'effet est déjà sur la cible et pas remplaçable
					if (not_replaceable && targetEntity.hasEffect(itemID)) {
						continue;
					}
					if (!returnEntities.contains(targetEntity)) {
						returnEntities.add(targetEntity);
					}
					effectTargetEntities.add(targetEntity);
				}
				int targetCount = multiplied_by_target_count ? effectTargetEntities.size() : 1;

				if (!onCaster) { // If the effect is on caster, we only count the targets, not apply the effect
					for (Entity targetEntity : effectTargetEntities) {

						double aoe = areaFactors.get(targetEntity.getFId());

						effectTotalValue += Effect.createEffect(state, parameters.getId(), parameters.getTurns(), aoe, parameters.getValue1(), parameters.getValue2(), critical, targetEntity, caster, this, jet, stackable, previousEffectTotalValue, targetCount, propagate, modifiers);
					}
				}

				// Always caster
				if (onCaster) {
					returnEntities.add(caster);
					Effect.createEffect(state, parameters.getId(), parameters.getTurns(), 1, parameters.getValue1(), parameters.getValue2(), critical, caster, caster, this, jet, stackable, previousEffectTotalValue, targetCount, propagate, modifiers);
				}

				previousEffectTotalValue = effectTotalValue;
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
		if ((targets & Effect.TARGET_NON_SUMMONS) == 0 && !target.isSummon()) {
			return false;
		}

		// Summons
		if ((targets & Effect.TARGET_SUMMONS) == 0 && target.isSummon()) {
			return false;
		}

		return true;
	}

	// Compute the area effect attenuation : 100% at center, 50% on the border
	public double getPowerForCell(Cell target_cell, Cell current_cell) {

		if (area instanceof AreaLaserLine || area instanceof AreaFirstInLine || area instanceof AreaAllies || area instanceof AreaEnemies) {
			return 1.0;
		}

		double dist = Pathfinding.getCaseDistance(target_cell, current_cell);
		// Previous formula
		// return 0.5 + (area.getRadius() - dist) / area.getRadius() * 0.5;
		return 1 - dist * 0.2;
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

	public int getItemId() {
		return itemID;
	}
	public int getType() {
		return attackType;
	}

	public boolean needsEmptyCell() {
		for (EffectParameters ep : effects) {
			if (ep.getId() == Effect.TYPE_TELEPORT || ep.getId() == Effect.TYPE_SUMMON || ep.getId() == Effect.TYPE_RESURRECT)
				return true;
		}
		return false;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public Item getItem() {
		return item;
	}

	public long getMaxUses() {
		return maxUses;
	}
}
