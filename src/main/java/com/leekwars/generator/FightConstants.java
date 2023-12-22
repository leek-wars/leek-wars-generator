package com.leekwars.generator;

import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.EntityState;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.fight.Fight;

import leekscript.common.Type;
import leekscript.runner.ILeekConstant;

public enum FightConstants implements ILeekConstant {

	MAX_TURNS(Fight.MAX_TURNS, Type.INT),

	CELL_EMPTY(0, Type.INT),
	CELL_PLAYER(1, Type.INT),
	CELL_ENTITY(1, Type.INT),
	CELL_OBSTACLE(2, Type.INT),

	// Type entity
	ENTITY_LEEK(1, Type.INT),
	ENTITY_BULB(2, Type.INT),
	ENTITY_TURRET(3, Type.INT),
	ENTITY_CHEST(4, Type.INT),
	ENTITY_MOB(5, Type.INT),

	// Constants
	EFFECT_TARGET_ALLIES(Effect.TARGET_ALLIES, Type.INT),
	EFFECT_TARGET_ENEMIES(Effect.TARGET_ENEMIES, Type.INT),
	EFFECT_TARGET_CASTER(Effect.TARGET_CASTER, Type.INT),
	EFFECT_TARGET_NOT_CASTER(0, Type.INT),
	EFFECT_TARGET_NON_SUMMONS(Effect.TARGET_NON_SUMMONS, Type.INT),
	EFFECT_TARGET_SUMMONS(Effect.TARGET_SUMMONS, Type.INT),
	EFFECT_TARGET_ALWAYS_CASTER(0, Type.INT),

	// Effect modifiers
	EFFECT_MODIFIER_STACKABLE(Effect.MODIFIER_STACKABLE, Type.INT),
	EFFECT_MODIFIER_MULTIPLIED_BY_TARGETS(Effect.MODIFIER_MULTIPLIED_BY_TARGETS, Type.INT),
	EFFECT_MODIFIER_ON_CASTER(Effect.MODIFIER_ON_CASTER, Type.INT),
	EFFECT_MODIFIER_NOT_REPLACEABLE(Effect.MODIFIER_NOT_REPLACEABLE, Type.INT),
	EFFECT_MODIFIER_IRREDUCTIBLE(Effect.MODIFIER_IRREDUCTIBLE, Type.INT),

