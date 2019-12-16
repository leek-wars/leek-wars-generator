package com.leekwars.generator;

import com.leekwars.generator.attack.effect.Effect;
import com.leekwars.generator.fight.Fight;

import leekscript.runner.ILeekConstant;
import leekscript.runner.LeekFunctions;

public enum FightConstants implements ILeekConstant {

	MAX_TURNS(Fight.MAX_TURNS, LeekFunctions.INT),

	CELL_EMPTY(0, LeekFunctions.INT),
	CELL_PLAYER(1, LeekFunctions.INT),
	CELL_OBSTACLE(2, LeekFunctions.INT),

	// Type entity
	ENTITY_LEEK(1, LeekFunctions.INT),
	ENTITY_BULB(2, LeekFunctions.INT),
	ENTITY_TURRET(3, LeekFunctions.INT),

	// Constants
	EFFECT_TARGET_ALLIES(Effect.TARGET_ALLIES, LeekFunctions.INT),
	EFFECT_TARGET_ENEMIES(Effect.TARGET_ENEMIES, LeekFunctions.INT),
	EFFECT_TARGET_CASTER(Effect.TARGET_CASTER, LeekFunctions.INT),
	EFFECT_TARGET_NOT_CASTER(0, LeekFunctions.INT),
	EFFECT_TARGET_NON_SUMMONS(Effect.TARGET_NON_SUMMONS, LeekFunctions.INT),
	EFFECT_TARGET_SUMMONS(Effect.TARGET_SUMMONS, LeekFunctions.INT),
	EFFECT_TARGET_ALWAYS_CASTER(Effect.TARGET_ALWAYS_CASTER, LeekFunctions.INT),

	EFFECT_DAMAGE(1, LeekFunctions.INT),
	EFFECT_HEAL(2, LeekFunctions.INT),
	EFFECT_FORCE(3, LeekFunctions.INT),
	EFFECT_AGILITY(4, LeekFunctions.INT),
	EFFECT_RELATIVE_SHIELD(5, LeekFunctions.INT),
	EFFECT_ABSOLUTE_SHIELD(6, LeekFunctions.INT),
	EFFECT_MP(7, LeekFunctions.INT),
	EFFECT_TP(8, LeekFunctions.INT),
	EFFECT_TELEPORT(10, LeekFunctions.INT),
	EFFECT_INVERT(Effect.TYPE_PERMUTATION, LeekFunctions.INT),
	EFFECT_RESURRECT(Effect.TYPE_RESURRECT, LeekFunctions.INT),

