package com.leekwars.generator.attack.effect;

import java.util.List;
import java.util.Map;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionAddEffect;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.leek.Stats;

public abstract class Effect {

	// Effect type constants
	public final static int TYPE_DAMAGE = 1;
	public final static int TYPE_HEAL = 2;
	public final static int TYPE_BUFF_STRENGTH = 3;
	public final static int TYPE_BUFF_AGILITY = 4;
	public final static int TYPE_RELATIVE_SHIELD = 5;
	public final static int TYPE_ABSOLUTE_SHIELD = 6;
	public final static int TYPE_BUFF_MP = 7;
	public final static int TYPE_BUFF_TP = 8;
	public final static int TYPE_DEBUFF = 9;
	public final static int TYPE_TELEPORT = 10;
	public final static int TYPE_PERMUTATION = 11;
	public final static int TYPE_VITALITY = 12;
	public final static int TYPE_POISON = 13;
	public final static int TYPE_SUMMON = 14;
	public final static int TYPE_RESURRECT = 15;
	public final static int TYPE_KILL = 16;
	public final static int TYPE_SHACKLE_MP = 17;
	public final static int TYPE_SHACKLE_TP = 18;
	public final static int TYPE_SHACKLE_STRENGTH = 19;
	public final static int TYPE_DAMAGE_RETURN = 20;
	public final static int TYPE_BUFF_RESISTANCE = 21;
	public final static int TYPE_BUFF_WISDOM = 22;
	public final static int TYPE_ANTIDOTE = 23;
	public final static int TYPE_SHACKLE_MAGIC = 24;
	public final static int TYPE_AFTEREFFECT = 25;
	public final static int TYPE_VULNERABILITY = 26;
	public final static int TYPE_ABSOLUTE_VULNERABILITY = 27;
	public final static int TYPE_LIFE_DAMAGE = 28;
	public final static int TYPE_STEAL_ABSOLUTE_SHIELD = 29;
	public final static int TYPE_NOVA_DAMAGE = 30;
	public final static int TYPE_RAW_BUFF_MP = 31;
	public final static int TYPE_RAW_BUFF_TP = 32;
	public final static int TYPE_POISON_TO_SCIENCE = 33;
	public final static int TYPE_DAMAGE_TO_ABSOLUTE_SHIELD = 34;
	public final static int TYPE_DAMAGE_TO_STRENGTH = 35;
	public final static int TYPE_NOVA_DAMAGE_TO_MAGIC = 36;
	public final static int TYPE_RAW_ABSOLUTE_SHIELD = 37;
	public final static int TYPE_RAW_BUFF_STRENGTH = 38;
	public final static int TYPE_RAW_BUFF_MAGIC = 39;
	public final static int TYPE_RAW_BUFF_SCIENCE = 40;
	public final static int TYPE_RAW_BUFF_AGILITY = 41;
	public final static int TYPE_RAW_BUFF_RESISTANCE = 42;
	public final static int TYPE_PROPAGATION = 43;
	public final static int TYPE_RAW_BUFF_WISDOM = 44;
	public final static int TYPE_NOVA_VITALITY = 45;
	public final static int TYPE_ATTRACT = 46;
	public final static int TYPE_SHACKLE_AGILITY = 47;
	public final static int TYPE_SHACKLE_WISDOM = 48;
	public final static int TYPE_REMOVE_SHACKLES = 49;
	public final static int TYPE_MOVED_TO_MP = 50;
	public final static int TYPE_PUSH = 51;
	public final static int TYPE_RAW_BUFF_POWER = 52;
	public final static int TYPE_REPEL = 53;
	public final static int TYPE_RAW_RELATIVE_SHIELD = 54;
	public final static int TYPE_ALLY_KILLED_TO_AGILITY = 55;

	// Target filters constants
	public final static int TARGET_ENEMIES = 1; // Enemies
	public final static int TARGET_ALLIES = 2; // Allies
	public final static int TARGET_CASTER = 4; // Caster
	public final static int TARGET_NON_SUMMONS = 8; // Non-summons
	public final static int TARGET_SUMMONS = 16; // Summons

	// Modifiers
	public final static int MODIFIER_STACKABLE = 1; // The effect is stackable
	public final static int MODIFIER_MULTIPLIED_BY_TARGETS = 2; // The effect is multiplied by the number of targets in the area
	public final static int MODIFIER_ON_CASTER = 4; // The effect is applied on the caster
	public final static int MODIFIER_NOT_REPLACEABLE = 8; // The effect will not replace the previous one
	public final static int MODIFIER_IRREDUCTIBLE = 16; // The effect is not reductible (by EFFECT_DEBUFF)

