package com.leekwars.game.fight.statistics;

public class FightStatistics {

	public static int KILLS = 12;
	public static int BULLETS = 13;
	public static int USED_CHIPS = 14;
	public static int SUMMONS = 17;
	public static int DAMAGE = 30;
	public static int HEAL = 31;
	public static int DISTANCE = 32;
	public static int STACK_OVERFLOWS = 34;
	public static int ERRORS = 33;
	public static int RESURRECTS = 47;
	public static int DAMAGE_POISON = 48;
	public static int DAMAGE_RETURN = 49;
	public static int CRITICAL_HITS = 50;
	public static int TP_USED = 51;
	public static int MP_USED = 52;
	public static int OPERATIONS = 53;
	public static int SAYS = 54;
	public static int SAYS_LENGTH = 55;
	
	private int sKills = 0;
	private int sBullets = 0;
	private int sUsedChips = 0;
	private int sSummons = 0;
	private long sDammages = 0;
	private long sHeal = 0;
	private long sDistance = 0;
	private int sStackOverflow = 0;
	private int sErrors = 0;
	private int sResurrects = 0;
	private long sDamagePoison = 0;
	private long sDamageReturn = 0;
	private int sCriticalHits = 0;
	private int sTPUsed = 0;
	private int sMPUsed = 0;
	private long sOperations = 0;
	private int sSays = 0;
	private long sSaysLength = 0;

	public void addStackOverflow(int n) {
		sStackOverflow += n;
	}

	public int getStackOverflow() {
		return sStackOverflow;
	}

	public void addDistance(int n) {
		sDistance += n;
	}

	public long getDistance() {
		return sDistance;
	}

	public void addHeal(int n) {
		sHeal += n;
	}

	public long getHeal() {
		return sHeal;
	}

	public void addDammages(int n) {
		sDammages += n;
	}

	public long getDamage() {
		return sDammages;
	}

	public void addSummons(int n) {
		sSummons += n;
	}

	public int getSummons() {
		return sSummons;
	}

	public void addUsedChips(int n) {
		sUsedChips += n;
	}

	public int getUsedChips() {
		return sUsedChips;
	}

	public void addBullets(int n) {
		sBullets += n;
	}

	public int getBullets() {
		return sBullets;
	}

	public void addKills(int n) {
		sKills += n;
	}

	public int getKills() {
		return sKills;
	}

	public void addErrors(int errors) {
		sErrors += errors;
	}

	public int getErrors() {
		return sErrors;
	}

	public long getDamagePoison() {
		return sDamagePoison;
	}

	public void addDamagePoison(long damagePoison) {
		this.sDamagePoison += damagePoison;
	}

	public int getResurrects() {
		return sResurrects;
	}

	public void addResurrects(int resurrects) {
		this.sResurrects += resurrects;
	}

	public long getDamageReturn() {
		return sDamageReturn;
	}

	public void addDamageReturn(long damageReturn) {
		this.sDamageReturn += damageReturn;
	}

	public int getTPUsed() {
		return sTPUsed;
	}

	public void addTPUsed(int TPUsed) {
		this.sTPUsed += TPUsed;
	}

	public int getCriticalHits() {
		return sCriticalHits;
	}

	public void addCriticalHits(int criticalHits) {
		this.sCriticalHits += criticalHits;
	}

	public int getMPUsed() {
		return sMPUsed;
	}

	public void addMPUsed(int MPUsed) {
		this.sMPUsed += MPUsed;
	}

	public long getOperations() {
		return sOperations;
	}

	public void addOperations(long operations) {
		this.sOperations += operations;
	}

	public int getSays() {
		return sSays;
	}

	public void addSays(int says) {
		this.sSays += says;
	}

	public long getSaysLength() {
		return sSaysLength;
	}

	public void addSaysLength(long saysLength) {
		this.sSaysLength += saysLength;
	}
}