	EFFECT_BUFF_DAMAGE(1, LeekFunctions.INT),
	EFFECT_BUFF_HEAL(2, LeekFunctions.INT),
	EFFECT_BUFF_FORCE(3, LeekFunctions.INT),
	EFFECT_BUFF_STRENGTH(3, LeekFunctions.INT),
	EFFECT_BUFF_AGILITY(4, LeekFunctions.INT),
	EFFECT_BUFF_RELATIVE_SHIELD(5, LeekFunctions.INT),
	EFFECT_BUFF_ABSOLUTE_SHIELD(6, LeekFunctions.INT),
	EFFECT_BUFF_MP(7, LeekFunctions.INT),
	EFFECT_BUFF_TP(8, LeekFunctions.INT),
	EFFECT_BUFF_DEBUFF(9, LeekFunctions.INT),
	EFFECT_BUFF_TELEPORTATION(10, LeekFunctions.INT),
	EFFECT_BOOST_MAX_LIFE(Effect.TYPE_VITALITY, LeekFunctions.INT),
	EFFECT_POISON(Effect.TYPE_POISON, LeekFunctions.INT),
	EFFECT_SUMMON(Effect.TYPE_SUMMON, LeekFunctions.INT),
	EFFECT_DEBUFF(Effect.TYPE_DEBUFF, LeekFunctions.INT),
	EFFECT_KILL(Effect.TYPE_KILL, LeekFunctions.INT),
	EFFECT_SHACKLE_MP(Effect.TYPE_SHACKLE_MP, LeekFunctions.INT),
	EFFECT_SHACKLE_TP(Effect.TYPE_SHACKLE_TP, LeekFunctions.INT),
	EFFECT_SHACKLE_STRENGTH(Effect.TYPE_SHACKLE_STRENGTH, LeekFunctions.INT),
	EFFECT_DAMAGE_RETURN(Effect.TYPE_DAMAGE_RETURN, LeekFunctions.INT),
	EFFECT_BUFF_RESISTANCE(Effect.TYPE_BUFF_RESISTANCE, LeekFunctions.INT),
	EFFECT_BUFF_WISDOM(Effect.TYPE_BUFF_WISDOM, LeekFunctions.INT),
	EFFECT_SHACKLE_MAGIC(Effect.TYPE_SHACKLE_MAGIC, LeekFunctions.INT),
	EFFECT_ANTIDOTE(Effect.TYPE_ANTIDOTE, LeekFunctions.INT),
	EFFECT_AFTEREFFECT(Effect.TYPE_AFTEREFFECT, LeekFunctions.INT),
	EFFECT_VULNERABILITY(Effect.TYPE_VULNERABILITY, LeekFunctions.INT),
	EFFECT_ABSOLUTE_VULNERABILITY(Effect.TYPE_ABSOLUTE_VULNERABILITY, LeekFunctions.INT),
	EFFECT_LIFE_DAMAGE(Effect.TYPE_LIFE_DAMAGE, LeekFunctions.INT),

	// Résultats attaque
	USE_CRITICAL(2, LeekFunctions.INT),
	USE_SUCCESS(1, LeekFunctions.INT),
	USE_FAILED(0, LeekFunctions.INT),
	USE_INVALID_TARGET(-1, LeekFunctions.INT),
	USE_NOT_ENOUGH_TP(-2, LeekFunctions.INT),
	USE_INVALID_COOLDOWN(-3, LeekFunctions.INT),
	USE_INVALID_POSITION(-4, LeekFunctions.INT),
	USE_TOO_MUCH_SUMMONS(-5, LeekFunctions.INT),
	USE_TOO_MANY_SUMMONS(-5, LeekFunctions.INT),
	USE_RESURRECT_INVALID_ENTIITY(-6, LeekFunctions.INT),

	// Armes
	WEAPON_PISTOL(37, LeekFunctions.INT),
	WEAPON_MACHINE_GUN(38, LeekFunctions.INT),
	WEAPON_DOUBLE_GUN(39, LeekFunctions.INT),
	WEAPON_SHOTGUN(41, LeekFunctions.INT),
	WEAPON_MAGNUM(45, LeekFunctions.INT),
	WEAPON_LASER(42, LeekFunctions.INT),
	WEAPON_GRENADE_LAUNCHER(43, LeekFunctions.INT),
	WEAPON_FLAME_THROWER(46, LeekFunctions.INT),
	WEAPON_DESTROYER(40, LeekFunctions.INT),
	WEAPON_GAZOR(48, LeekFunctions.INT),
	WEAPON_ELECTRISOR(44, LeekFunctions.INT),
	WEAPON_M_LASER(47, LeekFunctions.INT),
	WEAPON_B_LASER(60, LeekFunctions.INT),
	WEAPON_KATANA(107, LeekFunctions.INT),
	WEAPON_BROADSWORD(108, LeekFunctions.INT),
	WEAPON_AXE(109, LeekFunctions.INT),