	EFFECT_DAMAGE(1, Type.INT),
	EFFECT_HEAL(2, Type.INT),
	EFFECT_FORCE(3, Type.INT),
	EFFECT_AGILITY(4, Type.INT),
	EFFECT_RELATIVE_SHIELD(5, Type.INT),
	EFFECT_ABSOLUTE_SHIELD(6, Type.INT),
	EFFECT_MP(7, Type.INT),
	EFFECT_TP(8, Type.INT),
	EFFECT_TELEPORT(10, Type.INT),
	EFFECT_INVERT(Effect.TYPE_PERMUTATION, Type.INT),
	EFFECT_RESURRECT(Effect.TYPE_RESURRECT, Type.INT),
	EFFECT_BUFF_DAMAGE(1, Type.INT),
	EFFECT_BUFF_HEAL(2, Type.INT),
	EFFECT_BUFF_FORCE(3, Type.INT),
	EFFECT_BUFF_STRENGTH(3, Type.INT),
	EFFECT_BUFF_AGILITY(4, Type.INT),
	EFFECT_BUFF_RELATIVE_SHIELD(5, Type.INT),
	EFFECT_BUFF_ABSOLUTE_SHIELD(6, Type.INT),
	EFFECT_BUFF_MP(7, Type.INT),
	EFFECT_BUFF_TP(8, Type.INT),
	EFFECT_BUFF_DEBUFF(9, Type.INT),
	EFFECT_BUFF_TELEPORTATION(10, Type.INT),
	EFFECT_BOOST_MAX_LIFE(Effect.TYPE_VITALITY, Type.INT),
	EFFECT_POISON(Effect.TYPE_POISON, Type.INT),
	EFFECT_SUMMON(Effect.TYPE_SUMMON, Type.INT),
	EFFECT_DEBUFF(Effect.TYPE_DEBUFF, Type.INT),
	EFFECT_KILL(Effect.TYPE_KILL, Type.INT),
	EFFECT_SHACKLE_MP(Effect.TYPE_SHACKLE_MP, Type.INT),
	EFFECT_SHACKLE_TP(Effect.TYPE_SHACKLE_TP, Type.INT),
	EFFECT_SHACKLE_STRENGTH(Effect.TYPE_SHACKLE_STRENGTH, Type.INT),
	EFFECT_DAMAGE_RETURN(Effect.TYPE_DAMAGE_RETURN, Type.INT),
	EFFECT_BUFF_RESISTANCE(Effect.TYPE_BUFF_RESISTANCE, Type.INT),
	EFFECT_BUFF_WISDOM(Effect.TYPE_BUFF_WISDOM, Type.INT),
	EFFECT_SHACKLE_MAGIC(Effect.TYPE_SHACKLE_MAGIC, Type.INT),
	EFFECT_ANTIDOTE(Effect.TYPE_ANTIDOTE, Type.INT),
	EFFECT_AFTEREFFECT(Effect.TYPE_AFTEREFFECT, Type.INT),
	EFFECT_VULNERABILITY(Effect.TYPE_VULNERABILITY, Type.INT),
	EFFECT_ABSOLUTE_VULNERABILITY(Effect.TYPE_ABSOLUTE_VULNERABILITY, Type.INT),
	EFFECT_LIFE_DAMAGE(Effect.TYPE_LIFE_DAMAGE, Type.INT),
	EFFECT_STEAL_ABSOLUTE_SHIELD(Effect.TYPE_STEAL_ABSOLUTE_SHIELD, Type.INT),
	EFFECT_NOVA_DAMAGE(Effect.TYPE_NOVA_DAMAGE, Type.INT),
	EFFECT_RAW_BUFF_MP(Effect.TYPE_RAW_BUFF_MP, Type.INT),
	EFFECT_RAW_BUFF_TP(Effect.TYPE_RAW_BUFF_TP, Type.INT),
	EFFECT_POISON_TO_SCIENCE(Effect.TYPE_POISON_TO_SCIENCE, Type.INT),
	EFFECT_DAMAGE_TO_ABSOLUTE_SHIELD(Effect.TYPE_DAMAGE_TO_ABSOLUTE_SHIELD, Type.INT),
	EFFECT_DAMAGE_TO_STRENGTH(Effect.TYPE_DAMAGE_TO_STRENGTH, Type.INT),
	EFFECT_NOVA_DAMAGE_TO_MAGIC(Effect.TYPE_NOVA_DAMAGE_TO_MAGIC, Type.INT),
	EFFECT_RAW_ABSOLUTE_SHIELD(Effect.TYPE_RAW_ABSOLUTE_SHIELD, Type.INT),
	EFFECT_RAW_BUFF_STRENGTH(Effect.TYPE_RAW_BUFF_STRENGTH, Type.INT),
	EFFECT_RAW_BUFF_MAGIC(Effect.TYPE_RAW_BUFF_MAGIC, Type.INT),
	EFFECT_RAW_BUFF_SCIENCE(Effect.TYPE_RAW_BUFF_SCIENCE, Type.INT),
	EFFECT_RAW_BUFF_AGILITY(Effect.TYPE_RAW_BUFF_AGILITY, Type.INT),
	EFFECT_RAW_BUFF_RESISTANCE(Effect.TYPE_RAW_BUFF_RESISTANCE, Type.INT),
	EFFECT_PROPAGATION(Effect.TYPE_PROPAGATION, Type.INT),
	EFFECT_RAW_BUFF_WISDOM(Effect.TYPE_RAW_BUFF_WISDOM, Type.INT),
	EFFECT_NOVA_VITALITY(Effect.TYPE_NOVA_VITALITY, Type.INT),
	EFFECT_SLIDE_TO(Effect.TYPE_ATTRACT, Type.INT), // Deprecated
	EFFECT_ATTRACT(Effect.TYPE_ATTRACT, Type.INT),
	EFFECT_SHACKLE_AGILITY(Effect.TYPE_SHACKLE_AGILITY, Type.INT),
	EFFECT_SHACKLE_WISDOM(Effect.TYPE_SHACKLE_WISDOM, Type.INT),
	EFFECT_REMOVE_SHACKLES(Effect.TYPE_REMOVE_SHACKLES, Type.INT),
	EFFECT_MOVED_TO_MP(Effect.TYPE_MOVED_TO_MP, Type.INT),
	EFFECT_PUSH(Effect.TYPE_PUSH, Type.INT),
	EFFECT_RAW_BUFF_POWER(Effect.TYPE_RAW_BUFF_POWER, Type.INT),
	EFFECT_REPEL(Effect.TYPE_REPEL, Type.INT),
	EFFECT_RAW_RELATIVE_SHIELD(Effect.TYPE_RAW_RELATIVE_SHIELD, Type.INT),
	EFFECT_ALLY_KILLED_TO_AGILITY(Effect.TYPE_ALLY_KILLED_TO_AGILITY, Type.INT),
	EFFECT_KILL_TO_TP(Effect.TYPE_KILL_TO_TP, Type.INT),
	EFFECT_RAW_HEAL(Effect.TYPE_RAW_HEAL, Type.INT),
	EFFECT_CRITICAL_TO_HEAL(Effect.TYPE_CRITICAL_TO_HEAL, Type.INT),
	EFFECT_ADD_STATE(Effect.TYPE_ADD_STATE, Type.INT),

