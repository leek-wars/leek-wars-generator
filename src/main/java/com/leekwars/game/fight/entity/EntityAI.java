package com.leekwars.game.fight.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;
import com.leekwars.game.Censorship;
import com.leekwars.game.ErrorManager;
import com.leekwars.game.FightConstants;
import com.leekwars.game.attack.Attack;
import com.leekwars.game.attack.EffectParameters;
import com.leekwars.game.attack.chips.Chip;
import com.leekwars.game.attack.chips.Chips;
import com.leekwars.game.attack.effect.Effect;
import com.leekwars.game.attack.weapons.Weapon;
import com.leekwars.game.attack.weapons.Weapons;
import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionAIError;
import com.leekwars.game.fight.action.ActionLoseTP;
import com.leekwars.game.fight.action.ActionSay;
import com.leekwars.game.fight.action.ActionSetWeapon;
import com.leekwars.game.fight.action.ActionShowCell;
import com.leekwars.game.items.Items;
import com.leekwars.game.leek.Leek;
import com.leekwars.game.leek.LeekLog;
import com.leekwars.game.leek.Register;
import com.leekwars.game.maps.Cell;
import com.leekwars.game.maps.Pathfinding;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.LeekValueManager;
import leekscript.runner.PhpArray;
import leekscript.runner.PhpArray.Element;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.ArrayLeekValue.ArrayIterator;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.StringLeekValue;
import leekscript.runner.values.VariableLeekValue;

public class EntityAI extends AI {

	public static final int ERROR_LOG_COST = 1000;

	protected static class LeekMessage {
		private final int mAuthor;
		private final int mType;
		private final AbstractLeekValue mMessage;

		public LeekMessage(int author, int type, AbstractLeekValue message) {
			mAuthor = author;
			mType = type;
			mMessage = message;
		}
	}

	// Un IARunner => égale plus ou moins à une fonction, une partie de code
	// Le LeekIA s'occuper de gérer les liens entre le code utilisateur et les
	// fonctions du combat

	protected Entity mEntity;
	protected Fight fight;
	protected final static boolean LOG_IA = true;
	protected int mBirthTurn = 1;

	protected long mIARunTime = 0;
	protected long mIACpuRunTime = 0;

	protected int ai_id = -1;
	protected String ai_name = "";
	protected int mInstructions;
	protected LeekLog logs;

	protected final List<LeekMessage> mMessages = new ArrayList<LeekMessage>();
	protected final List<String> mSays = new ArrayList<String>();

	protected boolean fp = true;
	protected boolean mIsValid = true;
	
	public EntityAI() {}

	public EntityAI(Entity entity, LeekLog logs) {
		this.mEntity = entity;
//		if (ai == null || ai.getValid() == 0) {
//			mIsValid = false;
//			log.addSystemLog(leek, LeekLog.SERROR, "", LeekLog.NO_AI_EQUIPPED, null);
//			return;
//		}
//		mLeekAI = ai;
//		if (ai.v2) {
//			return;
//		}
//		ai_name = ai.getName();
//		mInstructions = ai.getInstructions();
		try {
//			if (ai != null) {
//				ai_id = ai.getId();
//			}
//			mUAI = LeekScript.getUserAI(ai);
//			if (this == null) {
//				mIsValid = false;
//				return;
//			} else {
//				this.setLeekIA(this);
//			}
		} catch (ClassFormatError e) {
			System.out.println("Impossible de charger l'IA " + ai_id);
//			log.addSystemLog(leek, LeekLog.SERROR, "", LeekLog.CAN_NOT_COMPILE_AI, null);
//		} catch (LeekScriptException e) {
//			ErrorManager.exception(e);
//			if (e.getType() == LeekScriptException.CODE_TOO_LARGE) {
//				log.addSystemLog(leek, LeekLog.SERROR, "", LeekLog.CODE_TOO_LARGE, null);
//			} else if (e.getType() == LeekScriptException.CODE_TOO_LARGE_FUNCTION) {
//				log.addSystemLog(leek, LeekLog.SERROR, "", LeekLog.CODE_TOO_LARGE_FUNCTION, new String[] { e.getFunction() });
//			} else {
//				log.addSystemLog(leek, LeekLog.SERROR, "", LeekLog.CAN_NOT_COMPILE_AI, null);
//			}
		} catch (Exception e) {
//			log.addSystemLog(leek, LeekLog.SERROR, "", LeekLog.CAN_NOT_COMPILE_AI, null);
		}
	}

	public Entity leek() {
		return mEntity;
	}

	public AI getUAI() {
		return this;
	}

	public void setLogs(LeekLog leekLog) {
		logs = leekLog;
	}

	public void setEntity(Entity entity) {
		mEntity = entity;
	}
	
	public void addSystemLog(int type, String key) {
		addSystemLog(type, key, null);
	}

	public void addSystemLog(int type, String key, String[] parameters) {
		addSystemLog(type, key, parameters, Thread.currentThread().getStackTrace());
	}

	public void addSystemLog(int type, String key, String[] parameters, StackTraceElement[] elements) {

		if (type == LeekLog.WARNING)
			type = LeekLog.SWARNING;
		else if (type == LeekLog.ERROR)
			type = LeekLog.SERROR;
		else if (type == LeekLog.STANDARD)
			type = LeekLog.SSTANDARD;

		if (this != null) {
			logs.addSystemLog(mEntity, type, this.getErrorMessage(elements), key, parameters);
		}
	}

	private void putCells(List<Cell> ignore, AbstractLeekValue leeks_to_ignore) throws LeekRunException {
		for (AbstractLeekValue value : leeks_to_ignore.getArray()) {
			Cell l = fight.getMap().getCell(value.getInt(this));
			if (l == null)
				continue;
			ignore.add(l);
		}
	}

	public long getIARunTime() {
		return mIARunTime;
	}

	public long getIACpuRunTime() {
		return mIACpuRunTime / 1000000;
	}

	public int getLevel() {
		return mEntity.getLevel();
	}

	public int getInstructions() {
		return mInstructions;
	}

	public boolean isValid() {
		return mIsValid;
	}

	public LeekLog getLogs() {
		return logs;
	}

	public void addMessage(LeekMessage message) {
		if (mMessages.size() > 200) {
			return;
		}
		mMessages.add(message);
	}

	public List<String> getSays() {
		return mSays;
	}

	public void setFight(Fight fight) {
		this.fight = fight;
		logs.setLogs(fight.getActions());
	}

