package com.leekwars.generator.classes;

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

import leekscript.AILog;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.SetLeekValue;

public class EntityClass {

	private static final int SAY_LENGTH_LIMIT = 100;

	public static long getLife(EntityAI ai) {
		return (long) ai.getEntity().getLife();
	}

	public static Long getLife(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getLife();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getLife();
		}
		return null;
	}

	public static long getForce(EntityAI ai) {
		return (long) ai.getEntity().getStrength();
	}

	public static Long getForce(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getStrength();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getStrength();
		}
		return null;
	}

	public static long getStrength(EntityAI ai) {
		return (long) ai.getEntity().getStrength();
	}

	public static Long getStrength(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getStrength();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getStrength();
		}
		return null;
	}

	public static long getWisdom(EntityAI ai) {
		return (long) ai.getEntity().getWisdom();
	}

	public static Long getWisdom(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getWisdom();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getWisdom();
		}
		return null;
	}

	public static long getResistance(EntityAI ai) {
		return (long) ai.getEntity().getResistance();
	}

	public static Long getResistance(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getResistance();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getResistance();
		}
		return null;
	}

	public static long getAgility(EntityAI ai) {
		return (long) ai.getEntity().getAgility();
	}

	public static Long getAgility(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getAgility();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getAgility();
		}
		return null;
	}

	public static long getScience(EntityAI ai) {
		return (long) ai.getEntity().getScience();
	}

	public static Long getScience(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getScience();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getScience();
		}
		return null;
	}

	public static long getMagic(EntityAI ai) {
		return (long) ai.getEntity().getMagic();
	}

	public static Long getMagic(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getMagic();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getMagic();
		}
		return null;
	}

	public static long getAbsoluteShield(EntityAI ai) {
		return (long) ai.getEntity().getAbsoluteShield();
	}

	public static Long getAbsoluteShield(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getAbsoluteShield();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getAbsoluteShield();
		}
		return null;
	}

	public static long getRelativeShield(EntityAI ai) {
		return (long) ai.getEntity().getRelativeShield();
	}

	public static Long getRelativeShield(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getRelativeShield();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getRelativeShield();
		}
		return null;
	}

	public static long getDamageReturn(EntityAI ai) {
		return (long) ai.getEntity().getDamageReturn();
	}

	public static Long getDamageReturn(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getDamageReturn();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getDamageReturn();
		}
		return null;
	}

	public static long getFrequency(EntityAI ai) {
		return (long) ai.getEntity().getFrequency();
	}

	public static Long getFrequency(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getFrequency();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getFrequency();
		}
		return null;
	}

	public static long getCores(EntityAI ai) {
		return ai.getEntity().getCores();
	}

	public static Long getCores(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getCores();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getCores();
		}
		return null;
	}

	public static long getRAM(EntityAI ai) {
		return ai.getEntity().getRAM();
	}

	public static Long getRAM(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getRAM();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getRAM();
		}
		return null;
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value == null) {
			if (ai.getEntity().getWeapon() != null)
				return (long) ai.getEntity().getWeapon().getId();
		}
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null && l.getWeapon() != null)
				return (long) l.getWeapon().getId();
		}
		return null;
	}

	public static String getName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getName();
	}

	public static String getName(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().getName();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return l.getName();
		}
		return null;
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
		if (value == null)
			return (long) ai.getEntity().getMP();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getMP();
		}
		return null;
	}

	public static long getTP(EntityAI ai) {
		return (long) ai.getEntity().getTP();
	}

	public static Long getTP(EntityAI ai, Object value) {
		if (value == null)
			return (long) ai.getEntity().getTP();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getTP();
		}
		return null;
	}

	public static long getTotalMP(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTotalMP();
	}

	public static Long getTotalMP(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getTotalMP();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null) {
				return (long) l.getTotalMP();
			}
		}
		return null;
	}

	public static long getTotalTP(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTotalTP();
	}

	public static Long getTotalTP(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getTotalTP();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null) {
				return (long) l.getTotalTP();
			}
		}
		return null;
	}

	public static long getPower(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getPower();
	}

	public static Long getPower(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			return (long) ai.getEntity().getPower();
		}
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getPower();
		}
		return null;
	}

	public static boolean setWeapon(EntityAI ai, long weapon_id) throws LeekRunException {

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
		if (ai.getEntity().getTP() < 1) {
			return null;
		}
		ai.getEntity().useTP(1);
		ai.getFight().log(new ActionLama());
		ai.getFight().getState().statistics.lama(ai.getEntity());
		return null;
	}

	public static LegacyArrayLeekValue listen_v1_3(EntityAI ai) throws LeekRunException {
		var values = new LegacyArrayLeekValue(ai);
		for (var say : ai.getSays()) {
			var s = ai.newArray(2);
			s.push(ai, say.id);
			s.push(ai, say.message);
			values.pushNoClone(ai, s);
		}
		return values;
	}

	public static ArrayLeekValue listen(EntityAI ai) throws LeekRunException {
		var values = new ArrayLeekValue(ai);
		for (var say : ai.getSays()) {
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
		Entity l = null;
		if (value == null)
			l = ai.getEntity();
		else if (value instanceof Number)
			l = ai.getFight().getEntity(((Number) value).intValue());
		if (l == null)
			return null;
		var retour = new LegacyArrayLeekValue(ai);
		for (var weapon : l.getWeapons()) {
			retour.push(ai, (long) weapon.getId());
		}
		return retour;
	}

	public static ArrayLeekValue getWeapons(EntityAI ai, Object value) throws LeekRunException {
		Entity l = null;
		if (value == null)
			l = ai.getEntity();
		else if (value instanceof Number)
			l = ai.getFight().getEntity(((Number) value).intValue());
		if (l == null)
			return null;
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
		Entity l = null;
		if (value == null)
			l = ai.getEntity();
		else if (value instanceof Number)
			l = ai.getFight().getEntity(((Number) value).intValue());
		if (l == null)
			return null;
		var result = new ArrayLeekValue(ai);
		for (var chip : l.getChips()) {
			result.push(ai, (long) chip.getId());
		}
		return result;
	}


	public static LegacyArrayLeekValue getChips_v1_3(EntityAI ai, Object value) throws LeekRunException {
		Entity l = null;
		if (value == null)
			l = ai.getEntity();
		else if (value instanceof Number)
			l = ai.getFight().getEntity(((Number) value).intValue());
		if (l == null)
			return null;
		var result = new LegacyArrayLeekValue(ai);
		for (var chip : l.getChips()) {
			result.push(ai, (long) chip.getId());
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
		Entity l = null;
		if (value == null) {
			l = ai.getEntity();
		} else if (value instanceof Number) {
			l = ai.getFight().getEntity(ai.integer(value));
		}
		if (l == null) {
			return null;
		}
		var retour = new ArrayLeekValue(ai, l.getEffects().size());
		for (Effect effect : l.getEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}

	public static LegacyArrayLeekValue getEffects_v1_3(EntityAI ai, Object value) throws LeekRunException {
		Entity l = null;
		if (value == null) {
			l = ai.getEntity();
		} else if (value instanceof Number) {
			l = ai.getFight().getEntity(ai.integer(value));
		}
		if (l == null) {
			return null;
		}
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
		Entity l = null;
		if (value == null) {
			l = ai.getEntity();
		} else if (value instanceof Number) {
			l = ai.getFight().getEntity(ai.integer(value));
		}
		if (l == null) {
			return null;
		}
		var retour = new ArrayLeekValue(ai, l.getLaunchedEffects().size());
		for (var effect : l.getLaunchedEffects()) {
			retour.pushNoClone(ai, ai.getEffectArray(effect));
		}
		return retour;
	}


	public static LegacyArrayLeekValue getLaunchedEffects_v1_3(EntityAI ai, Object value) throws LeekRunException {
		Entity l = null;
		if (value == null) {
			l = ai.getEntity();
		} else if (value instanceof Number) {
			l = ai.getFight().getEntity(ai.integer(value));
		}
		if (l == null) {
			return null;
		}
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
		Entity l = null;
		if (value == null) {
			l = ai.getEntity();
		} else if (value instanceof Number) {
			l = ai.getFight().getEntity(ai.integer(value));
		}
		if (l == null) {
			return null;
		}
		var retour = new ArrayLeekValue(ai);
		for (var feature : l.getPassiveEffects()) {
			retour.pushNoClone(ai, ai.getFeatureArray(feature));
		}
		return retour;
	}

	public static LegacyArrayLeekValue getPassiveEffects_v1_3(EntityAI ai, Object value) throws LeekRunException {
		Entity l = null;
		if (value == null) {
			l = ai.getEntity();
		} else if (value instanceof Number) {
			l = ai.getFight().getEntity(ai.integer(value));
		}
		if (l == null) {
			return null;
		}
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
		if (value == null) {
			return ai.getEntity().getAIName();
		}
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(ai.integer(value));
			if (l != null)
				return l.getAIName();
		}
		return null;
	}

	public static String getTeamName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getTeamName();
	}

	public static String getTeamName(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().getTeamName();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(ai.integer(value));
			if (l != null && l.getTeamName() != null)
				return l.getTeamName();
		}
		return null;
	}

	public static String getFarmerName(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getFarmerName();
	}

	public static String getFarmerName(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().getFarmerName();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(ai.integer(value));
			if (l != null)
				return l.getFarmerName();
		}
		return null;
	}

	public static String getFarmerCountry(EntityAI ai) throws LeekRunException {
		return ai.getEntity().getFarmerCountry();
	}

	public static String getFarmerCountry(EntityAI ai, Object value) throws LeekRunException {
		if (value == null) {
			return ai.getEntity().getFarmerCountry();
		}
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return l.getFarmerCountry();
		}
		return null;
	}

	public static long getFarmerID(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getFarmer();
	}

	public static Long getFarmerID(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getFarmer();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getFarmer();
		}
		return null;
	}

	public static long getTeamID(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTeamId();
	}

	public static Long getTeamID(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getTeamId();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		else if (value instanceof Number)
			l = ai.getFight().getEntity(((Number) value).intValue());
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
		else if (value instanceof Number)
			l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null) {
				return (long) l.getType() + 1;
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getBirthTurn();
		}
		return null;
	}

	public static long getSummoner(EntityAI ai) throws LeekRunException {
		return ai.getEntity().isSummon() ? (long) ai.getEntity().getSummoner().getFId(): -1l;
	}

	public static Long getSummoner(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return ai.getEntity().isSummon() ? (long) ai.getEntity().getSummoner().getFId(): -1l;
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getTeam();
		}
		return null;
	}

	public static long getTotalLife(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getTotalLife();
	}

	public static Long getTotalLife(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getTotalLife();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) l.getTotalLife();
		}
		return null;
	}

	public static long getLevel(EntityAI ai) throws LeekRunException {
		return (long) ai.getEntity().getLevel();
	}

	public static Long getLevel(EntityAI ai, Object value) throws LeekRunException {
		if (value == null)
			return (long) ai.getEntity().getLevel();
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
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
		if (value instanceof Number) {
			var l = ai.getFight().getEntity(((Number) value).intValue());
			if (l != null)
				return (long) ((EntityAI) l.getAI()).getId();
		}
		return null;
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
			retour.add((long) state.ordinal());
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
			retour.add((long) state.ordinal());
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
}
