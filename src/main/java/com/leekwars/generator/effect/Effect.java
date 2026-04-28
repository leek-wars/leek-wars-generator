package com.leekwars.generator.effect;

import java.util.function.Supplier;

import com.leekwars.generator.action.ActionAddEffect;
import com.leekwars.generator.action.ActionStackEffect;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.items.Item;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;
import com.leekwars.generator.state.Stats;

public abstract class Effect implements Cloneable {

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
	public final static int TYPE_KILL_TO_TP = 56;
	public final static int TYPE_RAW_HEAL = 57;
	public final static int TYPE_CRITICAL_TO_HEAL = 58;
	public final static int TYPE_ADD_STATE = 59;
	public final static int TYPE_TOTAL_DEBUFF = 60;
	public final static int TYPE_STEAL_LIFE = 61;
	public final static int TYPE_MULTIPLY_STATS = 62;

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

	// Erosion rates
	public static final double EROSION_DAMAGE = 0.05;
	public static final double EROSION_POISON = 0.10;
	public static final double EROSION_CRITICAL_BONUS = 0.10;

	// Effect factories indexed by effect id (1-based). Used by createEffect to
	// avoid getDeclaredConstructor().newInstance() reflection on every buff/debuff,
	// which the JIT can't easily inline.
	@SuppressWarnings("unchecked")
	public final static Supplier<Effect>[] effects = new Supplier[] {
		(Supplier<Effect>) EffectDamage::new, // 1
		(Supplier<Effect>) EffectHeal::new, // 2
		(Supplier<Effect>) EffectBuffStrength::new, // 3
		(Supplier<Effect>) EffectBuffAgility::new, // 4
		(Supplier<Effect>) EffectRelativeShield::new, // 5
		(Supplier<Effect>) EffectAbsoluteShield::new, // 6
		(Supplier<Effect>) EffectBuffMP::new, // 7
		(Supplier<Effect>) EffectBuffTP::new, // 8
		(Supplier<Effect>) EffectDebuff::new, // 9
		(Supplier<Effect>) EffectTeleport::new, // 10
		(Supplier<Effect>) EffectPermutation::new, // 11
		(Supplier<Effect>) EffectVitality::new, // 12
		(Supplier<Effect>) EffectPoison::new, // 13
		(Supplier<Effect>) EffectSummon::new, // 14
		(Supplier<Effect>) EffectResurrect::new, // 15
		(Supplier<Effect>) EffectKill::new, // 16
		(Supplier<Effect>) EffectShackleMP::new, // 17
		(Supplier<Effect>) EffectShackleTP::new, // 18
		(Supplier<Effect>) EffectShackleStrength::new, // 19
		(Supplier<Effect>) EffectDamageReturn::new, // 20
		(Supplier<Effect>) EffectBuffResistance::new, // 21
		(Supplier<Effect>) EffectBuffWisdom::new, // 22
		(Supplier<Effect>) EffectAntidote::new, // 23
		(Supplier<Effect>) EffectShackleMagic::new, // 24
		(Supplier<Effect>) EffectAftereffect::new, // 25
		(Supplier<Effect>) EffectVulnerability::new, // 26
		(Supplier<Effect>) EffectAbsoluteVulnerability::new, // 27
		(Supplier<Effect>) EffectLifeDamage::new, // 28
		(Supplier<Effect>) EffectStealAbsoluteShield::new, // 29
		(Supplier<Effect>) EffectNovaDamage::new, // 30
		(Supplier<Effect>) EffectRawBuffMP::new, // 31
		(Supplier<Effect>) EffectRawBuffTP::new, // 32
		null, // 33
		null, // 34
		null, // 35
		null, // 36
		(Supplier<Effect>) EffectRawAbsoluteShield::new, // 37
		(Supplier<Effect>) EffectRawBuffStrength::new, // 38
		(Supplier<Effect>) EffectRawBuffMagic::new, // 39
		(Supplier<Effect>) EffectRawBuffScience::new, // 40
		(Supplier<Effect>) EffectRawBuffAgility::new, // 41
		(Supplier<Effect>) EffectRawBuffResistance::new, // 42
		null, // 43
		(Supplier<Effect>) EffectRawBuffWisdom::new, // 44
		(Supplier<Effect>) EffectNovaVitality::new, // 45
		(Supplier<Effect>) EffectAttract::new, // 46
		(Supplier<Effect>) EffectShackleAgility::new, // 47
		(Supplier<Effect>) EffectShackleWisdom::new, // 48
		(Supplier<Effect>) EffectRemoveShackles::new, // 49
		null, // 50
		(Supplier<Effect>) EffectPush::new, // 51
		(Supplier<Effect>) EffectRawBuffPower::new, // 52
		(Supplier<Effect>) EffectRepel::new, // 53
		(Supplier<Effect>) EffectRawRelativeShield::new, // 54
		null, // 55
		null, // 56
		(Supplier<Effect>) EffectRawHeal::new, // 57
		null, // 58
		(Supplier<Effect>) EffectAddState::new, // 59
		(Supplier<Effect>) EffectTotalDebuff::new, // 60
		(Supplier<Effect>) EffectStealLife::new, // 61
		(Supplier<Effect>) EffectMultiplyStats::new, // 62
	};

	// Effect characteristics
	private int id;
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
	protected EntityState state;

