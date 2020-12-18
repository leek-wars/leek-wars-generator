package com.leekwars.generator;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;

import leekscript.functions.Functions;
import leekscript.functions.VariableOperations;
import leekscript.runner.AI;
import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;

public enum FightFunctions implements ILeekFunction {
	getLife(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getLife(parameters[0]);
		}
	},
	getForce(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getStrength(parameters[0]);
		}
	},
	getStrength(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getStrength(parameters[0]);
		}
	},
	getAgility(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAgility(parameters[0]);
		}
	},
	getScience(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getScience(parameters[0]);
		}
	},
	getWisdom(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWisdom(parameters[0]);
		}
	},
	getResistance(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getResistance(parameters[0]);
		}
	},
	getMagic(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getMagic(parameters[0]);
		}
	},
	getCell(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCell(parameters[0]);
		}
	},
	getWeapon(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeapon(parameters[0]);
		}
	},
	getName(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getName(parameters[0]);
		}
	},
	getMP(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getMP(parameters[0]);
		}
	},
	getTP(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTP(parameters[0]);
		}
	},
	getTotalTP(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTotalTP(parameters[0]);
		}
	},
	getTotalMP(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTotalMP(parameters[0]);
		}
	},
	getTotalLife(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTotalLife(parameters[0]);
		}
	},
	setWeapon(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).setWeapon(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	say(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).say(parameters[0].getString(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING };
		}
	},
	getWeapons(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeapons(parameters[0]);
		}
	},
	isEnemy(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isEnemy(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isAlly(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isAlly(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isDead(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).isDead(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isAlive(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).isAlive(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getLeek(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getEntity());
		}
	},
	getEntity(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getEntity());
		}
	},
	getChips(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChips(parameters[0]);
		}
	},
	getEffects(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getEffects(parameters[0]);
		}
	},
	getLaunchedEffects(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getLaunchedEffects(parameters[0]);
		}
	},
	getPassiveEffects(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getPassiveEffects(parameters[0]);
		}
	},
	getAbsoluteShield(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAbsoluteShield(parameters[0]);
		}
	},
	getRelativeShield(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getRelativeShield(parameters[0]);
		}
	},
	getDate(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getDate();
		}
	},
	getDamageReturn(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getDamageReturn(parameters[0]);
		}
	},
	getLevel(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getLevel(parameters[0]);
		}
	},
	getCurrentCooldown(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			((EntityAI) leekIA).addOperations(EntityAI.ERROR_LOG_COST);
			((EntityAI) leekIA).addSystemLog(FarmerLog.WARNING, FarmerLog.DEPRECATED_FUNCTION, new String[] { "getCurrentCooldown", "getCooldown" });
			return LeekValueManager.NULL;
		}
	},
	getCooldown(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCurrentCooldown(parameters[0], parameters[1]);
		}
	},
	getFrequency(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getFrequency(parameters[0]);
		}
	},
	getCores(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCores(parameters[0]);
		}
	},
	listen(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).listen();
		}
	},
	getLeekID(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getLeekID(parameters[0]);
		}
	},
	getEntityID(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getLeekID(parameters[0]);
		}
	},
	getTeamName(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTeamName(parameters[0]);
		}
	},
	getTime(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTime();
		}
	},
	getTimestamp(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTimestamp();
		}
	},
	getFarmerName(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getFarmerName(parameters[0]);
		}
	},
	getFarmerCountry(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getFarmerCountry(parameters[0]);
		}
	},
	getTeamID(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getTeamId(parameters[0]);
		}
	},
	getFarmerID(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getFarmerId(parameters[0]);
		}
	},
	getAIName(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAIName(parameters[0]);
		}
	},
	getAIID(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAIId(parameters[0]);
		}
	},
	useWeaponOnCell(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).useWeaponOnCell(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getWeaponMinScope(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getWeaponMinRange(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	getWeaponMinRange(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getWeaponMinRange(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	getWeaponCost(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getWeaponCost(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	getWeaponEffects(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeaponEffects(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0]));
		}
	},
	getWeaponPassiveEffects(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeaponPassiveEffects(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0]));
		}
	},
	getWeaponMaxScope(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getWeaponMaxRange(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	getWeaponMaxRange(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getWeaponMaxRange(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	getWeaponName(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(((EntityAI) leekIA).getWeaponName(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	isInlineWeapon(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isInlineWeapon(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	weaponNeedLos(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).weaponNeedLos(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	useWeapon(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).useWeapon(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	canUseWeapon(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).canUseWeapon(parameters[0], parameters[1]));
		}
	},
	canUseWeaponOnCell(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).canUseWeaponOnCell(parameters[0], parameters[1]));
		}
	},
	getWeaponTargets(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeaponTargets(parameters[0], parameters[1]);
		}
	},
	getWeaponFailure(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeaponFail(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0]));
		}
	},
	isWeapon(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isWeapon(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getWeaponLaunchType(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeaponLaunchType(parameters[0]);
		}
	},
	getWeaponArea(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeaponArea(parameters[0]);
		}
	},
	useChip(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).useChip(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0]), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
	},
	useChipOnCell(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).useChipOnCell(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0]), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
	},
	getChipName(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(((EntityAI) leekIA).getChipName(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0])));
		}
	},
	getChipMinScope(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipMinRange(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getChipMinRange(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipMinRange(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getChipMaxScope(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipMaxRange(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getChipMaxRange(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipMaxRange(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getChipCost(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipCost(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getChipEffects(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipEffects(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isInlineChip(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isInlineChip(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	chipNeedLos(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).chipNeedLos(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getChipCooldown(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getChipCooldown(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	canUseChip(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).canUseChip(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	canUseChipOnCell(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).canUseChipOnCell(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	getChipTargets(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipTargets(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	getChipFailure(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipFail(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isChip(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isChip(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getChipLaunchType(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipLaunchType(parameters[0]);
		}
	},
	getChipArea(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipArea(parameters[0]);
		}
	},
	resurrect(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).reborn(parameters[0], parameters[1]);
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	summon(3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).summon(parameters[0], parameters[1], parameters[2]);
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER, FUNCTION };
		}
	},
	getSummons(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getSummons(parameters[0]);
		}
	},
	getType(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getType(parameters[0]);
		}
	},
	isSummon(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).isSummon(parameters[0]);
		}
	},
	getSummoner(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getSummoner(parameters[0]);
		}
	},
	isStatic(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).isStatic(parameters[0]);
		}
	},
	getBirthTurn(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getBirthTurn(parameters[0]);
		}
	},
	getBulbChips(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getBulbChips(parameters[0]);
		}
	},
	getDistance(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(((EntityAI) leekIA).getDistance(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	getCellDistance(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(((EntityAI) leekIA).getCellDistance(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	getPathLength(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getPathLength(((EntityAI) leekIA), parameters[0], parameters[1], parameters[2]);
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			((EntityAI) leekIA).addOperations(hasVariableOperations() ? mVariableOperations.getOperations(retour.getInt(((EntityAI) leekIA).getUAI())) : 1);
		}
	},
	getPath(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getPath(parameters[0], parameters[1], parameters[2]);
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			((EntityAI) leekIA).addOperations(hasVariableOperations() ? mVariableOperations.getOperations(retour.isArray() ? retour.getArray().size() : 1) : 1);
		}
	},
	getLeekOnCell(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getLeekOnCell(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}
		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getEntityOnCell(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getLeekOnCell(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}
		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getCellContent(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getCellContent(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isEmptyCell(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isEmpty(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isObstacle(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isObstacle(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isOnSameLine(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isOnSameLine(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	isLeek(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isLeekCell(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	isEntity(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).isLeekCell(parameters[0].getInt(((EntityAI) leekIA).getUAI())));
		}
		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getCellX(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCellX(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getCellY(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCellY(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getNearestEnemy(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getNearestEnemy());
		}
	},
	getFarestEnemy(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getFarthestEnemy());
		}
	},
	getTurn(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getTurn());
		}
	},
	getAliveEnemies(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAliveEnemies();
		}
	},
	getAliveEnemiesCount(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getNumAliveEnemies());
		}
	},
	getAlliedTurret(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAlliedTurret();
		}
	},
	getAllChips(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAllChips();
		}
	},
	getAllWeapons(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAllWeapons();
		}
	},
	getAllEffects(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAllEffects();
		}
	},
	getEnemyTurret(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getEnemyTurret();
		}
	},
	getDeadEnemies(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getDeadEnemies();
		}
	},
	getDeadEnemiesCount(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getNumDeadEnemies());
		}
	},
	getEnemies(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getEnemies();
		}
	},
	getAllies(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAllies();
		}
	},
	getEnemiesCount(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getNumEnemies());
		}
	},
	getNearestAlly(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getNearestAlly());
		}
	},
	getFarestAlly(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getFarthestAlly());
		}
	},
	getAliveAllies(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getAliveAllies();
		}
	},
	getDeadAllies(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getDeadAllies();
		}
	},
	getAlliesCount(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getNumAllies());
		}
	},
	getNextPlayer(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getNextPlayer());
		}
	},
	getPreviousPlayer(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getPreviousPlayer());
		}
	},
	getCellToUseWeapon(1, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getCellToUseWeapon(parameters[0], parameters[1], parameters[2]));
		}
	},
	getCellToUseWeaponOnCell(1, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getCellToUseWeaponOnCell(parameters[0], parameters[1], parameters[2]));
		}
	},
	getCellToUseChip(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getCellToUseChip(parameters[0], parameters[1], parameters[2]));
		}
	},
	getCellToUseChipOnCell(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getCellToUseChipOnCell(parameters[0], parameters[1], parameters[2]));
		}
	},
	getEnemiesLife(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getEnemiesLife());
		}
	},
	getAlliesLife(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getAlliesLife());
		}
	},
	moveToward(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveToward(parameters[0].getInt(((EntityAI) leekIA).getUAI()), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
	},
	moveTowardCell(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveTowardCell(parameters[0].getInt(((EntityAI) leekIA).getUAI()), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
	},
	moveTowardLeeks(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveTowardLeeks(parameters[0].getArray(), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	moveTowardEntities(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveTowardLeeks(parameters[0].getArray(), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	moveTowardCells(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveTowardCells(parameters[0].getArray(), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	moveAwayFrom(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveAwayFrom(parameters[0].getInt(((EntityAI) leekIA).getUAI()), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
	},
	moveAwayFromCell(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveAwayFromCell(parameters[0].getInt(((EntityAI) leekIA).getUAI()), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
	},
	moveAwayFromCells(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveAwayFromCells(parameters[0].getArray(), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	moveAwayFromLeeks(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveAwayFromLeeks(parameters[0].getArray(), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	moveAwayFromEntities(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveAwayFromLeeks(parameters[0].getArray(), intOrNull(((EntityAI) leekIA).getUAI(), parameters[1])));
		}
		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	moveAwayFromLine(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveAwayFromLine(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI()), intOrNull(((EntityAI) leekIA).getUAI(), parameters[2])));
		}
	},
	moveTowardLine(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).moveTowardLine(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI()), intOrNull(((EntityAI) leekIA).getUAI(), parameters[2])));
		}
	},
	getCellFromXY(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCellFromXY(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	getFarthestEnemy(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getFarthestEnemy());
		}
	},
	getFarthestAlly(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getFarthestAlly());
		}
	},
	getFightID(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getFight().getId());
		}
	},
	getFightType(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getFight().getFightType());
		}
	},
	getFightContext(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getFight().getFightContext());
		}
	},
	getCellsToUseWeapon(1, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCellsToUseWeapon(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellsToUseWeaponOnCell(1, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCellsToUseWeaponOnCell(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellsToUseChip(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCellsToUseChip(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellsToUseChipOnCell(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getCellsToUseChipOnCell(parameters[0], parameters[1], parameters[2]);
		}
	},
	getNearestEnemyTo(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getNearestEnemyTo(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getNearestEnemyToCell(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getNearestEnemyToCell(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getNearestAllyToCell(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getNearestAllyToCell(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getNearestAllyTo(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getNearestAllyTo(parameters[0].getInt(((EntityAI) leekIA).getUAI()));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getWeaponEffectiveArea(1, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getWeaponArea(parameters[0], parameters[1], parameters[2]);
		}
	},
	getChipEffectiveArea(1, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getChipArea(parameters[0], parameters[1], parameters[2]);
		}
	},
	lineOfSight(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).lineOfSight(parameters[0], parameters[1], parameters[2]);
		}
	},
	getObstacles(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getObstacles();
		}
	},
	sendTo(3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(((EntityAI) leekIA).sendTo(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1].getInt(((EntityAI) leekIA).getUAI()), parameters[2]));
		}

		@Override
		public int[] parameters() {
			return new int[] { INT, INT, -1 };
		}
	},
	sendAll(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			((EntityAI) leekIA).sendAll(parameters[0].getInt(((EntityAI) leekIA).getUAI()), parameters[1]);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { INT, -1 };
		}
	},
	getMessages(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getMessages(intOrNull(((EntityAI) leekIA).getUAI(), parameters[0]));
		}
	},
	getMessageAuthor(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (parameters[0].getArray().size() == 3)
				return parameters[0].getArray().get(((EntityAI) leekIA).getUAI(), 0);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	getMessageType(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (parameters[0].getArray().size() == 3)
				return parameters[0].getArray().get(((EntityAI) leekIA).getUAI(), 1);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	getMessageParams(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (parameters[0].getArray().size() == 3)
				return parameters[0].getArray().get(((EntityAI) leekIA).getUAI(), 2);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	lama(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			((EntityAI) leekIA).lama();
			return LeekValueManager.NULL;
		}
	},
	mark(1, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).mark(parameters[0], parameters[1], parameters[2]);
		}
	},
	markText(2, 4) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).markText(parameters[0], parameters[1], parameters[2], parameters[3]);
		}
	},
	clearMarks(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).clearMarks();
		}
	},
	show(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).show(parameters[0], parameters[1]);
		}
	},
	getMapType(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((EntityAI) leekIA).getMapType());
		}
	},
	pause(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			((EntityAI) leekIA).pause();
			return LeekValueManager.NULL;
		}
	},
	getRegisters(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getRegisters();
		}
	},
	getRegister(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getRegister(parameters[0]);
		}
	},
	setRegister(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).setRegister(parameters[0], parameters[1]);
		}
	},
	deleteRegister(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).deleteRegister(parameters[0]);
		}
	},
	getEntityTurnOrder(0, 1) {
		@Override
		public AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, int count) throws Exception {
			return ((EntityAI) leekIA).getEntityTurnOrder(parameters[0]);
		}
	};

	private int mArguments;
	private int mArgumentsMin;
	private int mOperations = 1;
	protected VariableOperations mVariableOperations = null;

	public static final int DOUBLE = 1;
	public static final int INT = 2;
	public static final int BOOLEAN = 3;
	public static final int STRING = 4;
	public static final int NULL = 5;
	public static final int ARRAY = 6;
	public static final int NUMBER = 7;
	public static final int FUNCTION = 8;

	FightFunctions(int arguments) {
		mArgumentsMin = arguments;
		mArguments = arguments;
	}

	FightFunctions(int arguments, int arguments_max) {
		mArgumentsMin = arguments;
		mArguments = arguments_max;
	}

	@Override
	public int getArguments() {
		return mArguments;
	}

	@Override
	public int getArgumentsMin() {
		return mArgumentsMin;
	}

	@Override
	public boolean isExtra() {
		return true;
	}

	@Override
	public void addOperations(AI leekIA, ILeekFunction function, AbstractLeekValue[] parameters, AbstractLeekValue retour, int count) throws Exception {
		leekIA.addOperations(getOperations());
	}

	@Override
	public String getNamespace() {
		return "com.leekwars.generator.FightFunctions";
	}

	public int getOperations() {
		return mOperations;
	}

	public boolean hasVariableOperations() {
		if (mVariableOperations == null) {
			mVariableOperations = Functions.getVariableOperations(name());
		}
		return mVariableOperations != null;
	}

	public static FightFunctions getValue(String name) {
		for (FightFunctions func : FightFunctions.values()) {
			if (func.name().equals(name))
				return func;
			// return IAFunctions.valueOf(name);
		}
		return null;
	}

	/*
	 * Lancer la fonction
	 */
	public abstract AbstractLeekValue run(AI leekIA, ILeekFunction function, AbstractLeekValue parameters[], int count) throws Exception;

	public int[] parameters() {
		return null;
	}

	public int cost() {
		return 1;
	}

	public void setOperations(int operations) {
		mOperations = operations;
	}

	public static AbstractLeekValue executeFunction(EntityAI leekIA, ILeekFunction function, AbstractLeekValue parameters[], int count) throws Exception {

		// Vérification parametres
		int[] types = function.parameters();
		if (types == null || verifyParameters(types, parameters)) {
			AbstractLeekValue retour = function.run(((EntityAI) leekIA).getUAI(), function, parameters, count);
			function.addOperations(((EntityAI) leekIA).getUAI(), function, parameters, retour, count);
			return retour;
		} else {
			// Message d'erreur
			String ret = AbstractLeekValue.getParamString(parameters);
			((EntityAI) leekIA).addOperations(EntityAI.ERROR_LOG_COST);
			((EntityAI) leekIA).addSystemLog(FarmerLog.ERROR, FarmerLog.UNKNOWN_FUNCTION, new String[] { function + "(" + ret + ")" });
			return LeekValueManager.NULL;
		}
		// throw new LeekRunException(LeekRunException.UNKNOWN_FUNCTION);
	}

	public static int intOrNull(AI ai, AbstractLeekValue value) throws LeekRunException {
		if (isType(value, NULL))
			return -1;
		return value.getInt(ai);
	}

	public static boolean verifyParameters(int[] types, AbstractLeekValue[] parameters) {
		if (parameters == null) {
			return types.length == 0;
		}
		if (types.length != parameters.length) {
			return false;
		}
		for (int i = 0; i < types.length; i++) {
			if (types[i] == -1) {
				continue;
			}
			if (!isType(parameters[i], types[i])) {
				return false;
			}
		}
		return true;
	}

	public static boolean isType(AbstractLeekValue value, int type) {
		if (type == BOOLEAN && !(value instanceof BooleanLeekValue))
			return false;
		if (type == INT && !(value instanceof IntLeekValue))
			return false;
		if (type == DOUBLE && !(value instanceof DoubleLeekValue))
			return false;
		if (type == STRING && !(value instanceof StringLeekValue))
			return false;
		if (type == NULL && !(value instanceof NullLeekValue))
			return false;
		if (type == ARRAY && !(value instanceof ArrayLeekValue))
			return false;
		if (type == FUNCTION && !(value instanceof FunctionLeekValue))
			return false;
		if (type == NUMBER && !(value instanceof IntLeekValue) && !(value instanceof DoubleLeekValue))
			return false;
		return true;
	}
}