	// États
	STATE_UNHEALABLE(EntityState.UNHEALABLE.ordinal(), Type.INT),
	STATE_INVINCIBLE(EntityState.INVINCIBLE.ordinal(), Type.INT),

	// Résultats attaque
	USE_CRITICAL(2, Type.INT),
	USE_SUCCESS(1, Type.INT),
	USE_FAILED(0, Type.INT),
	USE_INVALID_TARGET(-1, Type.INT),
	USE_NOT_ENOUGH_TP(-2, Type.INT),
	USE_INVALID_COOLDOWN(-3, Type.INT),
	USE_INVALID_POSITION(-4, Type.INT),
	USE_TOO_MUCH_SUMMONS(-5, Type.INT),
	USE_TOO_MANY_SUMMONS(-5, Type.INT),
	USE_RESURRECT_INVALID_ENTITY(-6, Type.INT),

	// Armes
	WEAPON_PISTOL(37, Type.INT),
	WEAPON_MACHINE_GUN(38, Type.INT),
	WEAPON_DOUBLE_GUN(39, Type.INT),
	WEAPON_SHOTGUN(41, Type.INT),
	WEAPON_MAGNUM(45, Type.INT),
	WEAPON_LASER(42, Type.INT),
	WEAPON_GRENADE_LAUNCHER(43, Type.INT),
	WEAPON_FLAME_THROWER(46, Type.INT),
	WEAPON_DESTROYER(40, Type.INT),
	WEAPON_GAZOR(48, Type.INT),
	WEAPON_ELECTRISOR(44, Type.INT),
	WEAPON_M_LASER(47, Type.INT),
	WEAPON_B_LASER(60, Type.INT),
	WEAPON_KATANA(107, Type.INT),
	WEAPON_BROADSWORD(108, Type.INT),
	WEAPON_AXE(109, Type.INT),
	WEAPON_J_LASER(115, Type.INT),
	WEAPON_ILLICIT_GRENADE_LAUNCHER(116, Type.INT),
	WEAPON_MYSTERIOUS_ELECTRISOR(117, Type.INT),
	WEAPON_UNBRIDLED_GAZOR(118, Type.INT),
	WEAPON_REVOKED_M_LASER(119, Type.INT),
	WEAPON_RIFLE(151, Type.INT),
	WEAPON_RHINO(153, Type.INT),
	WEAPON_EXPLORER_RIFLE(175, Type.INT),
	WEAPON_LIGHTNINGER(180, Type.INT),
	WEAPON_PROTON_CANON(181, Type.INT),
	WEAPON_NEUTRINO(182, Type.INT),
	WEAPON_TASER(183, Type.INT),
	WEAPON_BAZOOKA(184, Type.INT),
	WEAPON_DARK_KATANA(187, Type.INT),
	WEAPON_ENHANCED_LIGHTNINGER(225, Type.INT),
	WEAPON_UNSTABLE_DESTROYER(226, Type.INT),
	WEAPON_SWORD(277, Type.INT),
	WEAPON_HEAVY_SWORD(278, Type.INT),
	WEAPON_ODACHI(408, Type.INT),
	WEAPON_EXCALIBUR(409, Type.INT),
	WEAPON_SCYTHE(410, Type.INT),

