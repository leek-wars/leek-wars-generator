package com.leekwars.generator.trophy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TrophyVariables {

	private class LeekList {
		private final Map<Integer, HashSet<Long>> mLeekDatas;

		public LeekList() {
			mLeekDatas = new TreeMap<Integer, HashSet<Long>>();
		}

		public void add(int leek, long value) {

			if (!mLeekDatas.containsKey(leek)) {
				mLeekDatas.put(leek, new HashSet<Long>());
			}
			mLeekDatas.get(leek).add(value);
		}

		public int getCount() {
			int retour = 0;
			for (HashSet<Long> list : mLeekDatas.values()) {
				if (list.size() > retour)
					retour = list.size();
			}
			return retour;
		}
	}

	private final int mFarmer;
	private final List<Integer> mTrophees;
	private long mCellMovement = 0;
	private long mWeaponShot = 0;
	private int mPerfect = 0;
	private int mKilledAllies = 0;
	private int mKilledLeeks = 0;
	private int mUsedChips = 0;
	private int mSummoned = 0;
	private int mFightCellMovement = 0;
	private int mFightWeaponShot = 0;
	private int mFightSpellLaunch = 0;
	private final LeekList mUsedSpells = new LeekList();
	private final LeekList mUsedWeapons = new LeekList();
	private boolean mSuicide = false;
	private final HashSet<Integer> mPrimeCells = new HashSet<Integer>();
	public final List<Integer> cardinal_cells = new ArrayList<Integer>();
	public final boolean[] walked_cells = new boolean[613];
	public int damage = 0;

	public TrophyVariables(int farmer) {
		mFarmer = farmer;
		mTrophees = new ArrayList<Integer>();
	}

	public TrophyVariables(int farmer, long cell_movement, int won_tournaments, int test_fights,
			int perfect, int consecutive_win, long weapon_shot, int killed_allies,
			int won_farmer_tournaments,
			int won_team_tournaments, int killed_leeks, long earned_habs,
			int max_consecutive_farmer, int max_consecutive_leek, int max_consecutive_composition,
			int consecutive_farmer,
			int consecutive_leek, int consecutive_composition, int used_chips, int summoned, int win_battle_royale) {
		mFarmer = farmer;
		mCellMovement = cell_movement;
		mPerfect = perfect;
		mWeaponShot = weapon_shot;
		mKilledAllies = killed_allies;
		mKilledLeeks = killed_leeks;
		mSummoned = summoned;
		mUsedChips = used_chips;
		mTrophees = new ArrayList<Integer>();
	}

	public void addTrophy(int id) {
		mTrophees.add(id);
	}

	public void winTrophy(int trophy) {
		if (mTrophees.contains(trophy)) {
			return;
		}
		TrophyManager.winTrophy(mFarmer, trophy);
		mTrophees.add(trophy);
	}

	public long getCellMovement() {
		return mCellMovement;
	}

	public void addCellMovement(long add) {
		mCellMovement += add;
	}

	public void addPerfect(int nb) {
		mPerfect += nb;
	}

	public int getPerfect() {
		return mPerfect;
	}

	public int getFarmer() {
		return mFarmer;
	}

	public boolean hasTrophee(int id) {
		return mTrophees.contains(id);
	}

	public int getFightCellMovement() {
		return mFightCellMovement;
	}

	public void addFightCellMovement(int nb) {
		mFightCellMovement += nb;
	}

	public void addUsedWeapon(int leek, int id) {
		mUsedWeapons.add(leek, id);
	}

	public void addUsedSpell(int leek, int id) {
		mUsedSpells.add(leek, id);
	}

	public void addWeaponShot(int nb) {
		mWeaponShot += nb;
		mFightWeaponShot += nb;
	}

	public void addSpellLaunch(int nb) {
		mFightSpellLaunch += nb;
	}

	public long getWeaponShot() {
		return mWeaponShot;
	}

	public int getFightWeaponShot() {
		return mFightWeaponShot;
	}

	public int getFightSpellLaunch() {
		return mFightSpellLaunch;
	}

	public int countWeapons() {
		return mUsedWeapons.getCount();
	}

	public int countSpells() {
		return mUsedSpells.getCount();
	}

	public void setSuicide(boolean suicide) {
		mSuicide = suicide;
	}

	public boolean getSuicide() {
		return mSuicide;
	}

	public int getKilledAllies() {
		return mKilledAllies;
	}

	public void addKilledAllies(int nb) {
		mKilledAllies += nb;
	}

	public int getPrimeCell() {
		return mPrimeCells.size();
	}

	public void addPrimeCell(int i) {
		mPrimeCells.add(i);
	}

	public void addTrophies(List<Integer> trophies) {
		mTrophees.addAll(trophies);
	}

	public void addKill(int nb) {
		mKilledLeeks += nb;
	}

	public int getKill() {
		return mKilledLeeks;
	}

	public void addUsedChips(int nb) {
		mUsedChips += nb;
	}

	public int getUsedChips() {
		return mUsedChips;
	}

	public void addSummoned(int nb) {
		mSummoned += nb;
	}

	public int getSummoned() {
		return mSummoned;
	}
}
