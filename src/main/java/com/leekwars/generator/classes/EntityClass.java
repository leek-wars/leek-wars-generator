package com.leekwars.generator.classes;

import java.util.List;

import com.leekwars.generator.Censorship;
import com.leekwars.generator.action.ActionLama;
import com.leekwars.generator.action.ActionSay;
import com.leekwars.generator.action.ActionSetWeapon;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.entity.Say;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.weapons.Weapon;
import com.leekwars.generator.weapons.Weapons;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;

import java.util.List;

import leekscript.AILog;
import leekscript.compiler.AIFile;
import leekscript.runner.classes.SystemClass;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.SetLeekValue;

public class EntityClass {

	private static final int SAY_LENGTH_LIMIT = 100;

	// Combat actions are denied during beforeFight()/afterFight() hooks — no turn is
	// active, so consuming TP or emitting an entity-less action (say/lama) would
	// corrupt fight state or be misattributed in the report (the client falls back to
	// the current-turn entity, which is undefined outside a turn → frozen report).
	private static boolean denyDuringHook(EntityAI ai, String funcName) {
		return ai.denyDuringHook(funcName);
	}

	/**
	 * Target resolver for equipment- and stat-dependent natives. Returns null when
	 * the target doesn't exist or when masked by the beforeFight() hook.
	 *
	 * Spec §2.2 (symmetry of execution order): AIs running beforeFight() cannot read
	 * equipment / stats / effects of other entities, so the second-to-execute can't
	 * react to the first's setLoadout choice. Self queries (value == null) and the
	 * afterFight() hook are unmasked.
	 *
	 * Masked via this helper: getLife, getForce/Strength, getWisdom, getResistance,
	 * getAgility, getScience, getMagic, getAbsoluteShield, getRelativeShield,
	 * getDamageReturn, getFrequency, getCores, getRAM, getStat, getStats, getMP, getTP,
	 * getTotalMP, getTotalTP, getPower, getTotalLife, getWeapon, getWeapons,
	 * getChips, getComponents, getEffects, getLaunchedEffects, getPassiveEffects.
	 *
	 * Intentionally NOT masked (public lobby info, not loadout-dependent): getCell,
	 * getName, getAIName, getTeamName, getCompositionName, getFarmerName,
	 * getFarmerCountry, getFarmerID, getTeamID (but see identityTarget in anonymous fights), getType, getMobType, getBulbType, getPlantType, getPlantTrigger,
	 * getAwakeningZone, getPlantTriggers,
	 * getSummons, isSummon, getBirthTurn, getSummoner, isStatic, getLeekID, getSide,
	 * getLevel, getEntityTurnOrder, getAIID. These return info already visible in
	 * the lobby or fixed at fight init regardless of loadout. getAIPath only answers for
	 * AIs owned by the caller's farmer.
	 */
	private static Entity resolveStatTarget(EntityAI ai, Object value) {
		Entity l = resolveEntity(ai, value);
		if (l != null && ai.isInBeforeFightHook() && l != ai.getEntity()) return null;
		return l;
	}

	public static long getLife(EntityAI ai) {
		return (long) ai.getEntity().getLife();
	}

	public static Long getLife(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getLife();
	}

	public static long getForce(EntityAI ai) {
		return (long) ai.getEntity().getStrength();
	}

	public static Long getForce(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getStrength();
	}

	public static long getStrength(EntityAI ai) {
		return (long) ai.getEntity().getStrength();
	}

	public static Long getStrength(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getStrength();
	}

	public static long getWisdom(EntityAI ai) {
		return (long) ai.getEntity().getWisdom();
	}

	public static Long getWisdom(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getWisdom();
	}

	public static long getResistance(EntityAI ai) {
		return (long) ai.getEntity().getResistance();
	}

	public static Long getResistance(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getResistance();
	}

	public static long getAgility(EntityAI ai) {
		return (long) ai.getEntity().getAgility();
	}

	public static Long getAgility(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getAgility();
	}

	public static long getScience(EntityAI ai) {
		return (long) ai.getEntity().getScience();
	}

	public static Long getScience(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getScience();
	}

	public static long getMagic(EntityAI ai) {
		return (long) ai.getEntity().getMagic();
	}

	public static Long getMagic(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getMagic();
	}

	public static long getAbsoluteShield(EntityAI ai) {
		return (long) ai.getEntity().getAbsoluteShield();
	}

	public static Long getAbsoluteShield(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getAbsoluteShield();
	}

	public static long getRelativeShield(EntityAI ai) {
		return (long) ai.getEntity().getRelativeShield();
	}

	public static Long getRelativeShield(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getRelativeShield();
	}

	public static long getDamageReturn(EntityAI ai) {
		return (long) ai.getEntity().getDamageReturn();
	}

	public static Long getDamageReturn(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getDamageReturn();
	}

	public static long getFrequency(EntityAI ai) {
		return (long) ai.getEntity().getFrequency();
	}

	public static Long getFrequency(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getFrequency();
	}

	public static long getCores(EntityAI ai) {
		return ai.getEntity().getCores();
	}

	public static Long getCores(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getCores();
	}

	public static long getRAM(EntityAI ai) {
		return ai.getEntity().getRAM();
	}

	public static Long getRAM(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getRAM();
	}

	public static long getStat(EntityAI ai, long stat) {
		return (long) ai.getEntity().getStat((int) stat);
	}

	public static Long getStat(EntityAI ai, Object entity, long stat) {
		var l = resolveStatTarget(ai, entity);
		return l == null ? null : (long) l.getStat((int) stat);
	}