	// Messages
	MESSAGE_HEAL(1, Type.INT),
	MESSAGE_ATTACK(2, Type.INT),
	MESSAGE_DEBUFF(3, Type.INT),
	MESSAGE_SHIELD(4, Type.INT),
	MESSAGE_BUFF_MP(5, Type.INT),
	MESSAGE_BUFF_TP(6, Type.INT),
	MESSAGE_BUFF_FORCE(7, Type.INT),
	MESSAGE_BUFF_STRENGTH(7, Type.INT),
	MESSAGE_BUFF_AGILITY(8, Type.INT),
	MESSAGE_MOVE_TOWARD(9, Type.INT),
	MESSAGE_MOVE_AWAY(10, Type.INT),
	MESSAGE_MOVE_TOWARD_CELL(11, Type.INT),
	MESSAGE_MOVE_AWAY_CELL(12, Type.INT),
	MESSAGE_CUSTOM(13, Type.INT),

	// Area
	AREA_POINT(1, Type.INT),// Une seule cellule
	AREA_LASER_LINE(2, Type.INT),
	AREA_CIRCLE_1(3, Type.INT),
	AREA_CIRCLE_2(4, Type.INT),
	AREA_CIRCLE_3(5, Type.INT),
	AREA_PLUS_1(3, Type.INT),
	AREA_PLUS_2(6, Type.INT),
	AREA_PLUS_3(7, Type.INT),
	AREA_X_1(8, Type.INT),
	AREA_X_2(9, Type.INT),
	AREA_X_3(10, Type.INT),
	AREA_SQUARE_1(11, Type.INT),
	AREA_SQUARE_2(12, Type.INT),
	AREA_FIRST_INLINE(13, Type.INT),
	AREA_ENEMIES(14, Type.INT),
	AREA_ALLIES(15, Type.INT),

	// Mode de lancé
	LAUNCH_TYPE_LINE(Attack.LAUNCH_TYPE_LINE, Type.INT),
	LAUNCH_TYPE_DIAGONAL(Attack.LAUNCH_TYPE_DIAGONAL, Type.INT),
	LAUNCH_TYPE_STAR(Attack.LAUNCH_TYPE_STAR, Type.INT),
	LAUNCH_TYPE_STAR_INVERTED(Attack.LAUNCH_TYPE_STAR_INVERTED, Type.INT),
	LAUNCH_TYPE_DIAGONAL_INVERTED(Attack.LAUNCH_TYPE_DIAGONAL_INVERTED, Type.INT),
	LAUNCH_TYPE_LINE_INVERTED(Attack.LAUNCH_TYPE_LINE_INVERTED, Type.INT),
	LAUNCH_TYPE_CIRCLE(Attack.LAUNCH_TYPE_CIRCLE, Type.INT),