	public static int createEffect(State state, int id, int turns, double aoe, double value1, double value2, boolean critical, Entity target, Entity caster, Attack attack, double jet, boolean stackable, int previousEffectTotalValue, int targetCount, int propagate, int modifiers) {

		// Invalid effect id (effect IDs are 1-based — id 0 would index effects[-1])
		if (id <= 0 || id > effects.length) {
			return 0;
		}
		Supplier<Effect> factory = effects[id - 1];
		if (factory == null) {
			return 0;
		}
		// Defensive try/catch matches the previous reflection-based code: a buggy
		// Effect constructor must not propagate out of createEffect into the action loop.
		Effect effect;
		try {
			effect = factory.get();
		} catch (Exception e) {
			return 0;
		}
		effect.setId(id);
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
		effect.erosionRate = id == TYPE_POISON ? EROSION_POISON : EROSION_DAMAGE;
		if (critical) effect.erosionRate += EROSION_CRITICAL_BONUS;
		effect.previousEffectTotalValue = previousEffectTotalValue;
		effect.targetCount = targetCount;
		effect.propagate = propagate;
		effect.modifiers = modifiers;

		// Remove previous effect of the same type (that is not stackable)
		if (effect.getTurns() != 0) {
			if (!stackable) {
				var effects = target.getEffects();
				for (int i = 0; i < effects.size(); ++i) {
					var e = effects.get(i);
					if (e.getId() == id && (e.attack == null ? attack == null : attack != null && e.attack.getItemId() == attack.getItemId())) {
						e.getCaster().removeLaunchedEffect(e);
						target.removeEffect(e);
						break;
					}
				}
			}
		}
		// Compute the effect
		effect.apply(state);

		// Stack to previous item with the same characteristics
		if (effect.value > 0) {
			for (var e : target.getEffects()) {
				if ((e.attack == null ? attack == null : attack != null && e.attack.getItemId() == attack.getItemId()) && e.getId() == id && e.turns == turns && e.caster == caster) {
					e.mergeWith(effect);
					state.getActions().log(new ActionStackEffect(e.getLogID(), effect.value));
					return effect.value; // No need to apply the effect again
				}
			}
		}

		// Add effect to the target and the caster
		if (effect.getTurns() != 0 && effect.value > 0) {
			target.addEffect(effect);
			caster.addLaunchedEffect(effect);
			effect.addLog(state);
			state.statistics.effect(target, caster, effect);
		}
		return effect.value;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void addLog(State state) {
		if (turns == 0) {
			return;
		}
		logID = ActionAddEffect.createEffect(state.getActions(), attack == null ? Attack.TYPE_CHIP : attack.getType(), attack == null ? 0 : attack.getItemId(), caster, target, getId(), value, turns, modifiers);
	}

	public Stats getStats() {
		return stats;
	}

	public int getID() {
		return getId();
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

	public void reduce(double percent, Entity caster) {
		double reduction = Math.max(0.0, 1.0 - percent);
		value = (int) Math.round((double) value * reduction);
		for (var stat : stats.stats.entrySet()) {
			// abs(round(v * r)) * sign(v) pour l'arrondi si r = 0.5
			int newValue = (int) (Math.round((double) Math.abs(stat.getValue()) * reduction) * Math.signum(stat.getValue()));
			int delta = newValue - stat.getValue();
			stats.updateStat(stat.getKey(), delta);
			target.updateBuffStats(stat.getKey(), delta, caster);
		}
	}

	public void mergeWith(Effect effect) {
		value += effect.value;
		for (var stat : stats.stats.entrySet()) {
			int signum = stat.getValue() > 0 ? 1 : -1;
			stats.updateStat(stat.getKey(), effect.value * signum);
		}
	}

	// Abstract methods
	public void apply(State state) {}

	public void applyStartTurn(State state) {}

	public static int getEffectStat(int type) {
		switch (type) {
			case TYPE_DAMAGE:
				return Entity.STAT_STRENGTH;
			case TYPE_POISON:
			case TYPE_SHACKLE_MAGIC:
			case TYPE_SHACKLE_STRENGTH:
			case TYPE_SHACKLE_MP:
			case TYPE_SHACKLE_TP:
				return Entity.STAT_MAGIC;
			case TYPE_LIFE_DAMAGE:
				return Entity.STAT_LIFE;
			case TYPE_NOVA_DAMAGE:
			case TYPE_BUFF_AGILITY:
			case TYPE_BUFF_STRENGTH:
			case TYPE_BUFF_MP:
			case TYPE_BUFF_TP:
			case TYPE_BUFF_RESISTANCE:
			case TYPE_BUFF_WISDOM:
				return Entity.STAT_SCIENCE;
			case TYPE_DAMAGE_RETURN:
				return Entity.STAT_AGILITY;
			case TYPE_HEAL:
			case TYPE_VITALITY:
				return Entity.STAT_WISDOM;
			case TYPE_RELATIVE_SHIELD:
			case TYPE_ABSOLUTE_SHIELD:
				return Entity.STAT_RESISTANCE;
		}
		return -1;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setTarget(Entity entity) {
		this.target = entity;
	}

	public void setCaster(Entity entity) {
		this.caster = entity;
	}

	public Item getItem() {
		return this.attack != null ? this.attack.getItem() : null;
	}

	public EntityState getState() {
		return state;
	}
}
