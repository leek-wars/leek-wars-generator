package com.leekwars.generator.fight.action;

import com.alibaba.fastjson.JSONArray;

public interface Action {

	// Actions
	public final static int START_FIGHT = 0;
	public final static int USE_WEAPON = 1;
	public final static int USE_CHIP = 2;
	public final static int SET_WEAPON = 3;
	public final static int END_FIGHT = 4;
	public final static int PLAYER_DEAD = 5;
	public final static int NEW_TURN = 6;
	public final static int LEEK_TURN = 7;
	public final static int END_TURN = 8;
	public final static int SUMMON = 9;
	public final static int MOVE_TO = 10;
	public static final int KILL = 11;

	// Buffs
	public final static int LOST_PT = 100;
	public final static int LOST_LIFE = 101;
	public final static int LOST_PM = 102;
	public final static int HEAL = 103;
	public static final int VITALITY = 104;
	public static final int RESURRECT = 105;
	public static final int LOSE_STRENGTH = 106;
	public final static int NOVA_DAMAGE = 107;
	public final static int DAMAGE_RETURN = 108;
	public final static int LIFE_DAMAGE = 109;
	public final static int POISON_DAMAGE = 110;
	public final static int AFTEREFFECT = 111;
	public static final int NOVA_VITALITY = 112;

	// "fun" actions
	public final static int SAY = 200;
	public final static int LAMA = 201;
	public final static int SHOW_CELL = 202;

	// Effects
	public final static int ADD_WEAPON_EFFECT = 301;
	public final static int ADD_CHIP_EFFECT = 302;
	public final static int REMOVE_EFFECT = 303;
	public final static int UPDATE_EFFECT = 304;
	public final static int ADD_STACKED_EFFECT = 305;
	public final static int REDUCE_EFFECTS = 306; // Action juste pour afficher "Les effets de X sont réduits de Y%"
	public final static int REMOVE_POISONS = 307; // Action juste pour afficher "Les poisons de X sont neutralisés"
	public final static int REMOVE_SHACKLES = 308; // Action juste pour afficher "Les entraves de X sont retirées"

	// Other
	public final static int ERROR = 1000;
	public final static int MAP = 1001;
	public final static int AI_ERROR = 1002;

	public JSONArray getJSON();
}