	public void runTurn() {

		long startTime = System.nanoTime();

		mSays.clear();

		try {

			runIA();

		} catch (StackOverflowError e) {

			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, LeekLog.AI_INTERRUPTED, new String[] { "Stack Overflow" }, e.getStackTrace());
			fight.getTrophyManager().stackOverflow(mEntity);
			fight.statistics.addErrors(1);
			fight.statistics.addStackOverflow(1);

		} catch (ArithmeticException e) {

			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, LeekLog.AI_INTERRUPTED, new String[] { e.getMessage() }, e.getStackTrace());
			fight.statistics.addErrors(1);

		} catch (LeekRunException e) {

			// Exception de l'utilisateur, normales
			fight.statistics.addErrors(1);
			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, LeekLog.AI_INTERRUPTED, new String[] { e.getMessage() }, e.getStackTrace());

			if (e.getError() == LeekRunException.TOO_MUCH_OPERATIONS) {
				fight.getTrophyManager().tooMuchInstructions(mEntity);
			}

		} catch (OutOfMemoryError e) {// Plus de RAM, Erreur critique, on tente
										// de sauver les meubles
			fight.statistics.addErrors(1);
			// Premierement on coupe l'IA incriminée
			mIsValid = false;
			addSystemLog(LeekLog.ERROR, LeekLog.AI_INTERRUPTED, new String[] { "Out Of Memory" }, e.getStackTrace());
			System.out.println("Out Of Memory , Fight : " + fight.getId());

			ErrorManager.exception(e, ai_id);

		} catch (Exception e) {

			ErrorManager.exception(e, ai_id);

			fight.statistics.addErrors(1);
			System.out.println("Erreur importante dans l'IA " + ai_id + "  " + e.getMessage());
//			System.out.println(e);

			addSystemLog(LeekLog.ERROR, LeekLog.AI_INTERRUPTED, new String[] { "Undefined Error" }, e.getStackTrace());
			if (fp) {
				ErrorManager.registerAIError(fight, mEntity, this.getErrorMessage(e), e);
				System.out.println("Informations sur l'erreur");
				fp = false;
			}
		} catch (Throwable e) {

			// ErrorManager.exception(e, mLeekAI.getId());

		} finally {
			if (this != null) {
//				Logger.logOp(fight.getId(), mEntity.getId(), mEntity.getName(), this.getOperations());
				fight.statistics.addOperations(this.getOperations());
			}
		}

		mMessages.clear();

		long endTime = System.nanoTime();
		mIARunTime += (endTime - startTime);
	}

	// setWeapon
	public boolean setWeapon(int weapon_id) throws Exception {
		boolean success = false;
		if (mEntity.getTP() > 0) {
			Weapon w = null;
			for (Weapon w1 : mEntity.getWeapons()) {
				if (w1.getId() == weapon_id)
					w = w1;
			}
			if (w != null) {
				mEntity.setWeapon(w);
				mEntity.useTP(1);
				fight.log(new ActionLoseTP(mEntity, 1));
				fight.log(new ActionSetWeapon(mEntity, w));
				success = true;
			}
		}
		return success;
	}

	//

	public int getEntity() {
		return mEntity.getFId();
	}

	/**
	 * Retourne une LeekVariable contenant le nombre de PM du poireau demandé
	 * (ou du poireau du joueur s'il s'agit d'une NullLeekValue)
	 *
	 * @param value
	 *            Leek dont on veut connaitre le nombre de PM
	 * @return Nombre de PM du leek ou Null si le leek est invalide
	 * @throws LeekRunException
	 */
	public AbstractLeekValue getMP(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getMP());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getMP());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getTP(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getTP());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getTP());
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getTotalMP(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getTotalMP());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getTotalMP());
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getTotalTP(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getTotalTP());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getTotalTP());
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getFrequency(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_FREQUENCY));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_FREQUENCY));
			}
		}
		return LeekValueManager.NULL;
	}

	// Deprecated function in LeekScript
	public AbstractLeekValue getCores(AbstractLeekValue value) {
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getStrength(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_STRENGTH));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_STRENGTH));
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue isSummon(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekBooleanValue(mEntity.getOwnerId() != -1);
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekBooleanValue(l.getOwnerId() != -1);
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getBirthTurn(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mBirthTurn);
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null && l.getLeekIA() != null)
				return LeekValueManager.getLeekIntValue(l.getLeekIA().mBirthTurn);
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getType(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity instanceof Leek ? FightConstants.ENTITY_LEEK.getIntValue() : FightConstants.ENTITY_BULB.getIntValue());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l instanceof Leek ? FightConstants.ENTITY_LEEK.getIntValue() : FightConstants.ENTITY_BULB.getIntValue());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getSummoner(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getOwnerId());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getOwnerId());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getAgility(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_AGILITY));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_AGILITY));
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getResistance(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_RESISTANCE));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_RESISTANCE));
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getScience(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_SCIENCE));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_SCIENCE));
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getMagic(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_MAGIC));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_MAGIC));
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getWisdom(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_WISDOM));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null) {
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_WISDOM));
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getLeekID(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getId());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getId());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getAbsoluteShield(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_ABSOLUTE_SHIELD));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_ABSOLUTE_SHIELD));
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getRelativeShield(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_RELATIVE_SHIELD));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_RELATIVE_SHIELD));
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getDamageReturn(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getStat(Entity.CHARAC_DAMAGE_RETURN));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getStat(Entity.CHARAC_DAMAGE_RETURN));
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getTotalLife(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getTotalLife());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getTotalLife());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getLevel(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getLevel());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getLevel());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getName(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return new StringLeekValue(mEntity.getName());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return new StringLeekValue(l.getName());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getCell(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL) {
			if (mEntity.getCell() != null)
				return LeekValueManager.getLeekIntValue(mEntity.getCell().getId());
		}
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null && l.getCell() != null)
				return LeekValueManager.getLeekIntValue(l.getCell().getId());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getWeapon(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL) {
			if (mEntity.getWeapon() != null)
				return LeekValueManager.getLeekIntValue(mEntity.getWeapon().getTemplate());
		}
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null && l.getWeapon() != null)
				return LeekValueManager.getLeekIntValue(l.getWeapon().getTemplate());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getLife(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getLife());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getLife());
		}
		return LeekValueManager.NULL;
	}

	public boolean isEnemy(int id) {
		Entity l = fight.getEntity(id);
		if (l == null)
			return false;
		return mEntity.getTeam() != l.getTeam();
	}

	public boolean isAlly(int id) {
		Entity l = fight.getEntity(id);
		if (l == null)
			return false;
		return mEntity.getTeam() == l.getTeam();
	}

	public AbstractLeekValue isAlive(int id) {
		Entity l = fight.getEntity(id);
		if (l == null)
			return LeekValueManager.getLeekBooleanValue(false);
		return LeekValueManager.getLeekBooleanValue(!l.isDead());
	}

	public AbstractLeekValue isDead(int id) {
		Entity l = fight.getEntity(id);
		if (l == null)
			return LeekValueManager.getLeekBooleanValue(false);
		return LeekValueManager.getLeekBooleanValue(l.isDead());
	}

	public AbstractLeekValue say(String message) {
		if (mEntity.getTP() < 1)
			return LeekValueManager.getLeekBooleanValue(false);
		mEntity.useTP(1);
		if (message.length() > 500)
			message = message.substring(0, 500);
		message = Censorship.checkString(message);
		fight.log(new ActionLoseTP(mEntity, 1));
		fight.log(new ActionSay(mEntity, message));
		mSays.add(message);
		fight.statistics.addSays(1);
		fight.statistics.addSaysLength(message.length());
		return LeekValueManager.getLeekBooleanValue(true);
	}

	public AbstractLeekValue getWeapons(AbstractLeekValue value) throws Exception {
		Entity l = null;
		if (value.getType() == AbstractLeekValue.NULL)
			l = mEntity;
		else if (value.getType() == AbstractLeekValue.NUMBER)
			l = fight.getEntity(value.getInt(this));
		if (l == null)
			return LeekValueManager.NULL;
		List<Weapon> weapons = l.getWeapons();
		ArrayLeekValue retour = new ArrayLeekValue();
		for (short i = 0; i < weapons.size(); i++) {
			retour.get(this, i).set(this, LeekValueManager.getLeekIntValue(weapons.get(i).getId()));
		}
		return retour;
	}

	public AbstractLeekValue getChips(AbstractLeekValue value) throws Exception {
		Entity l = null;
		if (value.getType() == AbstractLeekValue.NULL)
			l = mEntity;
		else if (value.getType() == AbstractLeekValue.NUMBER)
			l = fight.getEntity(value.getInt(this));
		if (l == null)
			return LeekValueManager.NULL;
		List<Chip> chips = l.getChips();
		ArrayLeekValue retour = new ArrayLeekValue();
		for (short i = 0; i < chips.size(); i++) {
			retour.get(this, i).set(this, LeekValueManager.getLeekIntValue(chips.get(i).getId()));
		}
		return retour;
	}

	// ----- Fonctions Weapon -----
	public int useWeapon(int leek_id) throws Exception {
		int success = -1;
		Entity target = fight.getEntity(leek_id);
		if (target != null && target != mEntity && !target.isDead()) {
			if (mEntity.getWeapon() == null) {
				this.addOperations(EntityAI.ERROR_LOG_COST);
				addSystemLog(LeekLog.WARNING, LeekLog.NO_WEAPON_EQUIPED);
			}
			success = fight.useWeapon(mEntity, target.getCell());
		}
		return success;
	}

	public int useWeaponOnCell(int cell_id) throws Exception {
		int success = -1;
		Cell target = fight.getMap().getCell(cell_id);
		if (target != null && target != mEntity.getCell()) {
			if (mEntity.getWeapon() == null) {
				this.addOperations(EntityAI.ERROR_LOG_COST);
				addSystemLog(LeekLog.WARNING, LeekLog.NO_WEAPON_EQUIPED);
			}
			success = fight.useWeapon(mEntity, target);
		}
		return success;
	}

	public boolean canUseWeapon(AbstractLeekValue value1, AbstractLeekValue value2) throws Exception {
		Entity target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getEntity(value1.getInt(this));
		} else {
			target = fight.getEntity(value2.getInt(this));
			weapon = Weapons.getWeapon(value1.getInt(this));
		}
		if (weapon == null)
			return false;
		if (target != null && target.getCell() != null) {
			return Pathfinding.canUseAttack(mEntity.getCell(), target.getCell(), weapon.getAttack());
		}
		return false;
	}

	public boolean canUseWeaponOnCell(AbstractLeekValue value1, AbstractLeekValue value2) throws Exception {
		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getMap().getCell(value1.getInt(this));
		} else {
			target = fight.getMap().getCell(value2.getInt(this));
			weapon = Weapons.getWeapon(value1.getInt(this));
		}
		if (weapon == null)
			return false;
		if (target != null) {
			return Pathfinding.canUseAttack(mEntity.getCell(), target, weapon.getAttack());
		}
		return false;
	}

	public int getWeaponMinRange(int id) {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return -1;
		return template.getAttack().getMinRange();
	}

	// Deprecated : always 0
	public AbstractLeekValue getWeaponFail(int id) {
		return LeekValueManager.getLeekIntValue(0);
	}

	public int getWeaponMaxRange(int id) {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return -1;
		return template.getAttack().getMaxRange();
	}

	public int getWeaponCost(int id) {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return -1;
		return template.getCost();
	}

	public boolean isInlineWeapon(int id) {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return false;
		return template.getAttack().getLaunchType() == Attack.LAUNCH_TYPE_LINE;
	}

	public String getWeaponName(int id) {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return "";
		return template.getName();
	}

	public AbstractLeekValue getWeaponEffects(int id) throws Exception {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return LeekValueManager.NULL;
		ArrayLeekValue retour = new ArrayLeekValue();
		for (EffectParameters e : template.getAttack().getEffects()) {
			ArrayLeekValue effect = new ArrayLeekValue();
			effect.push(this, LeekValueManager.getLeekIntValue(e.getId()));
			effect.push(this, LeekValueManager.getLeekDoubleValue(e.getValue1()));
			effect.push(this, LeekValueManager.getLeekDoubleValue(e.getValue1() + e.getValue2()));
			effect.push(this, LeekValueManager.getLeekIntValue(e.getTurns()));
			effect.push(this, LeekValueManager.getLeekIntValue(e.getTargets()));
			effect.push(this, LeekValueManager.getLeekBooleanValue(e.isStackable()));
			retour.push(this, effect);
		}
		return retour;
	}

	// ---- Fonctions Chip ----

	public int useChip(int chip_id, int leek_id) throws Exception {

		int success = -1;
		Entity target = fight.getEntity(leek_id);
		Chip chip = mEntity.getChip(chip_id);

		if (chip == null) {
			Chip ct = Chips.getChip(chip_id);

			this.addOperations(EntityAI.ERROR_LOG_COST);
			if (ct == null) {
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id) });
			} else {
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id), ct.getName() });
			}
		}
		if (target != null && chip != null && !target.isDead()) {
			success = fight.useChip(mEntity, target.getCell(), chip);
		}
		return success;
	}

	public int useChipOnCell(int chip_id, int cell_id) throws Exception {

		int success = -1;
		Cell target = fight.getMap().getCell(cell_id);
		Chip template = mEntity.getChip(chip_id);

		if (template == null) {
			Chip ct = Chips.getChip(chip_id);

			this.addOperations(EntityAI.ERROR_LOG_COST);
			if (ct == null) {
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id) });
			} else {
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id), ct.getName() });
			}
		}
		if (target != null && template != null) {
			success = fight.useChip(mEntity, target, template);
		}
		return success;
	}

	public boolean canUseChipOnCell(int chip_id, int cell_id) throws Exception {
		Cell target = fight.getMap().getCell(cell_id);
		Chip template = mEntity.getChip(chip_id);
		if (target != null && template != null && mEntity.getCell() != null) {
			return Pathfinding.canUseAttack(mEntity.getCell(), target, template.getAttack());
		}
		return false;
	}

	public boolean canUseChip(int chip_id, int leek_id) throws Exception {
		Entity target = fight.getEntity(leek_id);
		Chip template = mEntity.getChip(chip_id);
		if (target != null && template != null && target.getCell() != null && mEntity.getCell() != null) {
			return Pathfinding.canUseAttack(mEntity.getCell(), target.getCell(), template.getAttack());
		}
		return false;
	}

	public AbstractLeekValue getChipTargets(int chip_id, int cell_id) throws Exception {

		Cell target = fight.getMap().getCell(cell_id);
		Chip template = mEntity.getChip(chip_id);
		if (target != null && template != null) {
			ArrayLeekValue retour = new ArrayLeekValue();
			List<Entity> entities = template.getAttack().getWeaponTargets(fight, mEntity, fight.getMap().getCell(cell_id));
			for (Entity l : entities) {
				retour.push(this, LeekValueManager.getLeekIntValue(l.getFId()));
			}
			return retour;
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getCurrentCooldown(AbstractLeekValue chip_id, AbstractLeekValue v) throws LeekRunException {

		if (v.getType() == AbstractLeekValue.NULL) {
			Chip chipTemplate = Chips.getChip(chip_id.getInt(this));
			return LeekValueManager.getLeekIntValue(fight.getCooldown(mEntity, chipTemplate));
		}
		if (v.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(v.getInt(this));
			if (l != null) {
				Chip chipTemplate = Chips.getChip(chip_id.getInt(this));
				return LeekValueManager.getLeekIntValue(fight.getCooldown(l, chipTemplate));
			}
		}
		return LeekValueManager.NULL;
	}

	public String getChipName(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null)
			return "";
		return chip.getName();
	}

	public int getChipCooldown(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null)
			return 0;
		return chip.getCooldown();
	}

	public AbstractLeekValue getChipMinRange(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return LeekValueManager.NULL;
		}
		return LeekValueManager.getLeekIntValue(chip.getAttack().getMinRange());
	}

	public AbstractLeekValue getChipMaxRange(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return LeekValueManager.NULL;
		}
		return LeekValueManager.getLeekIntValue(chip.getAttack().getMaxRange());
	}

	// Deprecated : always 0
	public AbstractLeekValue getChipFail(int id) {
		return LeekValueManager.getLeekIntValue(0);
	}

	public AbstractLeekValue getChipCost(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return LeekValueManager.NULL;
		}
		return LeekValueManager.getLeekIntValue(chip.getCost());
	}

	public AbstractLeekValue getC(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return LeekValueManager.NULL;
		}
		return LeekValueManager.getLeekIntValue(chip.getCost());
	}

	public boolean isInlineChip(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null)
			return false;
		return chip.getAttack().getLaunchType() == Attack.LAUNCH_TYPE_LINE;
	}

	public AbstractLeekValue getChipEffects(int id) throws Exception {
		Chip chip = Chips.getChip(id);
		if (chip == null)
			return LeekValueManager.NULL;
		ArrayLeekValue retour = new ArrayLeekValue();
		for (EffectParameters e : chip.getAttack().getEffects()) {
			ArrayLeekValue effect = new ArrayLeekValue();
			effect.push(this, LeekValueManager.getLeekIntValue(e.getId()));
			effect.push(this, LeekValueManager.getLeekDoubleValue(e.getValue1()));
			effect.push(this, LeekValueManager.getLeekDoubleValue(e.getValue1() + e.getValue2()));
			effect.push(this, LeekValueManager.getLeekIntValue(e.getTurns()));
			effect.push(this, LeekValueManager.getLeekIntValue(e.getTargets()));
			effect.push(this, LeekValueManager.getLeekBooleanValue(e.isStackable()));
			retour.push(this, effect);
		}
		return retour;
	}

	// ---- Fonctions Fight ----

	public double getDistance(int c1, int c2) {
		Cell cell1 = fight.getMap().getCell(c1);
		if (cell1 == null)
			return -1;
		Cell cell2 = fight.getMap().getCell(c2);
		if (cell2 == null)
			return -1;
		return Pathfinding.getDistance(cell1, cell2);
	}

	public int getCellDistance(int c1, int c2) {
		Cell cell1 = fight.getMap().getCell(c1);
		if (cell1 == null)
			return -1;
		Cell cell2 = fight.getMap().getCell(c2);
		if (cell2 == null)
			return -1;
		return Pathfinding.getCaseDistance(cell1, cell2);
	}

	public AbstractLeekValue getPathLength(EntityAI ai, AbstractLeekValue c1, AbstractLeekValue c2, AbstractLeekValue leeks_to_ignore) throws Exception {

		Cell cell1 = fight.getMap().getCell(c1.getInt(this));
		if (cell1 == null)
			return LeekValueManager.NULL;
		Cell cell2 = fight.getMap().getCell(c2.getInt(this));
		if (cell2 == null)
			return LeekValueManager.NULL;

		List<Cell> ignore = new ArrayList<Cell>();

		if (leeks_to_ignore.getType() == AbstractLeekValue.ARRAY) {
			putCells(ignore, leeks_to_ignore);
		}
		if (cell1 == cell2)
			return LeekValueManager.getLeekIntValue(0);

		List<Cell> path = fight.getMap().getPathBeetween(ai, cell1, cell2, ignore);
		if (path == null)
			return LeekValueManager.NULL;
		return LeekValueManager.getLeekIntValue(path.size());
	}

	public AbstractLeekValue getPath(AbstractLeekValue c1, AbstractLeekValue c2, AbstractLeekValue leeks_to_ignore) throws Exception {

		Cell cell1 = fight.getMap().getCell(c1.getInt(this));
		if (cell1 == null)
			return LeekValueManager.NULL;
		Cell cell2 = fight.getMap().getCell(c2.getInt(this));
		if (cell2 == null)
			return LeekValueManager.NULL;

		List<Cell> ignore = new ArrayList<Cell>();

		if (leeks_to_ignore.getType() == AbstractLeekValue.ARRAY) {
			for (AbstractLeekValue value : leeks_to_ignore.getArray()) {
				Cell l = fight.getMap().getCell(value.getInt(this));
				if (l == null)
					continue;
				ignore.add(l);
			}
		} else if (leeks_to_ignore.getType() == AbstractLeekValue.NUMBER) {
			logs.addLog(mEntity, LeekLog.WARNING,
					"Attention, la fonction getPath(Cell start, Cell end, Leek leek_to_ignore) va disparaitre, il faut désorthiss utiliser un tableau de cellules à ignorer.");
			Entity l = fight.getEntity(leeks_to_ignore.getInt(this));
			if (l != null && l.getCell() != null) {
				ignore.add(l.getCell());
			}
		}
		if (cell1 == cell2)
			return new ArrayLeekValue();

		List<Cell> path = fight.getMap().getPathBeetween(this, cell1, cell2, ignore);
		if (path == null)
			return LeekValueManager.NULL;
		ArrayLeekValue retour = new ArrayLeekValue();
		for (short i = 0; i < path.size(); i++) {
			retour.get(this, i).set(this, LeekValueManager.getLeekIntValue(path.get(i).getId()));
		}
		return retour;
	}

	public int getLeekOnCell(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return -1;
		return cell.getPlayer() != null ? cell.getPlayer().getFId() : -1;
	}

	public int getCellContent(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return -1;
		return !cell.isWalkable() ? 2 : (cell.getPlayer() != null ? 1 : 0);
	}

	public AbstractLeekValue getCellFromXY(int x, int y) {
		Cell cell = fight.getMap().getCell(x + fight.getMap().getWidth() - 1, y);
		if (cell == null)
			return LeekValueManager.NULL;
		return LeekValueManager.getLeekIntValue(cell.getId());
	}

	public AbstractLeekValue getCellX(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return LeekValueManager.NULL;
		return LeekValueManager.getLeekIntValue(cell.getX() - fight.getMap().getWidth() + 1);
	}

	public AbstractLeekValue getCellY(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return LeekValueManager.NULL;
		return LeekValueManager.getLeekIntValue(cell.getY());
	}

	public boolean isEmpty(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return false;
		return cell.available();
	}

	public boolean isObstacle(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return true;
		return !cell.isWalkable();
	}

	public boolean isLeekCell(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return false;
		return cell.getPlayer() != null;
	}

	public boolean isOnSameLine(int c1, int c2) {
		Cell cell1 = fight.getMap().getCell(c1);
		if (cell1 == null)
			return false;
		Cell cell2 = fight.getMap().getCell(c2);
		if (cell2 == null)
			return false;
		return cell1.getX() == cell2.getX() || cell1.getY() == cell2.getY();
	}

	public int getNearestEnemy() throws Exception {

		List<Entity> entities = fight.getEnemiesEntities(mEntity.getTeam());

		if (mEntity.getCell() == null)
			return -1;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(mEntity.getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? -1 : nearest.getFId();
	}

	public int getFarthestEnemy() throws Exception {

		List<Entity> entities = fight.getEnemiesEntities(mEntity.getTeam());

		int dist = -1;
		Entity farest = null;
		for (Entity l : entities) {
			if (l.isDead() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(mEntity.getCell(), l.getCell());
			if (d > dist || dist == -1) {
				dist = d;
				farest = l;
			}
		}
		return farest == null ? -1 : farest.getFId();
	}

	public int getTurn() {
		return fight.getTurn();
	}

	public ArrayLeekValue getAliveEnemies() throws Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		short nb = 0;
		for (Entity e : fight.getAllEntities(false)) {
			if (e.getTeam() != mEntity.getTeam()) {
				retour.get(this, nb).set(this, LeekValueManager.getLeekIntValue(e.getFId()));
				nb++;
			}
		}
		return retour;
	}

	public int getNumAliveEnemies() throws Exception {
		short count = 0;
		for (Entity e : fight.getAllEntities(false)) {
			if (e.getTeam() != mEntity.getTeam()) {
				count++;
			}
		}
		return count;
	}

	public ArrayLeekValue getDeadEnemies() throws Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		short nb = 0;
		for (Entity e : fight.getAllEntities(true)) {
			if (e.getTeam() != mEntity.getTeam() && e.isDead()) {
				retour.get(this, nb).set(this, LeekValueManager.getLeekIntValue(e.getFId()));
				nb++;
			}
		}
		return retour;
	}

	public int getNumDeadEnemies() throws Exception {
		short count = 0;
		for (Entity e : fight.getAllEntities(true)) {
			if (e.getTeam() != mEntity.getTeam() && e.isDead()) {
				count++;
			}
		}
		return count;
	}

	public ArrayLeekValue getEnemies() throws Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		short nb = 0;
		for (Entity l : fight.getEnemiesEntities(mEntity.getTeam(), true)) {
			retour.get(this, nb).set(this, LeekValueManager.getLeekIntValue(l.getFId()));
			nb++;
		}
		return retour;
	}

	public int getEnemiesLife() {
		int life = 0;
		for (Entity l : fight.getEnemiesEntities(mEntity.getTeam())) {
			life += l.getLife();
		}
		return life;
	}

	public int getNumEnemies() throws Exception {
		return fight.getEnemiesEntities(mEntity.getTeam(), true).size();
	}

	public int getNearestAlly() throws Exception {
		List<Entity> entities = fight.getTeamEntities(mEntity.getTeam());
		if (mEntity.getCell() == null)
			return -1;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead() || l == mEntity || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(mEntity.getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? -1 : nearest.getFId();
	}

	public int getFarthestAlly() throws Exception {
		List<Entity> entities = fight.getTeamEntities(mEntity.getTeam());
		int dist = -1;
		Entity farest = null;
		for (Entity l : entities) {
			if (l.isDead() || l == mEntity || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(mEntity.getCell(), l.getCell());
			if (d > dist || dist == -1) {
				dist = d;
				farest = l;
			}
		}
		return farest == null ? -1 : farest.getFId();
	}

	public ArrayLeekValue getAliveAllies() throws Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		for (Entity l : fight.getTeamEntities(mEntity.getTeam())) {
			retour.push(this, LeekValueManager.getLeekIntValue(l.getFId()));
		}
		return retour;
	}

	public int getNumAliveAllies() throws Exception {
		return fight.getTeamEntities(mEntity.getTeam()).size();
	}

	public ArrayLeekValue getDeadAllies() throws Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		for (Entity l : fight.getTeamEntities(mEntity.getTeam(), true)) {
			if (l.isDead()) {
				retour.push(this, LeekValueManager.getLeekIntValue(l.getFId()));
			}
		}
		return retour;
	}

	public int getNumDeadAllies() throws Exception {
		short nb = 0;
		for (Entity l : fight.getTeamEntities(mEntity.getTeam(), true)) {
			if (l.isDead())
				nb++;
		}
		return nb;
	}

	public ArrayLeekValue getAllies() throws Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		for (Entity l : fight.getTeamEntities(mEntity.getTeam(), true)) {
			retour.push(this, LeekValueManager.getLeekIntValue(l.getFId()));
		}
		return retour;
	}

	public int getNumAllies() throws Exception {
		return fight.getTeamEntities(mEntity.getTeam(), true).size();
	}

	public int getAlliesLife() {
		int life = 0;
		for (Entity l : fight.getTeamEntities(mEntity.getTeam())) {
			life += l.getLife();
		}
		return life;
	}

	public int getNextPlayer() {
		return fight.getOrder().getNextPlayer().getFId();
	}

	public int getPreviousPlayer() {
		return fight.getOrder().getPreviousPlayer().getFId();
	}

	public AbstractLeekValue getWeaponTargets(AbstractLeekValue value1, AbstractLeekValue value2) throws Exception {

		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getMap().getCell(value1.getInt(this));
		} else {
			weapon = Weapons.getWeapon(value1.getInt(this));
			target = fight.getMap().getCell(value2.getInt(this));
		}

		if (weapon == null)
			return LeekValueManager.NULL;
		if (target != null && mEntity.getCell() != null) {
			ArrayLeekValue retour = new ArrayLeekValue();
			List<Entity> leeks = weapon.getAttack().getWeaponTargets(fight, mEntity, target);
			for (Entity l : leeks) {
				retour.push(this, LeekValueManager.getLeekIntValue(l.getFId()));
			}
			return retour;
		}
		return LeekValueManager.NULL;
	}

	/**
	 * Retourne la liste des cellules affectées par le tir sur la cellule
	 * target_cell
	 *
	 * @param value1
	 *            Cellule cible de l'attaque ou id d'arme
	 * @param value2
	 *            Cellule cible de l'attaque ou null
	 * @param value3
	 *            Cellule de départ de l'attaque ou null
	 * @return Array des cellules affectées
	 * @throws Exception
	 */
	public AbstractLeekValue getWeaponArea(AbstractLeekValue value1, AbstractLeekValue value2, AbstractLeekValue value3) throws Exception {
		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getMap().getCell(value1.getInt(this));
		} else {
			weapon = Weapons.getWeapon(value1.getInt(this));
			target = fight.getMap().getCell(value2.getInt(this));
		}

		Cell start_cell = mEntity.getCell();
		if (value3.getType() != AbstractLeekValue.NULL) {
			start_cell = fight.getMap().getCell(value3.getInt(this));
		}

		if (target == null)
			return LeekValueManager.NULL;
		// On récupère l'arme
		if (weapon == null)
			return LeekValueManager.NULL;
		// On vérifie que la cellule de départ existe
		if (start_cell == null)
			return LeekValueManager.NULL;
		ArrayLeekValue retour = new ArrayLeekValue();

		if (Pathfinding.verifyRange(start_cell, target, weapon.getAttack())) {
			// On récupère les cellules touchées
			List<Cell> cells = weapon.getAttack().getTargetCells(start_cell, target);
			// On les met dans le tableau
			for (Cell cell : cells) {
				retour.push(this, LeekValueManager.getLeekIntValue(cell.getId()));
			}
		}
		return retour;
	}

	public int getCellToUseWeapon(AbstractLeekValue value1, AbstractLeekValue value2, AbstractLeekValue value3) throws Exception {
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		Entity target = null;

		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getEntity(value1.getInt(this));
		} else {
			weapon = Weapons.getWeapon(value1.getInt(this));
			target = fight.getEntity(value2.getInt(this));
		}
		int cell = -1;
		if (target != null && target.getCell() != null && weapon != null) {

			ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
			if (value3.getType() == AbstractLeekValue.ARRAY) {
				putCells(cells_to_ignore, value3);
			} else
				cells_to_ignore.add(mEntity.getCell());
			List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target.getCell(), cells_to_ignore);
			if (possible != null && possible.size() > 0) {
				if (possible.contains(mEntity.getCell())) {
					cell = mEntity.getCell().getId();
				} else {
					List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), possible, cells_to_ignore);
					if (path != null) {
						if (path.size() > 0)
							cell = path.get(path.size() - 1).getId();
						else
							cell = -1;
					}

				}
			}
		}
		return cell;
	}

	public int getCellToUseWeaponOnCell(AbstractLeekValue value1, AbstractLeekValue value2, AbstractLeekValue value3) throws Exception {

		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getMap().getCell(value1.getInt(this));
		} else {
			weapon = Weapons.getWeapon(value1.getInt(this));
			target = fight.getMap().getCell(value2.getInt(this));
		}
		int retour = -1;
		if (target != null && weapon != null) {

			ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
			if (value3.getType() == AbstractLeekValue.ARRAY) {
				putCells(cells_to_ignore, value3);
			} else
				cells_to_ignore.add(mEntity.getCell());

			List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target, cells_to_ignore);
			if (possible != null && possible.size() > 0) {
				if (possible.contains(mEntity.getCell())) {
					retour = mEntity.getCell().getId();
				} else {
					List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), possible, cells_to_ignore);
					if (path != null) {
						if (path.size() > 0)
							retour = path.get(path.size() - 1).getId();
						else
							retour = -1;
					}

				}
			}
		}
		return retour;
	}

	public int getCellToUseChip(AbstractLeekValue chip, AbstractLeekValue t, AbstractLeekValue value3) throws Exception {

		Entity target = fight.getEntity(t.getInt(this));
		int cell = -1;
		if (target == null)
			return cell;
		Chip template = Chips.getChip(chip.getInt(this));
		if (template == null)
			return cell;
		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3.getType() == AbstractLeekValue.ARRAY) {
			putCells(cells_to_ignore, value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target.getCell(), cells_to_ignore);
		if (possible != null && possible.size() > 0) {
			if (possible.contains(mEntity.getCell())) {
				cell = mEntity.getCell().getId();
			} else {
				List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), possible, cells_to_ignore);
				if (path != null) {
					if (path.size() > 0)
						cell = path.get(path.size() - 1).getId();
					else
						cell = mEntity.getCell().getId();
				}
			}
		}
		return cell;
	}

	public int getCellToUseChipOnCell(AbstractLeekValue chip, AbstractLeekValue cell, AbstractLeekValue value3) throws Exception {

		int retour = -1;
		Cell target = fight.getMap().getCell(cell.getInt(this));
		if (target == null)
			return cell.getInt(this);
		Chip template = Chips.getChip(chip.getInt(this));
		if (template == null)
			return cell.getInt(this);

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3.getType() == AbstractLeekValue.ARRAY) {
			putCells(cells_to_ignore, value3);
		} else
			cells_to_ignore.add(mEntity.getCell());

		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target, cells_to_ignore);
		if (possible != null && possible.size() > 0) {
			if (possible.contains(mEntity.getCell())) {
				retour = mEntity.getCell().getId();
			} else {
				List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), possible);
				if (path != null) {
					if (path.size() > 0)
						retour = path.get(path.size() - 1).getId();
					else
						retour = mEntity.getCell().getId();
				}
			}
		}
		return retour;
	}

	public int moveToward(int leek_id, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP()) {
			pm = mEntity.getMP();
		}
		int used_pm = 0;
		if (pm > 0) {
			Entity target = fight.getEntity(leek_id);
			if (target != null && !target.isDead()) {
				List<Cell> path = fight.getMap().getPathBeetween(this, mEntity.getCell(), target.getCell(), null);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path.subList(1, Math.min(path.size(), pm + 1)));
				}
			}
		}
		return used_pm;
	}

	public int moveTowardCell(int cell_id, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP()) {
			pm = mEntity.getMP();
		}
		int used_pm = 0;
		if (pm > 0 && mEntity.getCell() != null) {
			Cell target = mEntity.getCell().getMap().getCell(cell_id);
			if (target != null && target != mEntity.getCell()) {
				List<Cell> path = null;
				if (!target.isWalkable())
					path = Pathfinding.getAStarPath(this, mEntity.getCell(), Pathfinding.getValidCellsAroundObstacle(target), null);
				else
					path = fight.getMap().getPathBeetween(this, mEntity.getCell(), target, null);

				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path.subList(1, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public int moveTowardLeeks(ArrayLeekValue leeks, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (short i = 0; i < leeks.size(); i++) {
				int value = leeks.get(this, i).getValue().getInt(this);
				Entity l = fight.getEntity(value);
				if (l != null && !l.isDead())
					targets.add(l.getCell());
			}
			if (targets.size() != 0) {
				List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), targets);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path.subList(1, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public int moveTowardCells(ArrayLeekValue leeks, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (short i = 0; i < leeks.size(); i++) {
				int value = leeks.get(this, i).getValue().getInt(this);
				Cell c = fight.getMap().getCell(value);
				if (c != null)
					targets.add(c);
			}
			if (targets.size() != 0) {
				List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), targets);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path.subList(1, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public int moveAwayFrom(int leek_id, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			Entity target = fight.getEntity(leek_id);
			if (target != null && target.getCell() != null) {
				List<Cell> cells = new ArrayList<Cell>();
				cells.add(target.getCell());
				List<Cell> path = fight.getMap().getPathAway(this, mEntity.getCell(), cells, pm);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path);
				}
			}
		}
		return used_pm;
	}

	public int moveAwayFromCell(int cell_id, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			Cell target = fight.getMap().getCell(cell_id);
			if (target != null) {
				List<Cell> cells = new ArrayList<Cell>();
				cells.add(target);
				List<Cell> path = fight.getMap().getPathAway(this, mEntity.getCell(), cells, pm);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path);
				}
			}
		}
		return used_pm;
	}

	public int moveAwayFromLeeks(ArrayLeekValue leeks, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (short i = 0; i < leeks.size(); i++) {
				int value = leeks.get(this, i).getValue().getInt(this);
				Entity l = fight.getEntity(value);
				if (l != null && !l.isDead())
					targets.add(l.getCell());
			}
			if (targets.size() != 0) {
				List<Cell> path = fight.getMap().getPathAway(this, mEntity.getCell(), targets, pm);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path);
				}
			}
		}
		return used_pm;
	}

	public int moveAwayFromCells(ArrayLeekValue leeks, int pm_to_use) throws Exception {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (short i = 0; i < leeks.size(); i++) {
				int value = leeks.get(this, i).getValue().getInt(this);
				Cell c = fight.getMap().getCell(value);
				if (c != null)
					targets.add(c);
			}
			if (targets.size() != 0) {
				List<Cell> path = fight.getMap().getPathAway(this, mEntity.getCell(), targets, pm);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path);
				}
			}
		}
		return used_pm;
	}

	public int moveAwayFromLine(int cell1, int cell2, int pm_to_use) throws Exception {
		// Nombre de PM à utiliser au max pour fuir comme un lache
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		// On met le nombre de PM utilisés au final (pour renvoyer à
		// l'utilisateur)
		int used_pm = 0;
		// On regarde si le nombre de PM qu'on veut utiliser est valide
		if (pm > 0) {
			// On récupère les cellules
			Cell target = fight.getMap().getCell(cell1);
			Cell target2 = fight.getMap().getCell(cell2);
			// Si il est trouvé on calcule le path
			if (target != null && target2 != null) {
				List<Cell> path = Pathfinding.getPathAwayFromLine(this, mEntity.getCell(), target, target2, pm);
				// Si un path a été trouvé on se barre !
				if (path != null) {
					int[] cells = new int[path.size()];
					for (int i = 0; i < path.size(); i++)
						cells[i] = path.get(i).getId();
					// log.addCell(mEntity, cells, 255, 1);
					used_pm = fight.moveEntity(mEntity, path);
				}
			}
		}
		return used_pm;
	}

	public int moveTowardLine(int cell1, int cell2, int pm_to_use) throws Exception {
		// Nombre de PM à utiliser au max
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		// On met le nombre de PM utilisés au final (pour renvoyer à
		// l'utilisateur)
		int used_pm = 0;
		// On regarde si le nombre de PM qu'on veut utiliser est valide
		if (pm > 0) {
			// On récupère les cellules
			Cell target = fight.getMap().getCell(cell1);
			Cell target2 = fight.getMap().getCell(cell2);
			// Si il est trouvé on calcule le path
			if (target != null && target2 != null) {
				List<Cell> path = Pathfinding.getPathTowardLine(this, mEntity.getCell(), target, target2);
				// Si un path a été trouvé on y va
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path.size() > pm ? path.subList(0, pm) : path);
				}
			}
		}
		return used_pm;
	}

	public Fight getFight() {
		return fight;
	}

	public void lama() {
		if (mEntity.getTP() < 1) {
			return;
		}
		mEntity.useTP(1);
		fight.log(new ActionLoseTP(mEntity, 1));
		fight.getTrophyManager().lama(mEntity);
	}

	public boolean sendTo(int target, int type, AbstractLeekValue message) {
		if (target == mEntity.getId()) {
			return false;
		}
		Entity l = fight.getEntity(target);
		if (l == null) {
			return false;
		}
		if (l.getUsedLeekIA() != null)
			l.getUsedLeekIA().addMessage(new LeekMessage(mEntity.getFId(), type, message));
		return true;
	}

	public void sendAll(int type, AbstractLeekValue message) {
		for (Entity l : fight.getTeamEntities(mEntity.getTeam())) {
			if (l.getId() == mEntity.getId())
				continue;
			if (l.getUsedLeekIA() != null)
				l.getUsedLeekIA().addMessage(new LeekMessage(mEntity.getFId(), type, message));
		}
	}

	public AbstractLeekValue getMessages(int target_leek) throws Exception {

		// On récupere le leek ciblé
		Entity l = mEntity;
		if (target_leek != -1 && target_leek != l.getFId()) {
			l = fight.getEntity(target_leek);
			if (mEntity.getTP() < 1) {
				return LeekValueManager.NULL;
			}
			mEntity.useTP(1);
			fight.log(new ActionLoseTP(mEntity, 1));
		}

		// On crée le tableau de retour
		EntityAI lia = l.getLeekIA();
		ArrayLeekValue messages = new ArrayLeekValue();

		// On y ajoute les messages
		for (LeekMessage message : lia.mMessages) {
			ArrayLeekValue m = new ArrayLeekValue();
			m.get(this, 0).set(this, LeekValueManager.getLeekIntValue(message.mAuthor));
			m.get(this, 1).set(this, LeekValueManager.getLeekIntValue(message.mType));
			m.get(this, 2).set(this, LeekOperations.clone(this, message.mMessage));
			messages.push(this, m);
		}
		return messages;
	}

	public AbstractLeekValue getEffects(AbstractLeekValue value) throws Exception {
		Entity l = null;
		if (value.getType() == AbstractLeekValue.NULL) {
			l = mEntity;
		} else if (value.getType() == AbstractLeekValue.NUMBER) {
			l = fight.getEntity(value.getInt(this));
		}
		if (l == null) {
			return LeekValueManager.NULL;
		}
		ArrayLeekValue retour = new ArrayLeekValue();
		int i = 0;
		for (Effect effect : l.getEffects()) {
			retour.get(this, i).set(this, effect.getLeekValue(this));
			i++;
		}
		return retour;
	}

	public AbstractLeekValue getLaunchedEffects(AbstractLeekValue value) throws Exception {
		Entity l = null;
		if (value.getType() == AbstractLeekValue.NULL) {
			l = mEntity;
		} else if (value.getType() == AbstractLeekValue.NUMBER) {
			l = fight.getEntity(value.getInt(this));
		}
		if (l == null) {
			return LeekValueManager.NULL;
		}
		ArrayLeekValue retour = new ArrayLeekValue();
		int i = 0;
		for (Effect effect : l.getLaunchedEffects()) {
			retour.get(this, i).set(this, effect.getLeekValue(this));
			i++;
		}
		return retour;
	}

	public boolean isTest() {
		return mEntity.getId() < 0;
	}

	/**
	 * Retourne la liste des cellules affectées par le sort lancé sur la cellule
	 * target_cell
	 *
	 * @param chip_id
	 *            Id du sort à lancer
	 * @param target_cell
	 *            Cellule cible de l'attaque
	 * @return Array des cellules affectées
	 * @throws Exception
	 */
	public AbstractLeekValue getChipArea(AbstractLeekValue value1, AbstractLeekValue value2, AbstractLeekValue value3) throws Exception {

		Cell start_cell = mEntity.getCell();
		if (value3.getType() != AbstractLeekValue.NULL) {
			start_cell = fight.getMap().getCell(value3.getInt(this));
		}
		// On vérifie que la cellule de départ existe

		if (start_cell == null)
			return LeekValueManager.NULL;
		// On récupère la cellule
		Cell c = fight.getMap().getCell(value2.getInt(this));
		if (c == null || mEntity.getCell() == null)
			return LeekValueManager.NULL;
		// On récupère le sort
		Chip template = Chips.getChip(value1.getInt(this));

		if (template == null)
			return LeekValueManager.NULL;

		ArrayLeekValue retour = new ArrayLeekValue();
		if (Pathfinding.verifyRange(start_cell, c, template.getAttack())) {
			// On récupère les cellules touchées
			List<Cell> cells = template.getAttack().getTargetCells(start_cell, c);
			// On les met dans le tableau
			if (cells != null) {
				for (Cell cell : cells) {
					retour.push(this, LeekValueManager.getLeekIntValue(cell.getId()));
				}
			}
		}
		return retour;
	}

	/**
	 * Renvoie la liste des cellules à partir desquelles ont peut utiliser son
	 * arme sur le poireau cible
	 *
	 * @param target_leek_id
	 *            Poireau cible
	 * @return Liste des cellules
	 * @throws Exception
	 */
	public AbstractLeekValue getCellsToUseWeapon(AbstractLeekValue value1, AbstractLeekValue value2, AbstractLeekValue value3) throws Exception {
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		Entity target = null;

		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getEntity(value1.getInt(this));
		} else {
			weapon = Weapons.getWeapon(value1.getInt(this));
			target = fight.getEntity(value2.getInt(this));
		}

		if (target == null || target.getCell() == null || weapon == null || mEntity.getCell() == null)
			return LeekValueManager.NULL;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3.getType() == AbstractLeekValue.ARRAY) {
			putCells(cells_to_ignore, value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target.getCell(), cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, LeekValueManager.getLeekIntValue(cell.getId()));
			}
		}

		return retour;
	}

	/**
	 * Renvoie la liste des cellules à partir desquelles on peut utiliser son
	 * arme sur la cellule cible
	 *
	 * @param target_cell_id
	 *            Cellule cible
	 * @return Liste des cellules
	 * @throws Exception
	 */
	public AbstractLeekValue getCellsToUseWeaponOnCell(AbstractLeekValue value1, AbstractLeekValue value2, AbstractLeekValue value3) throws Exception {

		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2.getType() == AbstractLeekValue.NULL) {
			target = fight.getMap().getCell(value1.getInt(this));
		} else {
			weapon = Weapons.getWeapon(value1.getInt(this));
			target = fight.getMap().getCell(value2.getInt(this));
		}

		if (target == null || weapon == null || mEntity.getCell() == null)
			return LeekValueManager.NULL;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3.getType() == AbstractLeekValue.ARRAY) {
			putCells(cells_to_ignore, value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target, cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, LeekValueManager.getLeekIntValue(cell.getId()));
			}
		}

		return retour;
	}

	/**
	 * Renvoie la liste des cellules à partir desquelles on peut utiliser la
	 * puce chip_id, sur le poireau cible
	 *
	 * @param chip_id
	 *            Puce à tester
	 * @param target_leek_id
	 *            Poireau cible
	 * @return Liste des cellules
	 * @throws Exception
	 */
	public AbstractLeekValue getCellsToUseChip(AbstractLeekValue chip_id, AbstractLeekValue target_leek_id, AbstractLeekValue value3) throws Exception {

		Entity target = fight.getEntity(target_leek_id.getInt(this));
		// On récupère le sort
		Chip template = Chips.getChip(chip_id.getInt(this));
		if (target == null || target.getCell() == null || template == null || mEntity.getCell() == null)
			return LeekValueManager.NULL;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3.getType() == AbstractLeekValue.ARRAY) {
			putCells(cells_to_ignore, value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target.getCell(), cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, LeekValueManager.getLeekIntValue(cell.getId()));
			}
		}

		return retour;
	}

	/**
	 * Renvoie la liste des cellules à partir desquelles on peut utiliser la
	 * puce chip_id, sur la cellule cible
	 *
	 * @param chip_id
	 *            Puce à tester
	 * @param target_cell_id
	 *            Cellule cible
	 * @return Liste des cellules
	 * @throws Exception
	 */
	public AbstractLeekValue getCellsToUseChipOnCell(AbstractLeekValue chip_id, AbstractLeekValue target_cell_id, AbstractLeekValue value3) throws Exception {

		Cell target = fight.getMap().getCell(target_cell_id.getInt(this));
		// On récupère le sort
		Chip template = Chips.getChip(chip_id.getInt(this));
		if (target == null || template == null || mEntity.getCell() == null)
			return LeekValueManager.NULL;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3.getType() == AbstractLeekValue.ARRAY) {
			putCells(cells_to_ignore, value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target, cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, LeekValueManager.getLeekIntValue(cell.getId()));
			}
		}

		return retour;
	}

	/**
	 * Retourne l'ennemi le plus proche du leek fournis en paramètre
	 *
	 * @param leek_id
	 *            Leek cible
	 * @return Ennemi le plus proche
	 */
	public AbstractLeekValue getNearestEnemyTo(int leek_id) {

		List<Entity> entities = fight.getEnemiesEntities(mEntity.getTeam());

		Entity entitie = fight.getEntity(leek_id);
		if (entitie == null || entitie.getCell() == null)
			return LeekValueManager.NULL;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead())
				continue;
			if (entitie == l)
				continue;
			if (l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(entitie.getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? LeekValueManager.NULL : LeekValueManager.getLeekIntValue(nearest.getFId());
	}

	/**
	 * Retourne l'ennemi le plus proche de la cellule fourni en paramètre
	 *
	 * @param cell_id
	 *            Cellule cible
	 * @return Ennemi le plus proche
	 */
	public AbstractLeekValue getNearestEnemyToCell(int cell_id) {

		List<Entity> entities = fight.getEnemiesEntities(mEntity.getTeam());

		Cell cell = fight.getMap().getCell(cell_id);
		if (cell == null)
			return LeekValueManager.NULL;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead() || l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(cell, l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? LeekValueManager.NULL : LeekValueManager.getLeekIntValue(nearest.getFId());
	}

	/**
	 * Retourne l'alli� le plus proche du leek fournis en paramètre
	 *
	 * @param leek_id
	 *            Leek cible
	 * @return Alli� le plus proche
	 */
	public AbstractLeekValue getNearestAllyTo(int leek_id) {
		List<Entity> entities = fight.getTeamEntities(mEntity.getTeam() == 2 ? 2 : 1);
		Entity entity = fight.getEntity(leek_id);
		if (entity == null || entity.getCell() == null)
			return LeekValueManager.NULL;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead())
				continue;
			if (entity == l || l == this.mEntity)
				continue;
			int d = Pathfinding.getDistance2(entity.getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? LeekValueManager.NULL : LeekValueManager.getLeekIntValue(nearest.getFId());
	}

	/**
	 * Retourne l'allié le plus proche de la cellule fournie en paramètre
	 *
	 * @param cell_id
	 *            Cellule cible
	 * @return C le plus proche
	 */
	public AbstractLeekValue getNearestAllyToCell(int cell_id) {
		List<Entity> entities = fight.getTeamEntities(mEntity.getTeam() == 2 ? 2 : 1);
		Cell cell = fight.getMap().getCell(cell_id);
		if (cell == null)
			return LeekValueManager.NULL;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead())
				continue;
			if (l == this.mEntity)
				continue;
			int d = Pathfinding.getDistance2(cell, l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}

		return nearest == null ? LeekValueManager.NULL : LeekValueManager.getLeekIntValue(nearest.getFId());
	}

	public AbstractLeekValue lineOfSight(AbstractLeekValue start, AbstractLeekValue end, AbstractLeekValue ignore) throws LeekRunException {

		Cell s = fight.getMap().getCell(start.getInt(this));
		Cell e = fight.getMap().getCell(end.getInt(this));

		if (s == null || e == null)
			return LeekValueManager.NULL;

		if (ignore.getType() == AbstractLeekValue.NUMBER) {

			Entity l = fight.getEntity(ignore.getInt(this));
			List<Cell> cells = new ArrayList<Cell>();
			if (l != null && l.getCell() != null)
				cells.add(l.getCell());
			return LeekValueManager.getLeekBooleanValue(Pathfinding.verifyLoS(s, e, null, cells));

		} else if (ignore.getType() == AbstractLeekValue.ARRAY) {

			List<Cell> cells = new ArrayList<Cell>();
			if (mEntity.getCell() != null)
				cells.add(mEntity.getCell());
			for (AbstractLeekValue value : ignore.getArray()) {
				if (value.getType() == AbstractLeekValue.NUMBER) {
					Entity l = fight.getEntity(value.getInt(this));
					if (l != null && l.getCell() != null) {
						cells.add(l.getCell());
					}
				}
			}
			return LeekValueManager.getLeekBooleanValue(Pathfinding.verifyLoS(s, e, null, cells));

		} else {

			List<Cell> cells = new ArrayList<Cell>();
			cells.add(mEntity.getCell());
			return LeekValueManager.getLeekBooleanValue(Pathfinding.verifyLoS(s, e, null, cells));
		}
	}

	public AbstractLeekValue getObstacles() throws Exception {
		Cell[] cells = fight.getMap().getObstacles();
		ArrayLeekValue retour = new ArrayLeekValue();
		if (cells == null)
			return retour;

		// On ajoute les caces
		for (Cell c : cells)
			retour.push(this, LeekValueManager.getLeekIntValue(c.getId()));

		return retour;
	}

	public AbstractLeekValue mark(AbstractLeekValue cell, AbstractLeekValue color, AbstractLeekValue duration) throws LeekRunException {
		int d = 1;
		int col = 1;
		int[] cel = null;
		if (cell.getType() == AbstractLeekValue.NUMBER) {
			int id = cell.getInt(this);
			if (fight.getMap().getCell(id) == null)
				return LeekValueManager.getLeekBooleanValue(false);
			cel = new int[] { cell.getInt(this) };
		} else if (cell.getType() == AbstractLeekValue.ARRAY) {
			cel = new int[cell.getArray().size()];
			int i = 0;
			for (AbstractLeekValue value : cell.getArray()) {
				if (fight.getMap().getCell(value.getInt(this)) == null)
					continue;
				cel[i] = value.getInt(this);
				i++;
			}
			if (i == 0)
				return LeekValueManager.getLeekBooleanValue(false);
		} else
			return LeekValueManager.getLeekBooleanValue(false);

		if (color.getType() == AbstractLeekValue.NUMBER)
			col = color.getInt(this);
		if (duration.getType() == AbstractLeekValue.NUMBER)
			d = duration.getInt(this);

		logs.addCell(mEntity, cel, col, d);

		return LeekValueManager.getLeekBooleanValue(true);
	}

	public void pause() {
		logs.addPause(mEntity);
	}

	public AbstractLeekValue show(AbstractLeekValue cell, AbstractLeekValue color) throws LeekRunException {
		int cell_id = 1;
		int col = 0xFFFFFF;
		if (cell.getType() == AbstractLeekValue.NUMBER)
			cell_id = cell.getInt(this);
		else
			return LeekValueManager.getLeekBooleanValue(false);
		if (fight.getMap().getCell(cell_id) == null)
			return LeekValueManager.getLeekBooleanValue(false);

		if (color.getType() == AbstractLeekValue.NUMBER)
			col = color.getInt(this);

		if (mEntity.getTP() < 1) {
			return LeekValueManager.getLeekBooleanValue(false);
		}
		mEntity.useTP(1);
		fight.log(new ActionLoseTP(mEntity, 1));

		fight.log(new ActionShowCell(mEntity, cell_id, col));

		return LeekValueManager.getLeekBooleanValue(true);
	}

	public AbstractLeekValue color(AbstractLeekValue red, AbstractLeekValue green, AbstractLeekValue blue) throws LeekRunException {
		return LeekValueManager.getLeekIntValue(((red.getInt(this) & 255) << 16) | ((green.getInt(this) & 255) << 8) | (blue.getInt(this) & 255));
	}

	public AbstractLeekValue listen() throws Exception {
		ArrayLeekValue values = new ArrayLeekValue();
		for (Entity l : fight.getAllEntities(false)) {
			if (l == mEntity)
				continue;
			for (String say : l.getLeekIA().getSays()) {
				ArrayLeekValue s = new ArrayLeekValue();
				s.push(this, LeekValueManager.getLeekIntValue(l.getFId()));
				s.push(this, new StringLeekValue(say));
				values.push(this, s);
			}
		}
		return values;
	}

	public boolean isWeapon(int id) {
		return Items.getType(id) == Items.TYPE_WEAPON;
	}

	public boolean isChip(int id) {
		return Items.getType(id) == Items.TYPE_CHIP;
	}

	public AbstractLeekValue getWeaponLaunchType(AbstractLeekValue weapon_id) throws LeekRunException {
		Weapon template = null;
		if (weapon_id.getType() == AbstractLeekValue.NULL) {
			template = mEntity.getWeapon();
		} else {
			template = Weapons.getWeapon(weapon_id.getInt(this));
		}
		if (template == null)
			return LeekValueManager.NULL;
		return LeekValueManager.getLeekIntValue(template.getAttack().getLaunchType());
	}

	public AbstractLeekValue getChipLaunchType(AbstractLeekValue chip_id) throws LeekRunException {
		Chip template = Chips.getChip(chip_id.getInt(this));
		if (template == null)
			return LeekValueManager.NULL;
		return LeekValueManager.getLeekIntValue(template.getAttack().getLaunchType());
	}

	public AbstractLeekValue getAIName(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return new StringLeekValue(mEntity.getAIName());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return new StringLeekValue(l.getAIName());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getTeamName(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return new StringLeekValue(mEntity.getTeamName());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null && l.getTeamName() != null)
				return new StringLeekValue(l.getTeamName());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getFarmerName(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return new StringLeekValue(mEntity.getFarmerName());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return new StringLeekValue(l.getFarmerName());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getFarmerCountry(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL) {
			return new StringLeekValue(mEntity.getFarmerCountry());
		}
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return new StringLeekValue(l.getFarmerCountry());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getFarmerId(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getFarmer());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getFarmer());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getTeamId(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getTeamId());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getTeamId());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getWeaponArea(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Weapon weapon = Weapons.getWeapon(value.getInt(this));
			if (weapon != null) {
				return LeekValueManager.getLeekIntValue(weapon.getAttack().getArea());
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getChipArea(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Chip template = Chips.getChip(value.getInt(this));
			if (template != null) {
				return LeekValueManager.getLeekIntValue(template.getAttack().getArea());
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getAIId(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(mEntity.getAIId());
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null)
				return LeekValueManager.getLeekIntValue(l.getAIId());
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue getRegisters() throws LeekRunException, Exception {
		ArrayLeekValue array = new ArrayLeekValue();
		if (mEntity.getRegister() == null)
			return array;
		for (Entry<String, String> e : mEntity.getRegister().getValues().entrySet()) {
			array.getOrCreate(this, new StringLeekValue( e.getKey())).set(this, new StringLeekValue(e.getValue()));
		}
		return array;
	}

	public AbstractLeekValue getRegister(AbstractLeekValue key) throws LeekRunException {
		if (mEntity.getRegister() == null)
			return LeekValueManager.NULL;
		String value = mEntity.getRegister().get(key.getString(this));
		if (value == null)
			return LeekValueManager.NULL;
		return new StringLeekValue( value);
	}

	public AbstractLeekValue setRegister(AbstractLeekValue key, AbstractLeekValue value) throws LeekRunException {
		if (mEntity.getRegister() == null) {
			fight.setRegister(mEntity.getId(), new Register(true));
		}
		Register r = mEntity.getRegister();
		if (r == null) return LeekValueManager.getLeekBooleanValue(false);
		boolean b = r.set(key.getString(this), value.getString(this));
		return LeekValueManager.getLeekBooleanValue(b);
	}

	public AbstractLeekValue deleteRegister(AbstractLeekValue key) throws LeekRunException {
		if (mEntity.getRegister() == null)
			return LeekValueManager.NULL;
		return LeekValueManager.getLeekBooleanValue(mEntity.getRegister().delete(key.getString(this)));
	}

	public void arrayFlatten(ArrayLeekValue array, ArrayLeekValue retour, int depth) throws Exception {
		for (AbstractLeekValue value : array) {
			if (value.getValue() instanceof ArrayLeekValue && depth > 0) {
				arrayFlatten(value.getArray(), retour, depth - 1);
			} else
				retour.push(this, LeekOperations.clone(this, value));
		}
	}

	public AbstractLeekValue arrayFoldLeft(ArrayLeekValue array, AbstractLeekValue function, AbstractLeekValue start_value) throws Exception {
		AbstractLeekValue result = LeekOperations.clone(this, start_value);
		// AbstractLeekValue prev = null;
		for (AbstractLeekValue value : array) {
			result = function.executeFunction(this, new AbstractLeekValue[] { result, value });
		}
		return result;
	}

	public AbstractLeekValue arrayFoldRight(ArrayLeekValue array, AbstractLeekValue function, AbstractLeekValue start_value) throws Exception {
		AbstractLeekValue result = LeekOperations.clone(this, start_value);
		// AbstractLeekValue prev = null;
		Iterator<AbstractLeekValue> it = array.getReversedIterator();
		while (it.hasNext()) {
			result = function.executeFunction(this, new AbstractLeekValue[] { it.next(), result });
		}
		return result;
	}

	public AbstractLeekValue arrayPartition(ArrayLeekValue array, AbstractLeekValue function) throws Exception {
		ArrayLeekValue list1 = new ArrayLeekValue();
		ArrayLeekValue list2 = new ArrayLeekValue();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return new ArrayLeekValue();
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		ArrayIterator iterator = array.getArrayIterator();
		boolean b;
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1)
				b = function.executeFunction(this, new AbstractLeekValue[] { value }).getBoolean();
			else
				b = function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value }).getBoolean();
			iterator.setValue(this, value);
			(b ? list1 : list2).getOrCreate(this, iterator.getKey(this)).set(this, iterator.getValue(this));
			iterator.next();
		}
		return new ArrayLeekValue(this, new AbstractLeekValue[] { list1, list2 }, false);
	}

	public ArrayLeekValue arrayMap(ArrayLeekValue array, AbstractLeekValue function) throws LeekRunException, Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		ArrayIterator iterator = array.getArrayIterator();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return retour;
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1)
				retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, function.executeFunction(this, new AbstractLeekValue[] { value }));
			else
				retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value }));
			iterator.setValue(this, value);
			iterator.next();
		}
		return retour;
	}

	public ArrayLeekValue arrayFilter(ArrayLeekValue array, AbstractLeekValue function) throws LeekRunException, Exception {
		ArrayLeekValue retour = new ArrayLeekValue();
		ArrayIterator iterator = array.getArrayIterator();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return retour;
		boolean b;
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1) {
				b = function.executeFunction(this, new AbstractLeekValue[] { value }).getBoolean();
				iterator.setValue(this, value);
				if (b)
					retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, iterator.getValue(this).getValue());

			} else {
				b = function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value }).getBoolean();
				iterator.setValue(this, value);
				if (b)
					retour.getOrCreate(this, iterator.getKey(this).getValue()).set(this, iterator.getValue(this).getValue());

			}
			iterator.next();
		}
		return retour;
	}

	public AbstractLeekValue arrayIter(ArrayLeekValue array, AbstractLeekValue function) throws LeekRunException, Exception {
		ArrayIterator iterator = array.getArrayIterator();
		int nb = function.getArgumentsCount(this);
		if (nb != 1 && nb != 2)
			return LeekValueManager.NULL;
		VariableLeekValue value = new VariableLeekValue(this, LeekValueManager.NULL);
		while (!iterator.ended()) {
			value.set(this, iterator.getValueReference());
			if (nb == 1)
				function.executeFunction(this, new AbstractLeekValue[] { value });
			else
				function.executeFunction(this, new AbstractLeekValue[] { iterator.getKey(this), value });
			iterator.setValue(this, value);
			iterator.next();
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue arraySort(ArrayLeekValue origin, final AbstractLeekValue function) throws Exception {
		try {
			int nb = function.getArgumentsCount(this);
			if (nb == 2) {
				ArrayLeekValue array = LeekOperations.clone(this, origin).getArray();
				array.sort(this, new Comparator<PhpArray.Element>() {
					@Override
					public int compare(Element o1, Element o2) {
						try {
							return function.executeFunction(EntityAI.this, new AbstractLeekValue[] { o1.value(), o2.value() }).getInt(EntityAI.this);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
				return array;
			} else if (nb == 4) {
				ArrayLeekValue array = LeekOperations.clone(this, origin).getArray();
				array.sort(this, new Comparator<PhpArray.Element>() {
					@Override
					public int compare(Element o1, Element o2) {
						try {
							return function.executeFunction(EntityAI.this, new AbstractLeekValue[] { o1.key(), o1.value(), o2.key(), o2.value() }).getInt(EntityAI.this);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
				return array;
			}
		} catch (RuntimeException e) {
			if (e.getCause() instanceof LeekRunException) {
				throw (LeekRunException) e.getCause();
			}
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue summon(AbstractLeekValue chip, AbstractLeekValue cell, AbstractLeekValue ai) throws Exception {

		int success = -1;

		Cell target = fight.getMap().getCell(cell.getInt(this));
		if (target == null)
			return LeekValueManager.getLeekIntValue(-1);

		if (ai.getType() != AbstractLeekValue.FUNCTION)
			return LeekValueManager.getLeekIntValue(-1);

		Chip template = mEntity.getChip(chip.getInt(this));
		if (template == null) {
			Chip ct = Chips.getChip(chip.getInt(this));

			this.addOperations(EntityAI.ERROR_LOG_COST);
			if (ct == null)
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip.getInt(this)) });
			else
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip.getInt(this)), ct.getName() });
			return LeekValueManager.getLeekIntValue(-1);
		}

		if (target != null && template != null) {
			success = fight.summonEntity(mEntity, target, template, (FunctionLeekValue) ai);
		}
		return LeekValueManager.getLeekIntValue(success);

	}

	public AbstractLeekValue getEntityTurnOrder(AbstractLeekValue value) throws LeekRunException {
		if (value.getType() == AbstractLeekValue.NULL)
			return LeekValueManager.getLeekIntValue(fight.getOrder().getEntityTurnOrder(mEntity));
		if (value.getType() == AbstractLeekValue.NUMBER) {
			Entity l = fight.getEntity(value.getInt(this));
			if (l != null && !l.isDead())
				return LeekValueManager.getLeekIntValue(fight.getOrder().getEntityTurnOrder(l));
		}
		return LeekValueManager.NULL;
	}

	public AbstractLeekValue reborn(AbstractLeekValue entity, AbstractLeekValue cell) throws Exception {

		int success = -1;

		Cell target = fight.getMap().getCell(cell.getInt(this));
		if (target == null) {
			return LeekValueManager.getLeekIntValue(-1);
		}

		Entity l = fight.getEntity(entity.getInt(this));
		if (l == null || !l.isDead()) {
			return LeekValueManager.getLeekIntValue(FightConstants.USE_RESURRECT_INVALID_ENTIITY.getIntValue());
		}

		Chip template = mEntity.getChip(FightConstants.CHIP_RESURRECTION.getIntValue());
		if (template == null) {

			Chip ct = Chips.getChip(FightConstants.CHIP_RESURRECTION.getIntValue());

			addOperations(ERROR_LOG_COST);
			if (ct == null)
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(FightConstants.CHIP_RESURRECTION) });
			else
				addSystemLog(LeekLog.WARNING, LeekLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(FightConstants.CHIP_RESURRECTION), ct.getName() });
			return LeekValueManager.getLeekIntValue(-1);
		}

		if (target != null && template != null) {
			success = fight.resurrectEntity(mEntity, target, template, l);
		}
		return LeekValueManager.getLeekIntValue(success);

	}

	public int getMapType() {
		// Nexus (the first map) is -1 so it's + 2
		return fight.getMap().getType() + 2;
	}

	public boolean weaponNeedLos(int id) {

		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null) {
			return false;
		}
		return template.getAttack().needLos();
	}

	public boolean chipNeedLos(int id) {

		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return false;
		}
		return chip.getAttack().needLos();
	}

	public AbstractLeekValue jsonEncode(AI ai, AbstractLeekValue object) {

		try {

			String json = JSON.toJSONString(object.toJSON(ai));
			addOperations(json.length() * 10);
			return new StringLeekValue(json);

		} catch (Exception e) {

			getLogs().addLog(leek(), LeekLog.ERROR, "Cannot encode object \"" + object.toString() + "\"");
			try {
				addOperations(100);
			} catch (Exception e1) {}
			return LeekValueManager.NULL;
		}
	}

	public AbstractLeekValue jsonDecode(String json) {

		try {

			AbstractLeekValue obj = LeekValueManager.parseJSON(JSON.parse(json), this);
			addOperations(json.length() * 10);
			return obj;

		} catch (Exception e) {

			getLogs().addLog(leek(), LeekLog.ERROR, "Cannot parse json \"" + json + "\"");
			try {
				addOperations(100);
			} catch (Exception e1) {}
			return LeekValueManager.NULL;
		}
	}

	@Override
	protected String getErrorString() {
		return null;
	}

	@Override
	protected String getAItring() {
		return null;
	}

	@Override
	public AbstractLeekValue runIA() throws Exception {
		return null;
	}

	@Override
	public int userFunctionCount(int id) {
		return 0;
	}

	@Override
	public boolean[] userFunctionReference(int id) {
		return null;
	}

	@Override
	public AbstractLeekValue userFunctionExecute(int id, AbstractLeekValue[] value) throws Exception {
		return null;
	}

	@Override
	public int anonymousFunctionCount(int id) {
		return 0;
	}

	@Override
	public boolean[] anonymousFunctionReference(int id) {
		return null;
	}
}
