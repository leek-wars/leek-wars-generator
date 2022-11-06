package com.leekwars.generator.classes;

import java.util.List;

import com.leekwars.generator.FightConstants;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.items.Items;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;

import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.common.Error;

public class ChipClass {

	// ---- Fonctions Chip ----

	public static Object getCurrentCooldown(EntityAI ai, long chip_id) throws LeekRunException {
		((EntityAI) ai).addSystemLog(FarmerLog.WARNING, Error.DEPRECATED_FUNCTION, new String[] { "getCurrentCooldown", "getCooldown" });
		return null;
	}

	public static Object getCurrentCooldown(EntityAI ai, long chip_id, long v) throws LeekRunException {
		((EntityAI) ai).addSystemLog(FarmerLog.WARNING, Error.DEPRECATED_FUNCTION, new String[] { "getCurrentCooldown", "getCooldown" });
		return null;
	}

	public static long getCooldown(EntityAI ai, long chip_id) throws LeekRunException {
		var chipTemplate = Chips.getChip((int) chip_id);
		return (long) ai.getFight().getCooldown(ai.getEntity(), chipTemplate);
	}

	public static Object getCooldown(EntityAI ai, long chip_id, Object v) throws LeekRunException {
		if (v == null) {
			var chipTemplate = Chips.getChip((int) chip_id);
			return (long) ai.getFight().getCooldown(ai.getEntity(), chipTemplate);
		}
		if (v instanceof Number) {
			var l = ai.getFight().getEntity(((Number) v).intValue());
			if (l != null) {
				Chip chipTemplate = Chips.getChip((int) chip_id);
				return (long) ai.getFight().getCooldown(l, chipTemplate);
			}
		}
		return null;
	}

	public static long useChip(EntityAI ai, long chip_id) throws LeekRunException {
		return useChip(ai, chip_id, ai.getEntity().getFId());
	}