	// Power in case of critical hit
	public static final double CRITICAL_FACTOR = 1.3;

	// Array of effect classes
	public final static Class<?>[] effects = {
		EffectDamage.class, // 1
		EffectHeal.class, // 2
		EffectBuffStrength.class, // 3
		EffectBuffAgility.class, // 4
		EffectRelativeShield.class, // 5
		EffectAbsoluteShield.class, // 6
		EffectBuffMP.class, // 7
		EffectBuffTP.class, // 8
		EffectDebuff.class, // 9
		EffectTeleport.class, // 10
		EffectPermutation.class, // 11
		EffectVitality.class, // 12
		EffectPoison.class, // 13
		EffectSummon.class, // 14
		EffectResurrect.class, // 15
		EffectKill.class, // 16
		EffectShackleMP.class, // 17
		EffectShackleTP.class, // 18
		EffectShackleStrength.class, // 19
		EffectDamageReturn.class, // 20
		EffectBuffResistance.class, // 21
		EffectBuffWisdom.class, // 22
		EffectAntidote.class, // 23
		EffectShackleMagic.class, // 24
		EffectAftereffect.class, // 25
		EffectVulnerability.class, // 26
		EffectAbsoluteVulnerability.class, // 27
		EffectLifeDamage.class, // 28
		EffectStealAbsoluteShield.class, // 29
		EffectNovaDamage.class, // 30
		EffectRawBuffMP.class, // 31
		EffectRawBuffTP.class, // 32
		null, // 33
		null, // 34
		null, // 35
		null, // 36
		EffectRawAbsoluteShield.class, // 37
		EffectRawBuffStrength.class, // 38
		EffectRawBuffMagic.class, // 39
		EffectRawBuffScience.class, // 40
		EffectRawBuffAgility.class, // 41
		EffectRawBuffResistance.class, // 42
		null, // 43
		EffectRawBuffWisdom.class, // 44
		EffectNovaVitality.class, // 45
		EffectAttract.class, // 46
		EffectShackleAgility.class, // 47
		EffectShackleWisdom.class, // 48
		EffectRemoveShackles.class, // 49
		null, // 50
		EffectPush.class, // 51
		EffectRawBuffPower.class, // 52
		EffectRepel.class, // 53
		EffectRawRelativeShield.class, // 54
		null, // 55
	};

	// Effect characteristics
	protected int id;
	protected int turns = 0;
	protected double aoe = 1.0;
	protected double value1;
	protected double value2;
	protected boolean critical = false;
	protected double criticalPower = 1.0;
	protected Entity caster;
	protected Entity target;
	protected Attack attack;
	protected double jet;
	protected Stats stats = new Stats();
	protected int logID = 0;
	protected double erosionRate;
	public int value = 0;
	public int previousEffectTotalValue;
	public int targetCount;
	public int propagate = 0; // Distance de propagation
	public int modifiers = 0;

	public static int createEffect(Fight fight, int id, int turns, double aoe, double value1, double value2, boolean critical, Entity target, Entity caster, Attack attack, double jet, boolean stackable, int previousEffectTotalValue, int targetCount, int propagate, int modifiers) {

		// Invalid effect id
		if (id < 0 || id > effects.length) {
			return 0;
		}

		// Create the effect
		Effect effect;
		try {
			effect = (Effect) effects[id - 1].getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			return 0;
		}
		effect.id = id;
		effect.turns = turns;
		effect.aoe = aoe;
		effect.value1 = value1;
		effect.value2 = value2;
		effect.critical = critical;
		effect.criticalPower = critical ? CRITICAL_FACTOR : 1.0;
		effect.caster = caster;
		effect.target = target;
		effect.attack = attack;
		effect.jet = jet;
		effect.erosionRate = id == TYPE_POISON ? 0.10 : 0.05;
		if (critical) effect.erosionRate += 0.10;
		effect.previousEffectTotalValue = previousEffectTotalValue;
		effect.targetCount = targetCount;
		effect.propagate = propagate;
		effect.modifiers = modifiers;

		// Remove previous effect of the same type (that is not stackable)
		if (effect.getTurns() != 0) {
			if (!stackable) {
				List<Effect> effects = target.getEffects();
				for (int i = 0; i < effects.size(); ++i) {
					Effect e = effects.get(i);
					if (e.id == id && (e.attack == null ? attack == null : attack != null && e.attack.getItemId() == attack.getItemId())) {
						target.removeEffect(e);
						break;
					}
				}
			}
		}
		// Compute the effect
		effect.apply(fight);

		// Stack to previous item with the same characteristics
		if (effect.value > 0) {
			for (Effect e : target.getEffects()) {
				if ((e.attack == null ? attack == null : attack != null && e.attack.getItemId() == attack.getItemId()) && e.id == id && e.turns == turns && e.caster == caster) {
					e.mergeWith(effect);
					effect.addLog(fight, true);
					return effect.value; // No need to apply the effect again
				}
			}
		}

		// Add effect to the target and the caster
		if (effect.getTurns() != 0 && effect.value > 0) {
			target.addEffect(effect);
			caster.addLaunchedEffect(effect);
			effect.addLog(fight, false);
			fight.statistics.effect(target, caster, effect);
		}
		return effect.value;
	}

