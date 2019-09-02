package com.leekwars.generator.trophy;

import com.leekwars.generator.fight.Fight;

public class Trophy {

	public final static int INFINITY = 45;
	public static final int LAMA = 51;
	public final static int BOOSTED = 56;
	public final static int BUFFED = 57;
	public final static int RUNNER = 58;
	public final static int SPRINTER = 59;
	public final static int CARAPACE = 60;
	public final static int MASTODON = 61;
	public final static int TANK = 62;
	public final static int DESERTER = 70;
	public final static int TRAITOR = 73;
	public final static int KAMIKAZE = 74;
	public final static int ROXXOR = 75;
	public final static int STATIC = 76;
	public final static int BRIEF = 79;
	public final static int FORTUNATE = 82;
	public final static int MATHEMATICIAN = 87;
	public final static int GEEK = 88;
	public final static int IMPALER = 94;
	public final static int SKEWERING = 95;
	public final static int STRATEGIST = 96;
	public final static int BUTCHER = 97;
	public final static int DESTROYER = 98;
	public static final int SCHIZOPHRENIC = 99;
	public final static int STACKOVERFLOW = 101;
	public final static int CARDINAL = 150;
	public final static int EXPLORER = 151;
	public final static int STASHED = 164;
	public final static int SNIPER = 165;

	private final static int TEST = 1;
	private final static int ARENA = 2;
	private final static int CHALLENGE = 4;
	private final static int TOURNAMENT = 8;
	private final static int SOLO = 1;
	private final static int FARMER = 2;
	private final static int TEAM = 4;

	private int[] mContexts = null;
	private int[] mTypes = null;

	Trophy() {}

	Trophy(int context, int type) {

		int[] contextes = new int[] { TEST, ARENA, CHALLENGE, TOURNAMENT };
		int[] types = new int[] { SOLO, FARMER, TEAM };
		int len = 0;
		for (int i : contextes)
			if ((context & i) == i)
				len++;

		mContexts = new int[len];
		len = 0;
		if ((context & TEST) == TEST) {
			mContexts[len] = Fight.CONTEXT_TEST;
			len++;
		}
		if ((context & ARENA) == ARENA) {
			mContexts[len] = Fight.CONTEXT_GARDEN;
			len++;
		}
		if ((context & CHALLENGE) == CHALLENGE) {
			mContexts[len] = Fight.CONTEXT_CHALLENGE;
			len++;
		}
		if ((context & TOURNAMENT) == TOURNAMENT) {
			mContexts[len] = Fight.CONTEXT_TOURNAMENT;
			len++;
		}
		len = 0;
		for (int i : types)
			if ((type & i) == i)
				len++;
		mTypes = new int[len];
		len = 0;
		if ((type & SOLO) == SOLO) {
			mTypes[len] = Fight.TYPE_SOLO;
			len++;
		}
		if ((type & FARMER) == FARMER) {
			mTypes[len] = Fight.TYPE_FARMER;
			len++;
		}
		if ((type & TEAM) == TEAM) {
			mTypes[len] = Fight.TYPE_TEAM;
			len++;
		}
	}

	Trophy(int[] contexts, int[] types) {
		mContexts = contexts;
		mTypes = types;
	}

	public boolean available(Fight f) {
		return verifyContext(f.getFightContext()) && verifyType(f.getFightType());
	}

	public boolean verifyContext(int context) {
		if (mContexts == null)
			return true;
		for (int c : mContexts) {
			if (c == context)
				return true;
		}
		return false;
	}

	public boolean verifyType(int type) {
		if (mTypes == null)
			return true;
		for (int c : mTypes) {
			if (c == type)
				return true;
		}
		return false;
	}
}