	public static long useChip(EntityAI ai, long chip_id, long leek_id) throws LeekRunException {

		long success = -1;
		Entity target = ai.getFight().getEntity(leek_id);
		Chip chip = ai.getEntity().getChip((int) chip_id);

		if (chip == null) {
			Chip ct = Chips.getChip((int) chip_id);
			if (ct == null) {
				ai.addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id) });
			} else {
				ai.addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EQUIPPED, new String[] { String.valueOf(chip_id), ct.getName() });
			}
		}
		if (target != null && chip != null && !target.isDead()) {
			success = ai.getFight().useChip(ai.getEntity(), target.getCell(), chip);
		}

		// Mort pendant le lancement, on arrête l'IA
		if (ai.getEntity().isDead()) {
			throw new LeekRunException(Error.ENTITY_DIED);
		}
		return success;
	}

	public static long useChipOnCell(EntityAI ai, long chip_id, long cell_id) throws LeekRunException {

		long success = -1;
		Cell target = ai.getFight().getMap().getCell((int) cell_id);
		Chip template = ai.getEntity().getChip((int) chip_id);

		if (template == null) {
			Chip ct = Chips.getChip((int) chip_id);
			if (ct == null) {
				ai.addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id) });
			} else {
				ai.addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EQUIPPED, new String[] { String.valueOf(chip_id), ct.getName() });
			}
		}
		if (target != null && template != null) {
			success = ai.getFight().useChip(ai.getEntity(), target, template);
		}

		// Mort pendant le lancement, on arrête l'IA
		if (ai.getEntity().isDead()) {
			throw new LeekRunException(Error.ENTITY_DIED);
		}
		return success;
	}

	public static boolean canUseChipOnCell(EntityAI ai, long chip_id, long cell_id) throws LeekRunException {
		Cell target = ai.getFight().getMap().getCell((int) cell_id);
		Chip template = ai.getEntity().getChip((int) chip_id);
		if (target != null && template != null && ai.getEntity().getCell() != null) {
			return Pathfinding.canUseAttack(ai.getEntity().getCell(), target, template.getAttack());
		}
		return false;
	}

	public static boolean canUseChip(EntityAI ai, long chip_id, long leek_id) throws LeekRunException {
		Entity target = ai.getFight().getEntity((int) leek_id);
		Chip template = ai.getEntity().getChip((int) chip_id);
		if (target != null && template != null && target.getCell() != null && ai.getEntity().getCell() != null) {
			return Pathfinding.canUseAttack(ai.getEntity().getCell(), target.getCell(), template.getAttack());
		}
		return false;
	}

	public static GenericArrayLeekValue getChipTargets(EntityAI ai, long chip_id, long cell_id) throws LeekRunException {
		Cell target = ai.getFight().getMap().getCell((int) cell_id);
		Chip template = Chips.getChip((int) chip_id);
		if (target != null && template != null) {
			var retour = ai.newArray();
			var entities = template.getAttack().getWeaponTargets(ai.getFight(), ai.getEntity(), ai.getFight().getMap().getCell((int) cell_id));
			for (Entity l : entities) {
				retour.push(ai, (long) l.getFId());
			}
			return retour;
		}
		return null;
	}

	public static String getChipName(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null)
			return "";
		return chip.getName();
	}

	public static long getChipCooldown(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null)
			return 0;
		return chip.getCooldown();
	}

	public static Object getChipMinScope(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null) {
			return null;
		}
		return (long) chip.getAttack().getMinRange();
	}

	public static Object getChipMinRange(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null) {
			return null;
		}
		return (long) chip.getAttack().getMinRange();
	}

	public static Object getChipMaxScope(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null) {
			return null;
		}
		return (long) chip.getAttack().getMaxRange();
	}

	public static Object getChipMaxRange(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null) {
			return null;
		}
		return (long) chip.getAttack().getMaxRange();
	}

	// Deprecated : always 0
	public static long getChipFailure(EntityAI ai, long id) {
		return 0l;
	}

	public static Object getChipCost(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null) {
			return null;
		}
		return (long) chip.getCost();
	}

	public static boolean isInlineChip(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null)
			return false;
		return chip.getAttack().getLaunchType() == Attack.LAUNCH_TYPE_LINE;
	}

	public static GenericArrayLeekValue getChipEffects(EntityAI ai, long id) throws LeekRunException {
		Chip chip = Chips.getChip((int) id);
		if (chip == null) {
			return null;
		}
		var retour = ai.newArray();
		for (var feature : chip.getAttack().getEffects()) {
			retour.pushNoClone(ai, feature.getFeatureArray(ai));
		}
		return retour;
	}

	public static long summon(EntityAI ai, long chip, long cell, FunctionLeekValue summonAI) throws LeekRunException {

		int success = -1;

		Cell target = ai.getFight().getMap().getCell((int) cell);
		if (target == null)
			return -1;

		if (!(summonAI instanceof FunctionLeekValue))
			return -1;

		Chip template = ai.getEntity().getChip((int) chip);
		if (template == null) {
			Chip ct = Chips.getChip((int) chip);
			if (ct == null)
				ai.addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(ai.integer(chip)) });
			else
				ai.addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EQUIPPED, new String[] { String.valueOf(ai.integer(chip)), ct.getName() });
			return -1;
		}

		if (target != null && template != null) {
			success = ai.getFight().summonEntity(ai.getEntity(), target, template, (FunctionLeekValue) summonAI);
		}
		return success;
	}

	public static long resurrect(EntityAI ai, long entity, long cell) throws LeekRunException {

		int success = -1;

		Cell target = ai.getFight().getMap().getCell((int) cell);
		if (target == null) {
			return -1;
		}

		Entity l = ai.getFight().getEntity(ai.integer(entity));
		if (l == null || !l.isDead()) {
			return FightConstants.USE_RESURRECT_INVALID_ENTIITY.getIntValue();
		}

		Chip template = ai.getEntity().getChip(FightConstants.CHIP_RESURRECTION.getIntValue());
		if (template == null) {

			Chip ct = Chips.getChip(FightConstants.CHIP_RESURRECTION.getIntValue());

			if (ct == null)
				ai.addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(FightConstants.CHIP_RESURRECTION) });
			else
				ai.addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EQUIPPED, new String[] { String.valueOf(FightConstants.CHIP_RESURRECTION), ct.getName() });
			return -1;
		}

		if (target != null && template != null) {
			success = ai.getFight().resurrectEntity(ai.getEntity(), target, template, l);
		}
		return success;
	}

	public static GenericArrayLeekValue getChipEffectiveArea(EntityAI ai, long value1, long value2) throws LeekRunException {
		return getChipEffectiveArea(ai, value1, value2, null);
	}

	/**
	 * Retourne la liste des cellules affectées par le sort lancé sur la cellule
	 * target_cell
	 *
	 * @param chip_id
	 *            Id du sort à lancer
	 * @param target_cell
	 *            Cellule cible de l'attaque
	 * @return Array des cellules affectées
	 * @throws LeekRunException
	 */
	public static GenericArrayLeekValue getChipEffectiveArea(EntityAI ai, long value1, long value2, Object value3) throws LeekRunException {

		Cell start_cell = ai.getEntity().getCell();
		if (value3 != null) {
			start_cell = ai.getFight().getMap().getCell(ai.integer(value3));
		}
		// On vérifie que la cellule de départ existe

		if (start_cell == null)
			return null;
		// On récupère la cellule
		Cell c = ai.getFight().getMap().getCell((int) value2);
		if (c == null || ai.getEntity().getCell() == null)
			return null;
		// On récupère le sort
		Chip template = Chips.getChip((int) value1);

		if (template == null)
			return null;

		var retour = ai.newArray();
		// On récupère les cellules touchées
		List<Cell> cells = template.getAttack().getTargetCells(start_cell, c);
		// On les met dans le tableau
		if (cells != null) {
			for (Cell cell : cells) {
				retour.push(ai, (long) cell.getId());
			}
		}
		return retour;
	}

	public static LegacyArrayLeekValue getAllChips_v1_3(EntityAI ai) throws LeekRunException {
		var retour = new LegacyArrayLeekValue();
		for (var chip : Chips.getTemplates().values()) {
			retour.push(ai, (long) chip.getId());
		}
		return retour;
	}

	public static ArrayLeekValue getAllChips(EntityAI ai) throws LeekRunException {
		var retour = new ArrayLeekValue(ai, Chips.getTemplates().size());
		for (var chip : Chips.getTemplates().values()) {
			retour.push(ai, (long) chip.getId());
		}
		return retour;
	}

	public static boolean chipNeedLos(EntityAI ai, long id) {
		Chip chip = Chips.getChip((int) id);
		if (chip == null) {
			return false;
		}
		return chip.getAttack().needLos();
	}

	public static boolean isChip(EntityAI ai, long id) {
		Integer i = Items.getType((int) id);
		if (i == null) {
			return false;
		}
		return i == Items.TYPE_CHIP;
	}

	public static Object getChipLaunchType(EntityAI ai, Object chip_id) throws LeekRunException {
		Chip template = Chips.getChip(ai.integer(chip_id));
		if (template == null)
			return null;
		return (long) template.getAttack().getLaunchType();
	}

	public static Object getChipArea(EntityAI ai, Object value) throws LeekRunException {
		if (value instanceof Number) {
			Chip template = Chips.getChip(ai.integer(value));
			if (template != null) {
				return (long) template.getAttack().getArea();
			}
		}
		return null;
	}
}