	// Chips
	CHIP_BANDAGE(3, Type.INT),
	CHIP_CURE(4, Type.INT),
	CHIP_DRIP(10, Type.INT),
	CHIP_REGENERATION(35, Type.INT),
	CHIP_VACCINE(11, Type.INT),
	CHIP_SHOCK(1, Type.INT),
	CHIP_FLASH(6, Type.INT),
	CHIP_LIGHTNING(33, Type.INT),
	CHIP_SPARK(18, Type.INT),
	CHIP_FLAME(5, Type.INT),
	CHIP_METEORITE(36, Type.INT),
	CHIP_PEBBLE(19, Type.INT),
	CHIP_ROCK(7, Type.INT),
	CHIP_ROCKFALL(32, Type.INT),
	CHIP_ICE(2, Type.INT),
	CHIP_STALACTITE(30, Type.INT),
	CHIP_ICEBERG(31, Type.INT),
	CHIP_SHIELD(20, Type.INT),
	CHIP_HELMET(21, Type.INT),
	CHIP_ARMOR(22, Type.INT),
	CHIP_WALL(23, Type.INT),
	CHIP_RAMPART(24, Type.INT),
	CHIP_FORTRESS(29, Type.INT),
	CHIP_PROTEIN(8, Type.INT),
	CHIP_STEROID(25, Type.INT),
	CHIP_DOPING(26, Type.INT),
	CHIP_STRETCHING(9, Type.INT),
	CHIP_WARM_UP(28, Type.INT),
	CHIP_REFLEXES(27, Type.INT),
	CHIP_LEATHER_BOOTS(14, Type.INT),
	CHIP_WINGED_BOOTS(13, Type.INT),
	CHIP_SEVEN_LEAGUE_BOOTS(12, Type.INT),
	CHIP_MOTIVATION(15, Type.INT),
	CHIP_ADRENALINE(16, Type.INT),
	CHIP_RAGE(17, Type.INT),
	CHIP_LIBERATION(34, Type.INT),
	CHIP_TELEPORTATION(59, Type.INT),
	CHIP_ARMORING(67, Type.INT),
	CHIP_INVERSION(68, Type.INT),
	CHIP_PUNY_BULB(73, Type.INT),
	CHIP_FIRE_BULB(74, Type.INT),
	CHIP_HEALER_BULB(75, Type.INT),
	CHIP_ROCKY_BULB(76, Type.INT),
	CHIP_ICED_BULB(77, Type.INT),
	CHIP_LIGHTNING_BULB(78, Type.INT),
	CHIP_METALLIC_BULB(79, Type.INT),
	CHIP_REMISSION(80, Type.INT),
	CHIP_CARAPACE(81, Type.INT),
	CHIP_RESURRECTION(84, Type.INT),
	CHIP_DEVIL_STRIKE(85, Type.INT),
	CHIP_WHIP(88, Type.INT),
	CHIP_LOAM(89, Type.INT),
	CHIP_FERTILIZER(90, Type.INT),
	CHIP_ACCELERATION(91, Type.INT),
	CHIP_SLOW_DOWN(92, Type.INT),
	CHIP_BALL_AND_CHAIN(93, Type.INT),
	CHIP_TRANQUILIZER(94, Type.INT),
	CHIP_SOPORIFIC(95, Type.INT),
	CHIP_SOLIDIFICATION(96, Type.INT),
	CHIP_VENOM(97, Type.INT),
	CHIP_TOXIN(98, Type.INT),
	CHIP_PLAGUE(99, Type.INT),
	CHIP_THORN(100, Type.INT),
	CHIP_MIRROR(101, Type.INT),
	CHIP_FEROCITY(102, Type.INT),
	CHIP_COLLAR(103, Type.INT),
	CHIP_BARK(104, Type.INT),
	CHIP_BURNING(105, Type.INT),
	CHIP_FRACTURE(106, Type.INT),
	CHIP_ANTIDOTE(110, Type.INT),
	CHIP_PUNISHMENT(114, Type.INT),
	CHIP_COVETOUSNESS(120, Type.INT),
	CHIP_VAMPIRIZATION(121, Type.INT),
	CHIP_PRECIPITATION(122, Type.INT),
	CHIP_ALTERATION(141, Type.INT),
	CHIP_WIZARD_BULB(142, Type.INT),
	CHIP_PLASMA(143, Type.INT),
	CHIP_JUMP(144, Type.INT),
	CHIP_COVID(152, Type.INT),
	CHIP_ELEVATION(154, Type.INT),
	CHIP_KNOWLEDGE(155, Type.INT),
	CHIP_WIZARDRY(156, Type.INT),
	CHIP_REPOTTING(157, Type.INT),
	CHIP_THERAPY(158, Type.INT),
	CHIP_MUTATION(159, Type.INT),
	CHIP_DESINTEGRATION(160, Type.INT),
	CHIP_TRANSMUTATION(161, Type.INT),
	CHIP_GRAPPLE(162, Type.INT),
	CHIP_BOXING_GLOVE(163, Type.INT),
	CHIP_CORN(164, Type.INT),
	CHIP_CHILLI_PEPPER(165, Type.INT),
	CHIP_TACTICIAN_BULB(166, Type.INT),
	CHIP_SAVANT_BULB(167, Type.INT),
	CHIP_SERUM(168, Type.INT),
	CHIP_CRUSHING(169, Type.INT),
	CHIP_BRAINWASHING(170, Type.INT),
	CHIP_ARSENIC(171, Type.INT),
	CHIP_BRAMBLE(172, Type.INT),
	CHIP_DOME(173, Type.INT),
	CHIP_MANUMISSION(174, Type.INT),
	CHIP_PRISM(276, Type.INT),
	CHIP_SHURIKEN(411, Type.INT),
	CHIP_KEMURIDAMA(412, Type.INT),
	CHIP_FIRE_BALL(413, Type.INT),
	CHIP_TREBUCHET(414, Type.INT),
	CHIP_AWEKENING(415, Type.INT),
	CHIP_THUNDER(416, Type.INT),
	CHIP_KILL(417, Type.INT),
	CHIP_APOCALYPSE(418, Type.INT),
	CHIP_DIVINE_PROTECTION(419, Type.INT),