	// Messages
	MESSAGE_HEAL(1, LeekFunctions.INT),
	MESSAGE_ATTACK(2, LeekFunctions.INT),
	MESSAGE_DEBUFF(3, LeekFunctions.INT),
	MESSAGE_SHIELD(4, LeekFunctions.INT),
	MESSAGE_BUFF_MP(5, LeekFunctions.INT),
	MESSAGE_BUFF_TP(6, LeekFunctions.INT),
	MESSAGE_BUFF_FORCE(7, LeekFunctions.INT),
	MESSAGE_BUFF_STRENGTH(7, LeekFunctions.INT),
	MESSAGE_BUFF_AGILITY(8, LeekFunctions.INT),
	MESSAGE_MOVE_TOWARD(9, LeekFunctions.INT),
	MESSAGE_MOVE_AWAY(10, LeekFunctions.INT),
	MESSAGE_MOVE_TOWARD_CELL(11, LeekFunctions.INT),
	MESSAGE_MOVE_AWAY_CELL(12, LeekFunctions.INT),
	MESSAGE_CUSTOM(13, LeekFunctions.INT),

	// Area
	AREA_POINT(1, LeekFunctions.INT),// Une seule cellule
	AREA_LASER_LINE(2, LeekFunctions.INT),
	AREA_CIRCLE_1(3, LeekFunctions.INT),
	AREA_CIRCLE_2(4, LeekFunctions.INT),
	AREA_CIRCLE_3(5, LeekFunctions.INT),

	// Chips
	CHIP_BANDAGE(3, LeekFunctions.INT),
	CHIP_CURE(4, LeekFunctions.INT),
	CHIP_DRIP(10, LeekFunctions.INT),
	CHIP_REGENERATION(35, LeekFunctions.INT),
	CHIP_VACCINE(11, LeekFunctions.INT),
	CHIP_SHOCK(1, LeekFunctions.INT),
	CHIP_FLASH(6, LeekFunctions.INT),
	CHIP_LIGHTNING(33, LeekFunctions.INT),
	CHIP_SPARK(18, LeekFunctions.INT),
	CHIP_FLAME(5, LeekFunctions.INT),
	CHIP_METEORITE(36, LeekFunctions.INT),
	CHIP_PEBBLE(19, LeekFunctions.INT),
	CHIP_ROCK(7, LeekFunctions.INT),
	CHIP_ROCKFALL(32, LeekFunctions.INT),
	CHIP_ICE(2, LeekFunctions.INT),
	CHIP_STALACTITE(30, LeekFunctions.INT),
	CHIP_ICEBERG(31, LeekFunctions.INT),
	CHIP_SHIELD(20, LeekFunctions.INT),
	CHIP_HELMET(21, LeekFunctions.INT),
	CHIP_ARMOR(22, LeekFunctions.INT),
	CHIP_WALL(23, LeekFunctions.INT),
	CHIP_RAMPART(24, LeekFunctions.INT),
	CHIP_FORTRESS(29, LeekFunctions.INT),
	CHIP_PROTEIN(8, LeekFunctions.INT),
	CHIP_STEROID(25, LeekFunctions.INT),
	CHIP_DOPING(26, LeekFunctions.INT),
	CHIP_STRETCHING(9, LeekFunctions.INT),
	CHIP_WARM_UP(27, LeekFunctions.INT),
	CHIP_REFLEXES(28, LeekFunctions.INT),
	CHIP_LEATHER_BOOTS(14, LeekFunctions.INT),
	CHIP_WINGED_BOOTS(12, LeekFunctions.INT),
	CHIP_SEVEN_LEAGUE_BOOTS(13, LeekFunctions.INT),
	CHIP_MOTIVATION(15, LeekFunctions.INT),
	CHIP_ADRENALINE(16, LeekFunctions.INT),
	CHIP_RAGE(17, LeekFunctions.INT),
	CHIP_LIBERATION(34, LeekFunctions.INT),
	CHIP_TELEPORTATION(59, LeekFunctions.INT),
	CHIP_ARMORING(67, LeekFunctions.INT),
	CHIP_INVERSION(68, LeekFunctions.INT),
	CHIP_PUNY_BULB(73, LeekFunctions.INT),
	CHIP_FIRE_BULB(74, LeekFunctions.INT),
	CHIP_HEALER_BULB(75, LeekFunctions.INT),
	CHIP_ROCKY_BULB(76, LeekFunctions.INT),
	CHIP_ICED_BULB(77, LeekFunctions.INT),
	CHIP_LIGHTNING_BULB(78, LeekFunctions.INT),
	CHIP_METALLIC_BULB(79, LeekFunctions.INT),
	CHIP_REMISSION(80, LeekFunctions.INT),
	CHIP_CARAPACE(81, LeekFunctions.INT),
	CHIP_RESURRECTION(84, LeekFunctions.INT),
	CHIP_DEVIL_STRIKE(85, LeekFunctions.INT),
	CHIP_WHIP(88, LeekFunctions.INT),
	CHIP_LOAM(89, LeekFunctions.INT),
	CHIP_FERTILIZER(90, LeekFunctions.INT),
	CHIP_ACCELERATION(91, LeekFunctions.INT),
	CHIP_SLOW_DOWN(92, LeekFunctions.INT),
	CHIP_BALL_AND_CHAIN(93, LeekFunctions.INT),
	CHIP_TRANQUILIZER(94, LeekFunctions.INT),
	CHIP_SOPORIFIC(95, LeekFunctions.INT),
	CHIP_SOLIDIFICATION(96, LeekFunctions.INT),
	CHIP_VENOM(97, LeekFunctions.INT),
	CHIP_TOXIN(98, LeekFunctions.INT),
	CHIP_PLAGUE(99, LeekFunctions.INT),
	CHIP_THORN(100, LeekFunctions.INT),
	CHIP_MIRROR(101, LeekFunctions.INT),
	CHIP_FEROCITY(102, LeekFunctions.INT),
	CHIP_COLLAR(103, LeekFunctions.INT),
	CHIP_BARK(104, LeekFunctions.INT),
	CHIP_BURNING(105, LeekFunctions.INT),
	CHIP_FRACTURE(106, LeekFunctions.INT),
	CHIP_ANTIDOTE(110, LeekFunctions.INT),