	public void addLog(Fight fight, boolean stacked) {
		if (turns == 0) {
			return;
		}
		logID = ActionAddEffect.createEffect(fight.getActions(), attack == null ? Attack.TYPE_CHIP : attack.getType(), attack == null ? 0 : attack.getItemId(), caster, target, id, value, turns, stacked, modifiers);
	}

	public Stats getStats() {
		return stats;
	}

	public int getID() {
		return id;
	}

	public int getLogID() {
		return logID;
	}

	public boolean isCritical() {
		return critical;
	}

	public int getTurns() {
		return turns;
	}

	public void setTurns(int turns) {
		this.turns = turns;
	}

	public double getAOE() {
		return aoe;
	}

	public int getValue() {
		return value;
	}

	public double getValue1() {
		return value1;
	}

	public double getValue2() {
		return value2;
	}

	public Entity getCaster() {
		return caster;
	}

	public Entity getTarget() {
		return target;
	}

	public Attack getAttack() {
		return attack;
	}

	public int getModifiers() {
		return modifiers;
	}

	public ArrayLeekValue getLeekValue(AI ai) throws LeekRunException {

		ArrayLeekValue retour = new ArrayLeekValue();
		retour.put(ai, 0, id);
		retour.put(ai, 1, value);
		retour.put(ai, 2, caster.getFId());
		retour.put(ai, 3, turns);
		retour.put(ai, 4, critical);
		retour.put(ai, 5, attack == null ? 0 : attack.getItemId());
		retour.put(ai, 6, target.getFId());
		retour.put(ai, 7, modifiers);
		return retour;
	}

	public void reduce(double percent, Entity caster) {
		double reduction = 1 - percent;
		value = (int) Math.round((double) value * reduction);
		for (Map.Entry<Integer, Integer> stat : stats.stats.entrySet()) {
			int newValue = (int) Math.round((double) stat.getValue() * reduction);
			int delta = newValue - stat.getValue();
			stats.updateStat(stat.getKey(), delta);
			target.updateBuffStats(stat.getKey(), delta, caster);
		}
	}

	public void mergeWith(Effect effect) {
		value += effect.value;
		for (Map.Entry<Integer, Integer> stat : stats.stats.entrySet()) {
			int signum = stat.getValue() > 0 ? 1 : -1;
			stats.updateStat(stat.getKey(), effect.value * signum);
		}
	}

	// Abstract methods
	public void apply(Fight fight) {}

	public void applyStartTurn(Fight fight) {}

	public static int getEffectCharacteristic(int type) {
		switch (type) {
			case TYPE_DAMAGE:
				return Entity.CHARAC_STRENGTH;
			case TYPE_POISON:
			case TYPE_SHACKLE_MAGIC:
			case TYPE_SHACKLE_STRENGTH:
			case TYPE_SHACKLE_MP:
			case TYPE_SHACKLE_TP:
				return Entity.CHARAC_MAGIC;
			case TYPE_LIFE_DAMAGE:
				return Entity.CHARAC_LIFE;
			case TYPE_NOVA_DAMAGE:
			case TYPE_BUFF_AGILITY:
			case TYPE_BUFF_STRENGTH:
			case TYPE_BUFF_MP:
			case TYPE_BUFF_TP:
			case TYPE_BUFF_RESISTANCE:
			case TYPE_BUFF_WISDOM:
				return Entity.CHARAC_SCIENCE;
			case TYPE_DAMAGE_RETURN:
				return Entity.CHARAC_AGILITY;
			case TYPE_HEAL:
			case TYPE_VITALITY:
				return Entity.CHARAC_WISDOM;
			case TYPE_RELATIVE_SHIELD:
			case TYPE_ABSOLUTE_SHIELD:
				return Entity.CHARAC_RESISTANCE;
		}
		return -1;
	}
}
