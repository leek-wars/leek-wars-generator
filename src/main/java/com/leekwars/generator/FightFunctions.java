package com.leekwars.generator;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;

import leekscript.functions.Functions;
import leekscript.functions.VariableOperations;
import leekscript.runner.AI;
import leekscript.runner.CallableVersion;
import leekscript.runner.ILeekFunction;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.common.Error;
import leekscript.common.Type;

public enum FightFunctions implements ILeekFunction {
	getLife(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getLife(parameters[0]);
		}
	},
	getForce(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getStrength(parameters[0]);
		}
	},
	getStrength(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getStrength(parameters[0]);
		}
	},
	getAgility(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAgility(parameters[0]);
		}
	},
	getScience(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getScience(parameters[0]);
		}
	},
	getWisdom(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWisdom(parameters[0]);
		}
	},
	getResistance(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getResistance(parameters[0]);
		}
	},
	getMagic(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getMagic(parameters[0]);
		}
	},
	getCell(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCell(parameters[0]);
		}
	},
	getWeapon(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeapon(parameters[0]);
		}
	},
	getName(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getName(parameters[0]);
		}
	},
	getMP(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getMP(parameters[0]);
		}
	},
	getTP(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTP(parameters[0]);
		}
	},
	getTotalTP(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTotalTP(parameters[0]);
		}
	},
	getTotalMP(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTotalMP(parameters[0]);
		}
	},
	getTotalLife(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTotalLife(parameters[0]);
		}
	},
	getPower(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getPower(parameters[0]);
		}
	},
	setWeapon(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).setWeapon(ai.integer(parameters[0]));
		}
	},
	say(1, new int[] { AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).say(ai.string(parameters[0]));
		}
	},
	getWeapons(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeapons(parameters[0]);
		}
	},
	isEnemy(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isEnemy(ai.integer(parameters[0]));
		}
	},
	isAlly(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isAlly(ai.integer(parameters[0]));
		}
	},
	isDead(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isDead(ai.integer(parameters[0]));
		}
	},
	isAlive(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isAlive(ai.integer(parameters[0]));
		}
	},
	getLeek(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getEntity();
		}
	},
	getEntity(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getEntity();
		}
	},
	getChips(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChips(parameters[0]);
		}
	},
	getEffects(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getEffects(parameters[0]);
		}
	},
	getLaunchedEffects(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getLaunchedEffects(parameters[0]);
		}
	},
	getPassiveEffects(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getPassiveEffects(parameters[0]);
		}
	},
	getAbsoluteShield(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAbsoluteShield(parameters[0]);
		}
	},
	getRelativeShield(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getRelativeShield(parameters[0]);
		}
	},
	getDate(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getDate();
		}
	},
	getDamageReturn(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getDamageReturn(parameters[0]);
		}
	},
	getLevel(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getLevel(parameters[0]);
		}
	},
	getCurrentCooldown(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			((EntityAI) ai).addSystemLog(FarmerLog.WARNING, Error.DEPRECATED_FUNCTION, new String[] { "getCurrentCooldown", "getCooldown" });
			return null;
		}
	},
	getCooldown(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCurrentCooldown(parameters[0], parameters[1]);
		}
	},
	getFrequency(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFrequency(parameters[0]);
		}
	},
	getCores(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCores(parameters[0]);
		}
	},
	listen(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).listen();
		}
	},
	getLeekID(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getLeekID(parameters[0]);
		}
	},
	getEntityID(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getLeekID(parameters[0]);
		}
	},
	getTeamName(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTeamName(parameters[0]);
		}
	},
	getTime(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTime();
		}
	},
	getTimestamp(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTimestamp();
		}
	},
	getFarmerName(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFarmerName(parameters[0]);
		}
	},
	getFarmerCountry(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFarmerCountry(parameters[0]);
		}
	},
	getTeamID(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTeamId(parameters[0]);
		}
	},
	getFarmerID(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFarmerId(parameters[0]);
		}
	},
	getAIName(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAIName(parameters[0]);
		}
	},
	getAIID(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAIId(parameters[0]);
		}
	},
	useWeaponOnCell(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).useWeaponOnCell(ai.integer(parameters[0]));
		}
	},
	getWeaponMinScope(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponMinRange(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponMinRange(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponMinRange(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponLaunchType(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponLaunchType(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponCost(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponCost(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponEffects(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponEffects(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponPassiveEffects(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponPassiveEffects(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponMaxScope(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponMaxRange(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponMaxRange(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponMaxRange(ai.intOrNull(parameters[0]));
		}
	},
	getWeaponName(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponName(ai.intOrNull(parameters[0]));
		}
	},
	isInlineWeapon(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isInlineWeapon(ai.intOrNull(parameters[0]));
		}
	},
	weaponNeedLos(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).weaponNeedLos(ai.intOrNull(parameters[0]));
		}
	},
	useWeapon(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).useWeapon(ai.integer(parameters[0]));
		}
	},
	canUseWeapon(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).canUseWeapon(parameters[0], parameters[1]);
		}
	},
	canUseWeaponOnCell(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).canUseWeaponOnCell(parameters[0], parameters[1]);
		}
	},
	getWeaponTargets(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponTargets(parameters[0], parameters[1]);
		}
	},
	getWeaponFailure(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponFail(ai.intOrNull(parameters[0]));
		}
	},
	isWeapon(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isWeapon(ai.integer(parameters[0]));
		}
	},
	getWeaponArea(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponArea(parameters[0]);
		}
	},
	useChip(2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).useChip(ai.intOrNull(parameters[0]), ai.intOrNull(parameters[1]));
		}
	},
	useChipOnCell(2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).useChipOnCell(ai.intOrNull(parameters[0]), ai.intOrNull(parameters[1]));
		}
	},
	getChipName(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipName(ai.intOrNull(parameters[0]));
		}
	},
	getChipMinScope(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipMinRange(ai.integer(parameters[0]));
		}
	},
	getChipMinRange(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipMinRange(ai.integer(parameters[0]));
		}
	},
	getChipMaxScope(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipMaxRange(ai.integer(parameters[0]));
		}
	},
	getChipMaxRange(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipMaxRange(ai.integer(parameters[0]));
		}
	},
	getChipCost(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipCost(ai.integer(parameters[0]));
		}
	},
	getChipEffects(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipEffects(ai.integer(parameters[0]));
		}
	},
	isInlineChip(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isInlineChip(ai.integer(parameters[0]));
		}
	},
	chipNeedLos(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).chipNeedLos(ai.integer(parameters[0]));
		}
	},
	getChipCooldown(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipCooldown(ai.integer(parameters[0]));
		}
	},
	canUseChip(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).canUseChip(ai.integer(parameters[0]), ai.integer(parameters[1]));
		}
	},
	canUseChipOnCell(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).canUseChipOnCell(ai.integer(parameters[0]), ai.integer(parameters[1]));
		}
	},
	getChipTargets(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipTargets(ai.integer(parameters[0]), ai.integer(parameters[1]));
		}
	},
	getChipFailure(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipFail(ai.integer(parameters[0]));
		}
	},
	isChip(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isChip(ai.integer(parameters[0]));
		}
	},
	getChipLaunchType(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipLaunchType(parameters[0]);
		}
	},
	getChipArea(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipArea(parameters[0]);
		}
	},
	resurrect(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).reborn(parameters[0], parameters[1]);
		}
	},
	summon(3, new int[] { AI.NUMBER, AI.NUMBER, AI.FUNCTION }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).summon(parameters[0], parameters[1], parameters[2]);
		}
	},
	getSummons(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getSummons(parameters[0]);
		}
	},
	getType(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getType(parameters[0]);
		}
	},
	isSummon(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isSummon(parameters[0]);
		}
	},
	getSummoner(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getSummoner(parameters[0]);
		}
	},
	isStatic(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isStatic(parameters[0]);
		}
	},
	getBirthTurn(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getBirthTurn(parameters[0]);
		}
	},
	getBulbChips(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getBulbChips(parameters[0]);
		}
	},
	getDistance(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getDistance(ai.integer(parameters[0]), ai.integer(parameters[1]));
		}
	},
	getCellDistance(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellDistance(ai.integer(parameters[0]), ai.integer(parameters[1]));
		}
	},
	getPathLength(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getPathLength(parameters[0], parameters[1], parameters[2]);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			((EntityAI) ai).ops(hasVariableOperations() ? mVariableOperations.getOperations(ai.integer(retour)) : 1);
		}
	},
	getPath(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			int start = ai.integer(parameters[0]);
			int end = ai.integer(parameters[1]);
			return ((EntityAI) ai).getPath(start, end, parameters[2]);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			((EntityAI) ai).ops(hasVariableOperations() ? mVariableOperations.getOperations(retour instanceof ArrayLeekValue ? ((ArrayLeekValue) retour).size() : 1) : 1);
		}
	},
	getLeekOnCell(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getLeekOnCell(ai.integer(parameters[0]));
		}
	},
	getEntityOnCell(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getLeekOnCell(ai.integer(parameters[0]));
		}
	},
	getCellContent(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellContent(ai.integer(parameters[0]));
		}
	},
	isEmptyCell(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isEmpty(ai.integer(parameters[0]));
		}
	},
	isObstacle(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isObstacle(ai.integer(parameters[0]));
		}
	},
	isOnSameLine(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isOnSameLine(ai.integer(parameters[0]), ai.integer(parameters[1]));
		}
	},
	isLeek(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isLeekCell(ai.integer(parameters[0]));
		}
	},
	isEntity(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).isLeekCell(ai.integer(parameters[0]));
		}
	},
	getCellX(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellX(ai.integer(parameters[0]));
		}
	},
	getCellY(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellY(ai.integer(parameters[0]));
		}
	},
	getNearestEnemy(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNearestEnemy();
		}
	},
	getFarestEnemy(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFarthestEnemy();
		}
	},
	getTurn(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getTurn();
		}
	},
	getAliveEnemies(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAliveEnemies();
		}
	},
	getAliveEnemiesCount(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNumAliveEnemies();
		}
	},
	getAlliedTurret(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAlliedTurret();
		}
	},
	getAllChips(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAllChips();
		}
	},
	getAllWeapons(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAllWeapons();
		}
	},
	getAllEffects(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAllEffects();
		}
	},
	getEnemyTurret(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getEnemyTurret();
		}
	},
	getDeadEnemies(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getDeadEnemies();
		}
	},
	getDeadEnemiesCount(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNumDeadEnemies();
		}
	},
	getEnemies(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getEnemies();
		}
	},
	getAllies(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAllies();
		}
	},
	getEnemiesCount(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNumEnemies();
		}
	},
	getNearestAlly(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNearestAlly();
		}
	},
	getFarestAlly(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFarthestAlly();
		}
	},
	getAliveAllies(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAliveAllies();
		}
	},
	getDeadAllies(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getDeadAllies();
		}
	},
	getAlliesCount(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNumAllies();
		}
	},
	getNextPlayer(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNextPlayer();
		}
	},
	getPreviousPlayer(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getPreviousPlayer();
		}
	},
	getCellToUseWeapon(1, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellToUseWeapon(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellToUseWeaponOnCell(1, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellToUseWeaponOnCell(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellToUseChip(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellToUseChip(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellToUseChipOnCell(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellToUseChipOnCell(parameters[0], parameters[1], parameters[2]);
		}
	},
	getEnemiesLife(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getEnemiesLife();
		}
	},
	getAlliesLife(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getAlliesLife();
		}
	},
	moveToward(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).moveToward(ai.integer(parameters[0]), ai.intOrNull(parameters[1]));
		}
	},
	moveTowardCell(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).moveTowardCell(ai.integer(parameters[0]), ai.intOrNull(parameters[1]));
		}
	},
	moveTowardLeeks(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var leeks = (ArrayLeekValue) parameters[0];
			return ((EntityAI) ai).moveTowardLeeks(leeks, ai.intOrNull(parameters[1]));
		}
	},
	moveTowardEntities(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var entities = (ArrayLeekValue) parameters[0];
			return ((EntityAI) ai).moveTowardLeeks(entities, ai.intOrNull(parameters[1]));
		}
	},
	moveTowardCells(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var cells = (ArrayLeekValue) parameters[0];
			return ((EntityAI) ai).moveTowardCells(cells, ai.intOrNull(parameters[1]));
		}
	},
	moveAwayFrom(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).moveAwayFrom(ai.integer(parameters[0]), ai.intOrNull(parameters[1]));
		}
	},
	moveAwayFromCell(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).moveAwayFromCell(ai.integer(parameters[0]), ai.intOrNull(parameters[1]));
		}
	},
	moveAwayFromCells(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var cells = (ArrayLeekValue) parameters[0];
			return ((EntityAI) ai).moveAwayFromCells(cells, ai.intOrNull(parameters[1]));
		}
	},
	moveAwayFromLeeks(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var leeks = (ArrayLeekValue) parameters[0];
			return ((EntityAI) ai).moveAwayFromLeeks(leeks, ai.intOrNull(parameters[1]));
		}
	},
	moveAwayFromEntities(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var entities = (ArrayLeekValue) parameters[0];
			return ((EntityAI) ai).moveAwayFromLeeks(entities, ai.intOrNull(parameters[1]));
		}
	},
	moveAwayFromLine(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).moveAwayFromLine(ai.integer(parameters[0]), ai.integer(parameters[1]), ai.intOrNull(parameters[2]));
		}
	},
	moveTowardLine(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).moveTowardLine(ai.integer(parameters[0]), ai.integer(parameters[1]), ai.intOrNull(parameters[2]));
		}
	},
	getCellFromXY(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellFromXY(ai.integer(parameters[0]), ai.integer(parameters[1]));
		}
	},
	getFarthestEnemy(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFarthestEnemy();
		}
	},
	getFarthestAlly(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFarthestAlly();
		}
	},
	getFightID(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFight().getId();
		}
	},
	getFightType(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFight().getFightType();
		}
	},
	getFightContext(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getFight().getFightContext();
		}
	},
	getCellsToUseWeapon(1, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellsToUseWeapon(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellsToUseWeaponOnCell(1, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellsToUseWeaponOnCell(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellsToUseChip(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellsToUseChip(parameters[0], parameters[1], parameters[2]);
		}
	},
	getCellsToUseChipOnCell(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getCellsToUseChipOnCell(parameters[0], parameters[1], parameters[2]);
		}
	},
	getNearestEnemyTo(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNearestEnemyTo(ai.integer(parameters[0]));
		}
	},
	getNearestEnemyToCell(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNearestEnemyToCell(ai.integer(parameters[0]));
		}
	},
	getNearestAllyToCell(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNearestAllyToCell(ai.integer(parameters[0]));
		}
	},
	getNearestAllyTo(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getNearestAllyTo(ai.integer(parameters[0]));
		}
	},
	getWeaponEffectiveArea(1, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getWeaponArea(parameters[0], parameters[1], parameters[2]);
		}
	},
	getChipEffectiveArea(1, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getChipArea(parameters[0], parameters[1], parameters[2]);
		}
	},
	lineOfSight(2, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).lineOfSight(parameters[0], parameters[1], parameters[2]);
		}
	},
	getObstacles(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getObstacles();
		}
	},
	sendTo(3, new int[] { AI.INT, AI.INT, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).sendTo(ai.integer(parameters[0]), ai.integer(parameters[1]), parameters[2]);
		}
	},
	sendAll(2, new int[] { AI.INT, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			((EntityAI) ai).sendAll(ai.integer(parameters[0]), parameters[1]);
			return null;
		}
	},
	getMessages(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getMessages(ai.intOrNull(parameters[0]));
		}
	},
	getMessageAuthor(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var message = (ArrayLeekValue) parameters[0];
			if (message.size() == 3)
				return message.get(((EntityAI) ai).getUAI(), 0);
			return null;
		}
	},
	getMessageType(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var message = (ArrayLeekValue) parameters[0];
			if (message.size() == 3)
				return message.get(((EntityAI) ai).getUAI(), 1);
			return null;
		}
	},
	getMessageParams(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var message = (ArrayLeekValue) parameters[0];
			if (message.size() == 3)
				return message.get(((EntityAI) ai).getUAI(), 2);
			return null;
		}
	},
	lama(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			((EntityAI) ai).lama();
			return null;
		}
	},
	mark(1, 3) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).mark(parameters[0], parameters[1], parameters[2]);
		}
	},
	markText(2, 4) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).markText(parameters[0], parameters[1], parameters[2], parameters[3]);
		}
	},
	clearMarks(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			((EntityAI) ai).clearMarks();
			return null;
		}
	},
	show(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).show(parameters[0], parameters[1]);
		}
	},
	getMapType(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getMapType();
		}
	},
	pause(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			((EntityAI) ai).pause();
			return null;
		}
	},
	getRegisters(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getRegisters();
		}
	},
	getRegister(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getRegister(parameters[0]);
		}
	},
	setRegister(2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).setRegister(parameters[0], parameters[1]);
		}
	},
	deleteRegister(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			((EntityAI) ai).deleteRegister(parameters[0]);
			return null;
		}
	},
	getEntityTurnOrder(0, 1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((EntityAI) ai).getEntityTurnOrder(parameters[0]);
		}
	};

	private int mArguments;
	private int mArgumentsMin;
	private int mOperations = 1;
	protected VariableOperations mVariableOperations = null;
	private int[] parameters = null;

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
		// this.parameters = new int[0];
	}

	FightFunctions(int arguments, int[] parameters) {
		mArgumentsMin = arguments;
		mArguments = arguments;
		this.parameters = parameters;
	}

	FightFunctions(int arguments, int arguments_max) {
		mArgumentsMin = arguments;
		mArguments = arguments_max;
		// this.parameters = new int[0];
	}

	FightFunctions(int arguments, int arguments_max, int[] parameters) {
		mArgumentsMin = arguments;
		mArguments = arguments_max;
		this.parameters = parameters;
	}

	@Override
	public Type getReturnType() {
		return Type.ANY;
	}

	@Override
	public CallableVersion[] getVersions() {
		return null;
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
	public void addOperations(AI ai, ILeekFunction function, Object[] parameters, Object retour) throws LeekRunException {
		ai.ops(getOperations());
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

	public int[] getParameters() {
		return parameters;
	}

	public int cost() {
		return 1;
	}

	public void setOperations(int operations) {
		mOperations = operations;
	}
}