	MAP_NEXUS(1, LeekFunctions.INT),
	MAP_FACTORY(2, LeekFunctions.INT),
	MAP_DESERT(3, LeekFunctions.INT),
	MAP_FOREST(4, LeekFunctions.INT),
	MAP_GLACIER(5, LeekFunctions.INT),
	MAP_BEACH(6, LeekFunctions.INT),

	FIGHT_TYPE_SOLO(Fight.TYPE_SOLO, LeekFunctions.INT),
	FIGHT_TYPE_FARMER(Fight.TYPE_FARMER, LeekFunctions.INT),
	FIGHT_TYPE_TEAM(Fight.TYPE_TEAM, LeekFunctions.INT),
	FIGHT_TYPE_BATTLE_ROYALE(Fight.TYPE_BATTLE_ROYALE, LeekFunctions.INT),

	FIGHT_CONTEXT_TEST(Fight.CONTEXT_TEST, LeekFunctions.INT),
	FIGHT_CONTEXT_GARDEN(Fight.CONTEXT_GARDEN, LeekFunctions.INT),
	FIGHT_CONTEXT_CHALLENGE(Fight.CONTEXT_CHALLENGE, LeekFunctions.INT),
	FIGHT_CONTEXT_TOURNAMENT(Fight.CONTEXT_TOURNAMENT, LeekFunctions.INT),
	FIGHT_CONTEXT_BATTLE_ROYALE(Fight.CONTEXT_BATTLE_ROYALE, LeekFunctions.INT),

	SUMMON_LIMIT(Fight.SUMMON_LIMIT, LeekFunctions.INT),
	CRITICAL_FACTOR(Effect.CRITICAL_FACTOR, LeekFunctions.DOUBLE);

	private double value;
	private int type;

	FightConstants(double value, int type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public double getValue() {
		return value;
	}
	@Override
	public int getIntValue() {
		return (int) value;
	}
	@Override
	public int getType() {
		return type;
	}
}