	private final static int[] ALL_STATS = { Entity.STAT_LIFE, Entity.STAT_TP, Entity.STAT_MP,
		Entity.STAT_STRENGTH, Entity.STAT_AGILITY, Entity.STAT_FREQUENCY, Entity.STAT_WISDOM,
		Entity.STAT_ABSOLUTE_SHIELD, Entity.STAT_RELATIVE_SHIELD, Entity.STAT_RESISTANCE,
		Entity.STAT_SCIENCE, Entity.STAT_MAGIC, Entity.STAT_DAMAGE_RETURN, Entity.STAT_POWER,
		Entity.STAT_CORES, Entity.STAT_RAM };

	public static MapLeekValue getStats(EntityAI ai) throws LeekRunException {
		return statsMap(ai, ai.getEntity());
	}

	public static MapLeekValue getStats(EntityAI ai, Object entity) throws LeekRunException {
		var l = resolveStatTarget(ai, entity);
		return l == null ? null : statsMap(ai, l);
	}

	private static MapLeekValue statsMap(EntityAI ai, Entity l) throws LeekRunException {
		var map = new MapLeekValue(ai);
		for (var stat : ALL_STATS)
			map.set(ai, (long) stat, (long) l.getStat(stat));
		return map;
	}

	public static Long getCell(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getCell() != null)
			return (long) ai.getEntity().getCell().getId();
		return null;
	}

	public static Long getCell(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			if (ai.getEntity().getCell() != null)
				return (long) ai.getEntity().getCell().getId();
		}
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null && l.getCell() != null)
				return (long) l.getCell().getId();
		}
		return null;
	}

	public static Long getWeapon(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getWeapon() != null)
			return (long) ai.getEntity().getWeapon().getId();
		return null;
	}

	public static Long getWeapon(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null || l.getWeapon() == null) return null;
		return (long) l.getWeapon().getId();
	}

	public static String getName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getName();
	}

	public static String getName(EntityAI ai, Object value) throws LeekRunException {
		// Seul un poireau porte le nom d'un joueur : invocations et tourelles gardent le leur
		var l = resolveEntity(ai, value);
		if (l == null || l.getType() != Entity.TYPE_LEEK) return l == null ? null : l.getName();
		return identityTarget(ai, value) == null ? null : l.getName();
	}

	public static long getMP(EntityAI ai) {
		return (long) ai.getEntity().getMP();
	}

	/**
	 * Retourne une LeekVariable contenant le nombre de PM du poireau demandé
	 * (ou du poireau du joueur s'il s'agit d'une NullLeekValue)
	 *
	 * @param value
	 *            Leek dont on veut connaitre le nombre de PM
	 * @return Nombre de PM du leek ou Null si le leek est invalide
	 * @throws LeekRunException
	 */
	public static Long getMP(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getMP();
	}

	public static long getTP(EntityAI ai) {
		return (long) ai.getEntity().getTP();
	}

	public static Long getTP(EntityAI ai, Object value) {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getTP();
	}

	public static long getTotalMP(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTotalMP();
	}

	public static Long getTotalMP(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getTotalMP();
	}

	public static long getTotalTP(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTotalTP();
	}

	public static Long getTotalTP(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getTotalTP();
	}

	public static long getPower(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getPower();
	}

	public static Long getPower(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getPower();
	}

	public static boolean setWeapon(EntityAI ai, long weapon_id) throws LeekRunException {

		if (denyDuringHook(ai, "setWeapon")) return false;

		var weapon = Weapons.getWeapon((int) weapon_id);
		if (weapon == null) {
			ai.addSystemLog(AILog.WARNING, FarmerLog.WEAPON_NOT_EXISTS, new String[] { String.valueOf(weapon_id) });
			return false;
		}

		Weapon w = null;
		for (var w1 : ai.getEntity().getWeapons()) {
			if (w1.getId() == weapon_id) {
				w = w1;
				break;
			}
		}
		if (w == null) {
			ai.addSystemLog(AILog.WARNING, FarmerLog.WEAPON_NOT_EQUIPPED, new String[] { String.valueOf(weapon_id), weapon.getName() });
			return false;
		}

		return ai.getState().setWeapon(ai.getEntity(), weapon);
	}

	public static boolean say(EntityAI ai, Object messageObject) throws LeekRunException {
		if (denyDuringHook(ai, "say")) return false;
		if (ai.getEntity().getTP() < 1) {
			return false;
		}
		ai.getEntity().useTP(1);
		if (ai.getEntity().saysTurn >= Entity.SAY_LIMIT_TURN) {
			return false;
		}
		String message = ai.string(messageObject);
		ai.getEntity().saysTurn++;
		if (message.length() > SAY_LENGTH_LIMIT) {
			message = message.substring(0, SAY_LENGTH_LIMIT);
		}
		message = Censorship.checkString(ai.getFight(), message);
		ai.getFight().log(new ActionSay(message));
		var sayData = new Say(ai.getEntity().getFId(), message);
		for (var entity : ai.getState().getEntities().values()) {
			if (entity != ai.getEntity() && entity.isAlive() && entity.getAI() != null) {
				((EntityAI) entity.getAI()).getSays().add(sayData);
			}
		}
		ai.getFight().getState().statistics.say(ai.getEntity(), message);
		return true;
	}

	public static Object lama(EntityAI ai) {
		if (denyDuringHook(ai, "lama")) return null;
		if (ai.getEntity().getTP() < 1) {
			return null;
		}
		ai.getEntity().useTP(1);
		ai.getFight().log(new ActionLama());
		ai.getFight().getState().statistics.lama(ai.getEntity());
		return null;
	}

	/**
	 * Messages entendus par l'entité qui joue. Un bulbe exécute une fermeture de l'IA de son
	 * invocateur : `ai` est alors l'IA de l'invocateur, dont la liste est vidée à la fin de son
	 * tour. Le bulbe a la sienne, remplie par say() et vidée à la fin de son propre tour.
	 */
	private static List<Say> heard(EntityAI ai) {
		return ((EntityAI) ai.getEntity().getAI()).getSays();
	}

	public static LegacyArrayLeekValue listen_v1_3(EntityAI ai) throws LeekRunException {
		var values = new LegacyArrayLeekValue(ai);
		for (var say : heard(ai)) {
			var s = ai.newArray(2);
			s.push(ai, say.id);
			s.push(ai, say.message);
			values.pushNoClone(ai, s);
		}
		return values;
	}

	public static ArrayLeekValue listen(EntityAI ai) throws LeekRunException {
		var values = new ArrayLeekValue(ai);
		for (var say : heard(ai)) {
			var s = ai.newArray(2);
			s.push(ai, say.id);
			s.push(ai, say.message);
			values.pushNoClone(ai, s);
		}
		return values;
	}

	public static LegacyArrayLeekValue getWeapons_v1_3(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new LegacyArrayLeekValue(ai);
		for (var weapon : l.getWeapons()) {
			retour.push(ai, (long) weapon.getId());
		}
		return retour;
	}

	public static ArrayLeekValue getWeapons(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new ArrayLeekValue(ai);
		for (var weapon : l.getWeapons()) {
			retour.push(ai, (long) weapon.getId());
		}
		return retour;
	}

	public static LegacyArrayLeekValue getWeapons_v1_3(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new LegacyArrayLeekValue(ai);
		for (var weapon : l.getWeapons()) {
			retour.push(ai, (long) weapon.getId());
		}
		return retour;
	}

	public static ArrayLeekValue getWeapons(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new ArrayLeekValue(ai);
		for (var weapon : l.getWeapons()) {
			retour.push(ai, (long) weapon.getId());
		}
		return retour;
	}

	public static boolean isEnemy(EntityAI ai, long id) {
		var l = ai.getFight().getEntity(id);
		if (l == null)
			return false;
		return ai.getEntity().getTeam() != l.getTeam();
	}

	public static boolean isAlly(EntityAI ai, long id) {
		var l = ai.getFight().getEntity(id);
		if (l == null)
			return false;
		return ai.getEntity().getTeam() == l.getTeam();
	}

	public static boolean isAlive(EntityAI ai, long id) {
		var l = ai.getFight().getEntity(id);
		if (l == null)
			return false;
		return l.isAlive();
	}

	public static boolean isDead(EntityAI ai, long id) {
		var l = ai.getFight().getEntity(id);
		if (l == null)
			return false;
		return l.isDead();
	}

	public static long getLeek(EntityAI ai) {
		return ai.getEntity().getFId();
	}

	public static long getEntity(EntityAI ai) {
		return ai.getEntity().getFId();
	}

	public static LegacyArrayLeekValue getChips_v1_3(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var result = new LegacyArrayLeekValue(ai);
		for (var chip : l.getChips()) {
			result.push(ai, (long) chip.getId());
		}
		return result;
	}

	public static ArrayLeekValue getChips(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var result = new ArrayLeekValue(ai, l.getChips().size());
		for (var chip : l.getChips()) {
			result.push(ai, (long) chip.getId());
		}
		return result;
	}

	public static ArrayLeekValue getChips(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var result = new ArrayLeekValue(ai);
		for (var chip : l.getChips()) {
			result.push(ai, (long) chip.getId());
		}
		return result;
	}


	public static LegacyArrayLeekValue getChips_v1_3(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var result = new LegacyArrayLeekValue(ai);
		for (var chip : l.getChips()) {
			result.push(ai, (long) chip.getId());
		}
		return result;
	}

	public static LegacyArrayLeekValue getComponents_v1_3(EntityAI ai) throws LeekRunException {
		return legacyIds(ai, ai.getEntity().getComponentTemplates());
	}

	public static ArrayLeekValue getComponents(EntityAI ai) throws LeekRunException {
		return ids(ai, ai.getEntity().getComponentTemplates());
	}

	public static LegacyArrayLeekValue getComponents_v1_3(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : legacyIds(ai, l.getComponentTemplates());
	}

	public static ArrayLeekValue getComponents(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : ids(ai, l.getComponentTemplates());
	}

	private static ArrayLeekValue ids(EntityAI ai, List<Integer> values) throws LeekRunException {
		var result = new ArrayLeekValue(ai, values.size());
		for (int id : values) {
			result.push(ai, (long) id);
		}
		return result;
	}

	private static LegacyArrayLeekValue legacyIds(EntityAI ai, List<Integer> values) throws LeekRunException {
		var result = new LegacyArrayLeekValue(ai);
		for (int id : values) {
			result.push(ai, (long) id);
		}
		return result;
	}

	public static LegacyArrayLeekValue getEffects_v1_3(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new LegacyArrayLeekValue(ai);
		for (Effect effect : l.getEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static ArrayLeekValue getEffects(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new ArrayLeekValue(ai, l.getEffects().size());
		for (Effect effect : l.getEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static ArrayLeekValue getEffects(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new ArrayLeekValue(ai, l.getEffects().size());
		for (Effect effect : l.getEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static LegacyArrayLeekValue getEffects_v1_3(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new LegacyArrayLeekValue(ai);
		for (Effect effect : l.getEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static LegacyArrayLeekValue getLaunchedEffects_v1_3(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new LegacyArrayLeekValue(ai);
		for (var effect : l.getLaunchedEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static ArrayLeekValue getLaunchedEffects(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new ArrayLeekValue(ai, l.getLaunchedEffects().size());
		for (var effect : l.getLaunchedEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static ArrayLeekValue getLaunchedEffects(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new ArrayLeekValue(ai, l.getLaunchedEffects().size());
		for (var effect : l.getLaunchedEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}


	public static LegacyArrayLeekValue getLaunchedEffects_v1_3(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new LegacyArrayLeekValue(ai);
		for (var effect : l.getLaunchedEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static LegacyArrayLeekValue getPassiveEffects_v1_3(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new LegacyArrayLeekValue(ai);
		for (var feature : l.getPassiveEffects()) {
			retour.pushNoClone(ai, ai.getFeatureArray(feature));
		}
		return retour;
	}

	public static ArrayLeekValue getPassiveEffects(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var retour = new ArrayLeekValue(ai, l.getPassiveEffects().size());
		for (var feature : l.getPassiveEffects()) {
			retour.pushNoClone(ai, ai.getFeatureArray(feature));
		}
		return retour;
	}

	public static ArrayLeekValue getPassiveEffects(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new ArrayLeekValue(ai);
		for (var feature : l.getPassiveEffects()) {
			retour.pushNoClone(ai, ai.getFeatureArray(feature));
		}
		return retour;
	}

	public static LegacyArrayLeekValue getPassiveEffects_v1_3(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		if (l == null) return null;
		var retour = new LegacyArrayLeekValue(ai);
		for (var feature : l.getPassiveEffects()) {
			retour.pushNoClone(ai, ai.getFeatureArray(feature));
		}
		return retour;
	}

	public static String getAIName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getAIName();
	}

	public static String getAIName(EntityAI ai, Object value) throws LeekRunException {
		var l = identityTarget(ai, value);
		return l == null ? null : l.getAIName();
	}

	/** Chemin de l'IA d'une entité, si son fichier appartient au même éleveur que celui de l'appelant ; sinon privé (getAIName en donne le nom de fichier). */
	public static String getAIPath(EntityAI ai) throws LeekRunException {
		return aiPath(ai.getEntity(), ai.getEntity());
	}

	public static String getAIPath(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveEntity(ai, value);
		return l == null ? null : aiPath(ai.getEntity(), l);
	}

	/**
	 * Le propriétaire du fichier d'IA, et non l'éleveur de l'entité : une tourelle (éleveur -1) ou un
	 * bot du combat de test (éleveur 0) exécute l'IA d'un éleveur précis.
	 */
	private static String aiPath(Entity me, Entity target) {
		var mine = aiFileOf(me);
		var file = aiFileOf(target);
		return mine != null && file != null && file.getOwner() == mine.getOwner() ? SystemClass.sourcePath(file.getPath()) : null;
	}

	/** Fichier de l'IA qu'exécute une entité : celui de son invocateur pour une invocation. */
	private static AIFile aiFileOf(Entity entity) {
		for (var e = entity; e != null; e = e.getSummoner()) {
			if (e.getAIFile() instanceof AIFile file) return file;
		}
		return null;
	}

	public static String getTeamName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getTeamName();
	}

	public static String getTeamName(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().getTeamName();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(ai.integer(value));
			if (l != null && l.getTeamName() != null)
				return l.getTeamName();
		}
		return null;
	}

	public static String getCompositionName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getCompositionName();
	}

	public static String getCompositionName(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().getCompositionName();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(ai.integer(value));
			if (l != null && l.getCompositionName() != null)
				return l.getCompositionName();
		}
		return null;
	}

	public static String getFarmerName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getFarmerName();
	}

	public static String getFarmerName(EntityAI ai, Object value) throws LeekRunException {
		var l = identityTarget(ai, value);
		return l == null ? null : l.getFarmerName();
	}

	public static String getFarmerCountry(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getFarmerCountry();
	}

	public static String getFarmerCountry(EntityAI ai, Object value) throws LeekRunException {
		var l = identityTarget(ai, value);
		return l == null ? null : l.getFarmerCountry();
	}

	public static long getFarmerID(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getFarmer();
	}

	public static Long getFarmerID(EntityAI ai, Object value) throws LeekRunException {
		var l = identityTarget(ai, value);
		return l == null ? null : (long) l.getFarmer();
	}

	public static long getTeamID(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTeamId();
	}

	public static Long getTeamID(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getTeamId();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return (long) l.getTeamId();
		}
		return null;
	}

	public static LegacyArrayLeekValue getSummons_v1_3(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var result = new LegacyArrayLeekValue(ai);
		for (var summon : l.getSummons(false)) {
			result.push(ai, (long) summon.getFId());
		}
		return result;
	}

	public static ArrayLeekValue getSummons(EntityAI ai) throws LeekRunException {
		var l = ai.getEntity();
		var result = new ArrayLeekValue(ai);
		for (var summon : l.getSummons(false)) {
			result.push(ai, (long) summon.getFId());
		}
		return result;
	}

	public static ArrayLeekValue getSummons(EntityAI ai, Object value) throws LeekRunException {
		Entity l = null;
		if (value == null)
			l = ai.getEntity();
		else if (value instanceof Number n)
			l = ai.getFight().getEntity(n.intValue());
		if (l == null)
			return null;
		var result = new ArrayLeekValue(ai);
		for (var summon : l.getSummons(false)) {
			result.push(ai, (long) summon.getFId());
		}
		return result;
	}


	public static LegacyArrayLeekValue getSummons_v1_3(EntityAI ai, Object value) throws LeekRunException {
		Entity l = null;
		if (value == null)
			l = ai.getEntity();
		else if (value instanceof Number n)
			l = ai.getFight().getEntity(n.intValue());
		if (l == null)
			return null;
		var result = new LegacyArrayLeekValue(ai);
		for (var summon : l.getSummons(false)) {
			result.push(ai, (long) summon.getFId());
		}
		return result;
	}

	public static long getType(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getType() + 1;
	}

	public static Long getType(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			return (long) ai.getEntity().getType() + 1;
		}
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null) {
				return (long) l.getType() + 1;
			}
		}
		return null;
	}

	public static long getMobType(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getType() == Entity.TYPE_MOB) {
			return (long) ai.getEntity().getSkin();
		}
		return -1;
	}

	public static Long getMobType(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			if (ai.getEntity().getType() == Entity.TYPE_MOB) {
				return (long) ai.getEntity().getSkin();
			}
			return -1L;
		}
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null) {
				if (l.getType() == Entity.TYPE_MOB) {
					return (long) l.getSkin();
				}
				return -1L;
			}
		}
		return null;
	}

	public static long getChestType(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getType() == Entity.TYPE_CHEST) {
			return (long) ai.getEntity().getSkin();
		}
		return -1;
	}

	public static Long getChestType(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			if (ai.getEntity().getType() == Entity.TYPE_CHEST) {
				return (long) ai.getEntity().getSkin();
			}
			return -1L;
		}
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null) {
				if (l.getType() == Entity.TYPE_CHEST) {
					return (long) l.getSkin();
				}
				return -1L;
			}
		}
		return null;
	}

	public static long getBulbType(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getType() == Entity.TYPE_BULB) {
			return (long) ai.getEntity().getSkin();
		}
		return -1;
	}

	public static Long getBulbType(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			if (ai.getEntity().getType() == Entity.TYPE_BULB) {
				return (long) ai.getEntity().getSkin();
			}
			return -1L;
		}
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null) {
				if (l.getType() == Entity.TYPE_BULB) {
					return (long) l.getSkin();
				}
				return -1L;
			}
		}
		return null;
	}

	public static long getPlantType(EntityAI ai) throws LeekRunException {
		if (ai.getEntity().getType() == Entity.TYPE_PLANT) {
			return (long) ai.getEntity().getSkin();
		}
		return -1;
	}

	public static Long getPlantType(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			if (ai.getEntity().getType() == Entity.TYPE_PLANT) {
				return (long) ai.getEntity().getSkin();
			}
			return -1L;
		}
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null) {
				if (l.getType() == Entity.TYPE_PLANT) {
					return (long) l.getSkin();
				}
				return -1L;
			}
		}
		return null;
	}

	public static boolean isSummon(EntityAI ai) throws LeekRunException {
		return ai.getEntity().isSummon();
	}

	public static Boolean isSummon(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().isSummon();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return l.isSummon();
		}
		return null;
	}

	public static long getBirthTurn(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getBirthTurn();
	}

	public static Long getBirthTurn(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getBirthTurn();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return (long) l.getBirthTurn();
		}
		return null;
	}

	/**
	 * Éveil des plantes : l'entité qui vient d'entrer dans la zone de la plante en train
	 * de jouer. -1 partout ailleurs — hors d'un réveil, et pour toute entité qui n'est
	 * pas une plante à zone.
	 */
	public static long getPlantTrigger(EntityAI ai) throws LeekRunException {
		var trigger = ai.getEntity().getAwakeningTrigger();
		return trigger == null ? -1l : (long) trigger.getFId();
	}

	/**
	 * Rayon de la zone d'Éveil, en cases. 0 pour toute entité qui joue son tour
	 * normalement, y compris une plante enracinée sans zone comme le Prototaxite :
	 * c'est donc aussi la réponse à « cette entité joue-t-elle un tour ? ».
	 * La zone est un cercle au sens LW, mesuré à la distance de cases, celle que
	 * getCellDistance() rend à l'IA.
	 */
	public static long getAwakeningZone(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getAwakeningZone();
	}

	public static Long getAwakeningZone(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveEntity(ai, value);
		return l == null ? null : (long) l.getAwakeningZone();
	}

	/**
	 * Entités vivantes qui ont déjà réveillé la plante depuis le début de LEUR dernier tour, par
	 * id croissant : y entrer de nouveau ne la réveillera pas avant leur prochain tour. Toujours
	 * vide pour une entité qui n'est pas une plante à zone.
	 */
	public static LegacyArrayLeekValue getPlantTriggers_v1_3(EntityAI ai) throws LeekRunException {
		return legacyIds(ai, plantTriggers(ai, ai.getEntity()));
	}

	public static ArrayLeekValue getPlantTriggers(EntityAI ai) throws LeekRunException {
		return ids(ai, plantTriggers(ai, ai.getEntity()));
	}

	public static LegacyArrayLeekValue getPlantTriggers_v1_3(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveEntity(ai, value);
		return l == null ? null : legacyIds(ai, plantTriggers(ai, l));
	}

	public static ArrayLeekValue getPlantTriggers(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveEntity(ai, value);
		return l == null ? null : ids(ai, plantTriggers(ai, l));
	}

	// Une entité morte reste dans la liste du moteur (elle n'en sort qu'au début de son tour)
	private static List<Integer> plantTriggers(EntityAI ai, Entity plant) {
		return plant.getAwakenedBy().stream().filter(id -> {
			var e = ai.getFight().getEntity(id);
			return e != null && !e.isDead();
		}).sorted().toList();
	}

	/** Entité désignée par `value` (soi si null), sans masque : null si elle n'existe pas. */
	private static Entity resolveEntity(EntityAI ai, Object value) {
		if (value == null) return ai.getEntity();
		if (!(value instanceof Number n)) return null;
		return ai.getFight().getEntity(n.intValue());
	}

	/**
	 * Entité dont l'IA peut lire l'identité (nom, éleveur, pays, IA) : en combat anonyme (tournoi
	 * d'IA, Scenario#anonymous), seulement celles de son propre éleveur. Une IA ne reconnaît pas ses
	 * alliés d'un autre compte.
	 */
	private static Entity identityTarget(EntityAI ai, Object value) {
		var l = resolveEntity(ai, value);
		if (l == null || !ai.getState().isAnonymous()) return l;
		return l.getFarmer() == ai.getEntity().getFarmer() ? l : null;
	}

	public static long getSummoner(EntityAI ai) throws LeekRunException {
		return ai.getEntity().isSummon() ? (long) ai.getEntity().getSummoner().getFId(): -1l;
	}

	public static Long getSummoner(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().isSummon() ? (long) ai.getEntity().getSummoner().getFId(): -1l;
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return l.isSummon() ? (long) l.getSummoner().getFId(): -1l;
		}
		return null;
	}

	public static boolean isStatic(EntityAI ai) throws LeekRunException {
		return ai.getEntity().hasState(EntityState.STATIC);
	}

	public static boolean isStatic(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().hasState(EntityState.STATIC);
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return l.hasState(EntityState.STATIC);
		}
		return false;
	}

	public static long getLeekID(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getId();
	}

	public static Long getLeekID(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getId();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return (long) l.getId();
		}
		return null;
	}

	public static long getSide(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTeam();
	}

	public static Long getSide(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getTeam();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return (long) l.getTeam();
		}
		return null;
	}

	public static long getTotalLife(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTotalLife();
	}

	public static Long getTotalLife(EntityAI ai, Object value) throws LeekRunException {
		var l = resolveStatTarget(ai, value);
		return l == null ? null : (long) l.getTotalLife();
	}

	public static long getLevel(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getLevel();
	}

	public static Long getLevel(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getLevel();
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null)
				return (long) l.getLevel();
		}
		return null;
	}

	public static long getEntityTurnOrder(EntityAI ai) throws LeekRunException {
		return (long) ai.getFight().getOrder().getEntityTurnOrder(ai.getEntity());
	}

	public static Long getEntityTurnOrder(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getFight().getOrder().getEntityTurnOrder(ai.getEntity());
		if (value instanceof Number n) {
			var l = ai.getFight().getEntity(n.intValue());
			if (l != null && !l.isDead())
				return (long) ai.getFight().getOrder().getEntityTurnOrder(l);
		}
		return null;
	}

	public static long getAIID(EntityAI ai) throws LeekRunException {
		return (long) ai.getId();
	}

	public static Long getAIID(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getId();
		var l = identityTarget(ai, value);
		return l == null ? null : (long) ((EntityAI) l.getAI()).getId();
	}


	public static LegacyArrayLeekValue getStates_v1_3(EntityAI ai) throws LeekRunException {
		Entity l = ai.getEntity();
		var retour = new LegacyArrayLeekValue(ai);
		for (var state : l.getStates()) {
			retour.push(ai, (long) state.ordinal());
		}
		return retour;
	}

	public static LegacyArrayLeekValue getStates_v1_3(EntityAI ai, long value) throws LeekRunException {
		Entity l = ai.getFight().getEntity(value);
		if (l == null) {
			return null;
		}
		var retour = new LegacyArrayLeekValue(ai);
		for (var state : l.getStates()) {
			retour.push(ai, (long) state.ordinal());
		}
		return retour;
	}

	public static SetLeekValue getStates(EntityAI ai) throws LeekRunException {
		Entity l = ai.getEntity();
		var retour = new SetLeekValue(ai);
		for (var state : l.getStates()) {
			retour.setPut(ai, (long) state.ordinal());
		}
		return retour;
	}

	public static SetLeekValue getStates(EntityAI ai, long value) throws LeekRunException {
		Entity l = ai.getFight().getEntity(value);
		if (l == null) {
			return null;
		}
		var retour = new SetLeekValue(ai);
		for (var state : l.getStates()) {
			retour.setPut(ai, (long) state.ordinal());
		}
		return retour;
	}

	public static long getItemUses(EntityAI ai, long item) throws LeekRunException {
		Entity l = ai.getEntity();
		if (l == null) {
			return 0;
		}
		return l.getItemUses((int) item);
	}

	// ---- LeekScript 5 : une entité null est « aucune » ----
	// L'appel sans l'argument entité désigne toujours l'entité qui joue ; une entité null (un
	// getNearestAlly() sans allié, par exemple) n'est plus elle : le résultat vaut celui d'une
	// entité inconnue, null ou false.

	public static long getLife_v5(EntityAI ai) {
		return getLife(ai);
	}

	public static Long getLife_v5(EntityAI ai, Object value) {
		return value == null ? null : getLife(ai, value);
	}

	public static long getForce_v5(EntityAI ai) {
		return getForce(ai);
	}

	public static Long getForce_v5(EntityAI ai, Object value) {
		return value == null ? null : getForce(ai, value);
	}

	public static long getStrength_v5(EntityAI ai) {
		return getStrength(ai);
	}

	public static Long getStrength_v5(EntityAI ai, Object value) {
		return value == null ? null : getStrength(ai, value);
	}

	public static long getWisdom_v5(EntityAI ai) {
		return getWisdom(ai);
	}

	public static Long getWisdom_v5(EntityAI ai, Object value) {
		return value == null ? null : getWisdom(ai, value);
	}

	public static long getResistance_v5(EntityAI ai) {
		return getResistance(ai);
	}

	public static Long getResistance_v5(EntityAI ai, Object value) {
		return value == null ? null : getResistance(ai, value);
	}

	public static long getAgility_v5(EntityAI ai) {
		return getAgility(ai);
	}

	public static Long getAgility_v5(EntityAI ai, Object value) {
		return value == null ? null : getAgility(ai, value);
	}

	public static long getScience_v5(EntityAI ai) {
		return getScience(ai);
	}

	public static Long getScience_v5(EntityAI ai, Object value) {
		return value == null ? null : getScience(ai, value);
	}

	public static long getMagic_v5(EntityAI ai) {
		return getMagic(ai);
	}

	public static Long getMagic_v5(EntityAI ai, Object value) {
		return value == null ? null : getMagic(ai, value);
	}

	public static long getAbsoluteShield_v5(EntityAI ai) {
		return getAbsoluteShield(ai);
	}

	public static Long getAbsoluteShield_v5(EntityAI ai, Object value) {
		return value == null ? null : getAbsoluteShield(ai, value);
	}

	public static long getRelativeShield_v5(EntityAI ai) {
		return getRelativeShield(ai);
	}

	public static Long getRelativeShield_v5(EntityAI ai, Object value) {
		return value == null ? null : getRelativeShield(ai, value);
	}

	public static long getDamageReturn_v5(EntityAI ai) {
		return getDamageReturn(ai);
	}

	public static Long getDamageReturn_v5(EntityAI ai, Object value) {
		return value == null ? null : getDamageReturn(ai, value);
	}

	public static long getFrequency_v5(EntityAI ai) {
		return getFrequency(ai);
	}

	public static Long getFrequency_v5(EntityAI ai, Object value) {
		return value == null ? null : getFrequency(ai, value);
	}

	public static long getCores_v5(EntityAI ai) {
		return getCores(ai);
	}

	public static Long getCores_v5(EntityAI ai, Object value) {
		return value == null ? null : getCores(ai, value);
	}

	public static long getRAM_v5(EntityAI ai) {
		return getRAM(ai);
	}

	public static Long getRAM_v5(EntityAI ai, Object value) {
		return value == null ? null : getRAM(ai, value);
	}

	public static MapLeekValue getStats_v5(EntityAI ai) throws LeekRunException {
		return getStats(ai);
	}

	public static MapLeekValue getStats_v5(EntityAI ai, Object entity) throws LeekRunException {
		return entity == null ? null : getStats(ai, entity);
	}

	public static Long getCell_v5(EntityAI ai) throws LeekRunException {
		return getCell(ai);
	}

	public static Long getCell_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getCell(ai, value);
	}

	public static Long getWeapon_v5(EntityAI ai) throws LeekRunException {
		return getWeapon(ai);
	}

	public static Long getWeapon_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getWeapon(ai, value);
	}

	public static String getName_v5(EntityAI ai) throws LeekRunException {
		return getName(ai);
	}

	public static String getName_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getName(ai, value);
	}

	public static long getMP_v5(EntityAI ai) {
		return getMP(ai);
	}

	public static Long getMP_v5(EntityAI ai, Object value) {
		return value == null ? null : getMP(ai, value);
	}

	public static long getTP_v5(EntityAI ai) {
		return getTP(ai);
	}

	public static Long getTP_v5(EntityAI ai, Object value) {
		return value == null ? null : getTP(ai, value);
	}

	public static long getTotalMP_v5(EntityAI ai) throws LeekRunException {
		return getTotalMP(ai);
	}

	public static Long getTotalMP_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getTotalMP(ai, value);
	}

	public static long getTotalTP_v5(EntityAI ai) throws LeekRunException {
		return getTotalTP(ai);
	}

	public static Long getTotalTP_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getTotalTP(ai, value);
	}

	public static long getPower_v5(EntityAI ai) throws LeekRunException {
		return getPower(ai);
	}

	public static Long getPower_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getPower(ai, value);
	}

	public static ArrayLeekValue getWeapons_v5(EntityAI ai) throws LeekRunException {
		return getWeapons(ai);
	}

	public static ArrayLeekValue getWeapons_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getWeapons(ai, value);
	}

	public static ArrayLeekValue getChips_v5(EntityAI ai) throws LeekRunException {
		return getChips(ai);
	}

	public static ArrayLeekValue getChips_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getChips(ai, value);
	}

	public static ArrayLeekValue getComponents_v5(EntityAI ai) throws LeekRunException {
		return getComponents(ai);
	}

	public static ArrayLeekValue getComponents_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getComponents(ai, value);
	}

	public static ArrayLeekValue getEffects_v5(EntityAI ai) throws LeekRunException {
		return getEffects(ai);
	}

	public static ArrayLeekValue getEffects_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getEffects(ai, value);
	}

	public static ArrayLeekValue getLaunchedEffects_v5(EntityAI ai) throws LeekRunException {
		return getLaunchedEffects(ai);
	}

	public static ArrayLeekValue getLaunchedEffects_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getLaunchedEffects(ai, value);
	}

	public static ArrayLeekValue getPassiveEffects_v5(EntityAI ai) throws LeekRunException {
		return getPassiveEffects(ai);
	}

	public static ArrayLeekValue getPassiveEffects_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getPassiveEffects(ai, value);
	}

	public static String getAIName_v5(EntityAI ai) throws LeekRunException {
		return getAIName(ai);
	}

	public static String getAIName_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getAIName(ai, value);
	}

	public static String getAIPath_v5(EntityAI ai) throws LeekRunException {
		return getAIPath(ai);
	}

	public static String getAIPath_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getAIPath(ai, value);
	}

	public static String getTeamName_v5(EntityAI ai) throws LeekRunException {
		return getTeamName(ai);
	}

	public static String getTeamName_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getTeamName(ai, value);
	}

	public static String getCompositionName_v5(EntityAI ai) throws LeekRunException {
		return getCompositionName(ai);
	}

	public static String getCompositionName_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getCompositionName(ai, value);
	}

	public static String getFarmerName_v5(EntityAI ai) throws LeekRunException {
		return getFarmerName(ai);
	}

	public static String getFarmerName_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getFarmerName(ai, value);
	}

	public static String getFarmerCountry_v5(EntityAI ai) throws LeekRunException {
		return getFarmerCountry(ai);
	}

	public static String getFarmerCountry_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getFarmerCountry(ai, value);
	}

	public static long getFarmerID_v5(EntityAI ai) throws LeekRunException {
		return getFarmerID(ai);
	}

	public static Long getFarmerID_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getFarmerID(ai, value);
	}

	public static long getTeamID_v5(EntityAI ai) throws LeekRunException {
		return getTeamID(ai);
	}

	public static Long getTeamID_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getTeamID(ai, value);
	}

	public static ArrayLeekValue getSummons_v5(EntityAI ai) throws LeekRunException {
		return getSummons(ai);
	}

	public static ArrayLeekValue getSummons_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getSummons(ai, value);
	}

	public static long getType_v5(EntityAI ai) throws LeekRunException {
		return getType(ai);
	}

	public static Long getType_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getType(ai, value);
	}

	public static boolean isSummon_v5(EntityAI ai) throws LeekRunException {
		return isSummon(ai);
	}

	public static Boolean isSummon_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : isSummon(ai, value);
	}

	public static boolean isStatic_v5(EntityAI ai) throws LeekRunException {
		return isStatic(ai);
	}

	public static boolean isStatic_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? false : isStatic(ai, value);
	}

	public static long getBirthTurn_v5(EntityAI ai) throws LeekRunException {
		return getBirthTurn(ai);
	}

	public static Long getBirthTurn_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getBirthTurn(ai, value);
	}

	public static long getAwakeningZone_v5(EntityAI ai) throws LeekRunException {
		return getAwakeningZone(ai);
	}

	public static Long getAwakeningZone_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getAwakeningZone(ai, value);
	}

	public static ArrayLeekValue getPlantTriggers_v5(EntityAI ai) throws LeekRunException {
		return getPlantTriggers(ai);
	}

	public static ArrayLeekValue getPlantTriggers_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getPlantTriggers(ai, value);
	}

	public static long getLeekID_v5(EntityAI ai) throws LeekRunException {
		return getLeekID(ai);
	}

	public static Long getLeekID_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getLeekID(ai, value);
	}

	public static long getSide_v5(EntityAI ai) throws LeekRunException {
		return getSide(ai);
	}

	public static Long getSide_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getSide(ai, value);
	}

	public static long getTotalLife_v5(EntityAI ai) throws LeekRunException {
		return getTotalLife(ai);
	}

	public static Long getTotalLife_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getTotalLife(ai, value);
	}

	public static long getLevel_v5(EntityAI ai) throws LeekRunException {
		return getLevel(ai);
	}

	public static Long getLevel_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getLevel(ai, value);
	}

	public static long getEntityTurnOrder_v5(EntityAI ai) throws LeekRunException {
		return getEntityTurnOrder(ai);
	}

	public static Long getEntityTurnOrder_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getEntityTurnOrder(ai, value);
	}

	public static long getAIID_v5(EntityAI ai) throws LeekRunException {
		return getAIID(ai);
	}

	public static Long getAIID_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getAIID(ai, value);
	}

	public static long getStat_v5(EntityAI ai, long stat) {
		return getStat(ai, stat);
	}

	public static Long getStat_v5(EntityAI ai, Object entity, long stat) {
		return entity == null ? null : getStat(ai, entity, stat);
	}

	public static SetLeekValue getStates_v5(EntityAI ai) throws LeekRunException {
		return getStates(ai);
	}

	// getStates, isEnemy, isAlly, isAlive et isDead déclarent leur entité integer? en v5 : null
	// leur arrive tel quel au lieu de valoir 0, qui est une vraie entité
	public static SetLeekValue getStates_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : getStates(ai, ai.longint(value));
	}

	public static boolean isEnemy_v5(EntityAI ai, Object id) throws LeekRunException {
		return id != null && isEnemy(ai, ai.longint(id));
	}

	public static boolean isAlly_v5(EntityAI ai, Object id) throws LeekRunException {
		return id != null && isAlly(ai, ai.longint(id));
	}

	public static boolean isAlive_v5(EntityAI ai, Object id) throws LeekRunException {
		return id != null && isAlive(ai, ai.longint(id));
	}

	public static boolean isDead_v5(EntityAI ai, Object id) throws LeekRunException {
		return id != null && isDead(ai, ai.longint(id));
	}

	// ---- LeekScript 5 : null au lieu de -1 pour « aucun » ----

	public static Long getMobType_v5(EntityAI ai) throws LeekRunException {
		return V5.orNull(getMobType(ai));
	}

	public static Long getMobType_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : V5.orNull(getMobType(ai, value));
	}

	public static Long getChestType_v5(EntityAI ai) throws LeekRunException {
		return V5.orNull(getChestType(ai));
	}

	public static Long getChestType_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : V5.orNull(getChestType(ai, value));
	}

	public static Long getBulbType_v5(EntityAI ai) throws LeekRunException {
		return V5.orNull(getBulbType(ai));
	}

	public static Long getBulbType_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : V5.orNull(getBulbType(ai, value));
	}

	public static Long getPlantType_v5(EntityAI ai) throws LeekRunException {
		return V5.orNull(getPlantType(ai));
	}

	public static Long getPlantType_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : V5.orNull(getPlantType(ai, value));
	}

	public static Long getSummoner_v5(EntityAI ai) throws LeekRunException {
		return V5.orNull(getSummoner(ai));
	}

	public static Long getSummoner_v5(EntityAI ai, Object value) throws LeekRunException {
		return value == null ? null : V5.orNull(getSummoner(ai, value));
	}

	public static Long getPlantTrigger_v5(EntityAI ai) throws LeekRunException {
		return V5.orNull(getPlantTrigger(ai));
	}
}