	MAP_NEXUS(1, Type.INT),
	MAP_FACTORY(2, Type.INT),
	MAP_DESERT(3, Type.INT),
	MAP_FOREST(4, Type.INT),
	MAP_GLACIER(5, Type.INT),
	MAP_BEACH(6, Type.INT),
	MAP_TEMPLE(7, Type.INT),
	MAP_TEIEN(8, Type.INT),
	MAP_CASTLE(9, Type.INT),
	MAP_CEMETERY(10, Type.INT),

	FIGHT_TYPE_SOLO(Fight.TYPE_SOLO, Type.INT),
	FIGHT_TYPE_FARMER(Fight.TYPE_FARMER, Type.INT),
	FIGHT_TYPE_TEAM(Fight.TYPE_TEAM, Type.INT),
	FIGHT_TYPE_BATTLE_ROYALE(Fight.TYPE_BATTLE_ROYALE, Type.INT),
	FIGHT_TYPE_BOSS(Fight.TYPE_BOSS, Type.INT),

	FIGHT_CONTEXT_TEST(Fight.CONTEXT_TEST, Type.INT),
	FIGHT_CONTEXT_GARDEN(Fight.CONTEXT_GARDEN, Type.INT),
	FIGHT_CONTEXT_CHALLENGE(Fight.CONTEXT_CHALLENGE, Type.INT),
	FIGHT_CONTEXT_TOURNAMENT(Fight.CONTEXT_TOURNAMENT, Type.INT),
	FIGHT_CONTEXT_BATTLE_ROYALE(Fight.CONTEXT_BATTLE_ROYALE, Type.INT),

	// Boss
	BOSS_NASU_SAMOURAI(1, Type.INT),
	BOSS_FENNEL_KING(2, Type.INT),
	BOSS_EVIL_PUMPKIN(3, Type.INT),

	SUMMON_LIMIT(Fight.SUMMON_LIMIT, Type.INT),
	CRITICAL_FACTOR(Effect.CRITICAL_FACTOR, Type.REAL);

	private double value;
	private Type type;

	FightConstants(double value, Type type) {
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
	public Type getType() {
		return type;
	}
}
