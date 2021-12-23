package com.leekwars.generator.fight.entity;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.leekwars.generator.Censorship;
import com.leekwars.generator.FightConstants;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.EffectParameters;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.attack.effect.Effect;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.attack.weapons.Weapons;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionAIError;
import com.leekwars.generator.fight.action.ActionLama;
import com.leekwars.generator.fight.action.ActionLoseTP;
import com.leekwars.generator.fight.action.ActionSay;
import com.leekwars.generator.fight.action.ActionSetWeapon;
import com.leekwars.generator.fight.action.ActionShowCell;
import com.leekwars.generator.fight.bulbs.BulbTemplate;
import com.leekwars.generator.fight.bulbs.Bulbs;
import com.leekwars.generator.items.Items;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Pathfinding;
import com.leekwars.generator.scenario.EntityInfo;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.common.Error;

public class EntityAI extends AI {

	private static final String TAG = EntityAI.class.getSimpleName();

	protected static class LeekMessage {
		private final int mAuthor;
		private final int mType;
		private final Object mMessage;

		public LeekMessage(int author, int type, Object message) {
			mAuthor = author;
			mType = type;
			mMessage = message;
		}
	}

	// Un IARunner => égale plus ou moins à une fonction, une partie de code
	// Le LeekIA s'occuper de gérer les liens entre le code utilisateur et les fonctions du combat

	protected Entity mInitialEntity;
	protected Entity mEntity;
	protected Fight fight;
	protected final static boolean LOG_IA = true;
	protected int mBirthTurn = 1;

	protected long mIARunTime = 0;
	protected long mIACpuRunTime = 0;

	protected String ai_name = "";
	protected LeekLog logs;

	protected final List<LeekMessage> mMessages = new ArrayList<LeekMessage>();
	protected final List<String> mSays = new ArrayList<String>();

	protected boolean valid = false;

	public EntityAI(int instructions, int version) {
		super(instructions, version);
	}

	public EntityAI(Entity entity, LeekLog logs) {
		super(0, 11);
		setEntity(entity);
		this.logs = logs;
	}

	public static AIFile<?> resolve(Generator generator, EntityInfo entityInfo, Entity entity) {

		// No AI equipped : user error
		if (entityInfo.ai == null && entityInfo.ai_path == null) {
			entity.getLogs().addSystemLog(LeekLog.SERROR, Error.NO_AI_EQUIPPED);
			return null;
		}

		// Resolve first
		AIFile<?> file = null;
		try {
			if (entityInfo.ai_path != null) {
				file = LeekScript.getFileSystemResolver().resolve(entityInfo.ai_path, null);
			} else {
				var context = LeekScript.getResolver().createContext(entityInfo.farmer, entityInfo.aiOwner, entityInfo.ai_folder);
				file = LeekScript.getResolver().resolve(entityInfo.ai, context);
			}
		} catch (FileNotFoundException e) {
			// Failed to resolve, not normal
			generator.exception(e, entity.fight);
			entity.getLogs().addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA, new String[] { "Failed to resolve AI" });
		}
		return file;
	}

	/**
	 * Build an entity AI from parameters
	 * Add the correct errors in farmer logs
	 */
	public static EntityAI build(Generator generator, AIFile<?> file, Entity entity) {

		if (file == null) {
			return new EntityAI(entity, entity.getLogs());
		}

		Log.i(TAG, "Compile AI " + file.getPath() + "...");
		try {
			EntityAI ai = (EntityAI) LeekScript.compile(file, "com.leekwars.generator.fight.entity.EntityAI", generator.use_leekscript_cache);

			Log.i(TAG, "AI " + file.getPath() + " compiled!");
			ai.valid = true;
			ai.setEntity(entity);
			ai.setLogs(entity.getLogs());
			return ai;

		} catch (LeekScriptException e) {
			// Java compilation error : server error
			generator.exception(e, entity.fight, file.getId());
			if (e.getType() == Error.CODE_TOO_LARGE) {
				entity.getLogs().addSystemLog(LeekLog.SERROR, Error.CODE_TOO_LARGE, new String[] { e.getMessage() });
			} else if (e.getType() == Error.CODE_TOO_LARGE_FUNCTION) {
				entity.getLogs().addSystemLog(LeekLog.SERROR, Error.CODE_TOO_LARGE_FUNCTION, new String[] { e.getMessage() });
			} else {
				entity.getLogs().addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA, new String[] { e.getMessage() });
			}
			return new EntityAI(entity, entity.getLogs());

		} catch (LeekCompilerException e) {
			// Analyze error : AI is not valid, user error, no need to log it
			entity.getLogs().addSystemLog(LeekLog.SERROR, Error.INVALID_AI, new String[] { e.getMessage() });
			return new EntityAI(entity, entity.getLogs());

		} catch (Exception e) {
			// Other error : server error
			generator.exception(e, entity.fight, file.getId());
			entity.getLogs().addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA, new String[] { e.getMessage() });
			return new EntityAI(entity, entity.getLogs());
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
		mInitialEntity = entity;
		this.fight = entity.fight;
		this.randomGenerator = entity.fight.getRandom();
	}

	public void addSystemLog(int type, Error error, String[] parameters) {
		addSystemLog(type, error.ordinal(), parameters, Thread.currentThread().getStackTrace());
	}

	public void addSystemLog(int type, int error, String[] parameters) {
		addSystemLog(type, error, parameters, Thread.currentThread().getStackTrace());
	}

	public void addSystemLog(int type, Error error, StackTraceElement[] elements) {
		addSystemLog(type, error.ordinal(), new String[0], elements);
	}
	public void addSystemLog(int type, Error error, String[] parameters, StackTraceElement[] elements) {
		addSystemLog(type, error.ordinal(), parameters, elements);
	}

	public void addSystemLog(int type, int key, String[] parameters, StackTraceElement[] elements) {
		addOperationsNoCheck(AI.ERROR_LOG_COST);
		if (type == FarmerLog.WARNING)
			type = FarmerLog.SWARNING;
		else if (type == FarmerLog.ERROR)
			type = FarmerLog.SERROR;
		else if (type == FarmerLog.STANDARD)
			type = FarmerLog.SSTANDARD;

		if (this != null) {
			logs.addSystemLog(type, this.getErrorMessage(elements), key, parameters);
		}
	}

	private void putCells(List<Cell> ignore, ArrayLeekValue leeks_to_ignore) throws LeekRunException {
		for (Object value : leeks_to_ignore) {
			Cell l = fight.getMap().getCell(integer(value));
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

	public boolean isValid() {
		return valid;
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

			mEntity = mInitialEntity;
			runIA();

		} catch (StackOverflowError e) { // On suppose que c'est normal, ça vient de l'utilisateur

			// e.printStackTrace(System.out);
			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, Error.STACKOVERFLOW, e.getStackTrace());
			fight.statistics.stackOverflow(mEntity);
			fight.statistics.error(mEntity);
			// Pas de rethrow

		} catch (ArithmeticException e) { // On suppose que c'est normal, ça vient de l'utilisateur

			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { e.getMessage() }, e.getStackTrace());
			fight.statistics.error(mEntity);
			// Pas de rethrow

		} catch (LeekRunException e) { // Exception de l'utilisateur, normales

			// e.printStackTrace(System.out);
			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { e.getMessage() }, e.getStackTrace());
			fight.statistics.error(mEntity);

			if (e.getError() == LeekRunException.TOO_MUCH_OPERATIONS) {
				fight.statistics.tooMuchOperations(mEntity);
			}
			// Pas de rethrow

		} catch (OutOfMemoryError e) { // Plus de RAM, Erreur critique, on tente de sauver les meubles

			fight.log(new ActionAIError(mEntity));
			fight.statistics.error(mEntity);
			// Premierement on coupe l'IA incriminée
			valid = false;
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { "Out Of Memory" }, e.getStackTrace());
			System.out.println("Out Of Memory , Fight : " + fight.getId());
			fight.generator.exception(e, fight, id);
			throw e; // On rethrow tel quel

		} catch (RuntimeException e) { // Autre erreur, là c'est pas l'utilisateur

			e.printStackTrace(System.out);
			fight.statistics.error(mEntity);
			fight.log(new ActionAIError(mEntity));
			System.out.println("Erreur importante dans l'IA " + id + "  " + e.getMessage());
			e.printStackTrace();
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { "Generator Error" }, e.getStackTrace());
			fight.generator.exception(e, fight, id);
			throw e; // On rethrow tel quel

		} catch (Exception e) { // Autre erreur, là c'est pas l'utilisateur

			e.printStackTrace(System.out);
			fight.statistics.error(mEntity);
			fight.log(new ActionAIError(mEntity));
			System.out.println("Erreur importante dans l'IA " + id + "  " + e.getMessage());
			e.printStackTrace();
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { "Generator Error" }, e.getStackTrace());
			fight.generator.exception(e, fight, id);
			throw new RuntimeException("Erreur importante dans l'IA " + id + "  " + e.getMessage(), e);
		}

		mMessages.clear();

		long endTime = System.nanoTime();
		mIARunTime += (endTime - startTime);
	}

	// setWeapon
	public boolean setWeapon(int weapon_id) throws LeekRunException {
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
				fight.log(new ActionSetWeapon(mEntity, w));
				fight.log(new ActionLoseTP(mEntity, 1));
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
	public Object getMP(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getMP();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getMP();
		}
		return null;
	}

	public Object getTP(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getTP();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getTP();
			}
		}
		return null;
	}

	public Object getTotalMP(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getTotalMP();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getTotalMP();
			}
		}
		return null;
	}

	public Object getTotalTP(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getTotalTP();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getTotalTP();
			}
		}
		return null;
	}

	public Object getFrequency(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_FREQUENCY);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getStat(Entity.CHARAC_FREQUENCY);
			}
		}
		return null;
	}

	// Deprecated function in LeekScript
	public Object getCores(Object value) {
		return null;
	}

	public Object getStrength(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_STRENGTH);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getStat(Entity.CHARAC_STRENGTH);
			}
		}
		return null;
	}

	public Object isSummon(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.isSummon();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.isSummon();
		}
		return null;
	}

	public Object getBirthTurn(Object value) throws LeekRunException {
		if (value == null)
			return mBirthTurn;
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null && l.getAI() != null)
				return l.getAI().mBirthTurn;
		}
		return null;
	}

	public Object getBulbChips(Object value) throws LeekRunException {
		int id = integer(value);
		if (id > 0) {
			Chip chip = Chips.getChip(id);
			if (chip != null && chip.getAttack().getEffects().get(0).getId() == Effect.TYPE_SUMMON) {
				BulbTemplate template = Bulbs.getInvocationTemplate((int) chip.getAttack().getEffects().get(0).getValue1());
				if (template != null) {
					List<Chip> chips = template.getChips();
					ArrayLeekValue retour = new ArrayLeekValue();
					for (int i = 0; i < chips.size(); i++) {
						retour.put(this, i, chips.get(i).getId());
					}
					return retour;
				}
			}
		}
		return null;
	}

	public Object getType(Object value) throws LeekRunException {
		if (value == null) {
			return mEntity.getType() + 1;
		}
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getType() + 1;
			}
		}
		return null;
	}

	public Object getSummoner(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.isSummon() ? mEntity.getSummoner().getFId(): -1;
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.isSummon() ? l.getSummoner().getFId(): -1;
		}
		return null;
	}

	public boolean isStatic(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.isStatic();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.isStatic();
		}
		return false;
	}

	public Object getAgility(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_AGILITY);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getStat(Entity.CHARAC_AGILITY);
			}
		}
		return null;
	}

	public Object getResistance(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_RESISTANCE);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getStat(Entity.CHARAC_RESISTANCE);
			}
		}
		return null;
	}

	public Object getScience(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_SCIENCE);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getStat(Entity.CHARAC_SCIENCE);
			}
		}
		return null;
	}

	public Object getMagic(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_MAGIC);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getStat(Entity.CHARAC_MAGIC);
			}
		}
		return null;
	}

	public Object getWisdom(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_WISDOM);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null) {
				return l.getStat(Entity.CHARAC_WISDOM);
			}
		}
		return null;
	}

	public Object getLeekID(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getId();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getId();
		}
		return null;
	}

	public Object getAbsoluteShield(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_ABSOLUTE_SHIELD);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getStat(Entity.CHARAC_ABSOLUTE_SHIELD);
		}
		return null;
	}

	public Object getRelativeShield(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_RELATIVE_SHIELD);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getStat(Entity.CHARAC_RELATIVE_SHIELD);
		}
		return null;
	}

	public Object getDamageReturn(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getStat(Entity.CHARAC_DAMAGE_RETURN);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getStat(Entity.CHARAC_DAMAGE_RETURN);
		}
		return null;
	}

	public Object getTotalLife(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getTotalLife();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getTotalLife();
		}
		return null;
	}

	public Object getPower(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getPower();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getPower();
		}
		return null;
	}

	public Object getLevel(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getLevel();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getLevel();
		}
		return null;
	}

	public Object getName(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getName();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getName();
		}
		return null;
	}

	public Object getCell(Object value) throws LeekRunException {
		if (value == null) {
			if (mEntity.getCell() != null)
				return mEntity.getCell().getId();
		}
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null && l.getCell() != null)
				return l.getCell().getId();
		}
		return null;
	}

	public Object getWeapon(Object value) throws LeekRunException {
		if (value == null) {
			if (mEntity.getWeapon() != null)
				return mEntity.getWeapon().getId();
		}
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null && l.getWeapon() != null)
				return l.getWeapon().getId();
		}
		return null;
	}

	public Object getLife(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getLife();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getLife();
		}
		return null;
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

	public boolean isAlive(int id) {
		Entity l = fight.getEntity(id);
		if (l == null)
			return false;
		return !l.isDead();
	}

	public boolean isDead(int id) {
		Entity l = fight.getEntity(id);
		if (l == null)
			return false;
		return l.isDead();
	}

	public boolean say(String message) {
		if (mEntity.getTP() < 1) {
			return false;
		}
		mEntity.useTP(1);
		if (mEntity.saysTurn >= Entity.SAY_LIMIT_TURN) {
			return false;
		}
		mEntity.saysTurn++;
		if (message.length() > 200) {
			message = message.substring(0, 200);
		}
		message = Censorship.checkString(fight, message);
		fight.log(new ActionSay(mEntity, message));
		fight.log(new ActionLoseTP(mEntity, 1));
		mSays.add(message);
		fight.statistics.say(mEntity, message);
		return true;
	}

	public ArrayLeekValue getWeapons(Object value) throws LeekRunException {
		Entity l = null;
		if (value == null)
			l = mEntity;
		else if (value instanceof Number)
			l = fight.getEntity(((Number) value).intValue());
		if (l == null)
			return null;
		List<Weapon> weapons = l.getWeapons();
		ArrayLeekValue retour = new ArrayLeekValue();
		for (int i = 0; i < weapons.size(); i++) {
			retour.put(this, i, weapons.get(i).getId());
		}
		return retour;
	}

	public ArrayLeekValue getChips(Object value) throws LeekRunException {
		Entity l = null;
		if (value == null)
			l = mEntity;
		else if (value instanceof Number)
			l = fight.getEntity(((Number) value).intValue());
		if (l == null)
			return null;
		List<Chip> chips = l.getChips();
		ArrayLeekValue retour = new ArrayLeekValue();
		for (int i = 0; i < chips.size(); i++) {
			retour.put(this, i, chips.get(i).getId());
		}
		return retour;
	}

	public ArrayLeekValue getSummons(Object value) throws LeekRunException {
		Entity l = null;
		if (value == null)
			l = mEntity;
		else if (value instanceof Number)
			l = fight.getEntity(((Number) value).intValue());
		if (l == null)
			return null;
		List<Entity> summons = l.getSummons(false);
		ArrayLeekValue retour = new ArrayLeekValue();
		for (int i = 0; i < summons.size(); i++) {
			retour.put(this, i, summons.get(i).getFId());
		}
		return retour;
	}

	// ----- Fonctions Weapon -----
	public int useWeapon(int leek_id) throws LeekRunException {
		int success = -1;
		Entity target = fight.getEntity(leek_id);
		if (target != null && target != mEntity && !target.isDead()) {
			if (mEntity.getWeapon() == null) {
				addSystemLog(FarmerLog.WARNING, FarmerLog.NO_WEAPON_EQUIPED);
			}
			success = fight.useWeapon(mEntity, target.getCell());
		}
		return success;
	}

	public int useWeaponOnCell(int cell_id) throws LeekRunException {
		int success = -1;
		Cell target = fight.getMap().getCell(cell_id);
		if (target != null && target != mEntity.getCell()) {
			if (mEntity.getWeapon() == null) {
				addSystemLog(FarmerLog.WARNING, FarmerLog.NO_WEAPON_EQUIPED);
			}
			success = fight.useWeapon(mEntity, target);
		}
		return success;
	}

	public boolean canUseWeapon(Object value1, Object value2) throws LeekRunException {
		Entity target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		if (value2 == null) {
			target = fight.getEntity(integer(value1));
		} else {
			target = fight.getEntity(integer(value2));
			weapon = Weapons.getWeapon(integer(value1));
		}
		if (weapon == null)
			return false;
		if (target != null && target.getCell() != null) {
			return Pathfinding.canUseAttack(mEntity.getCell(), target.getCell(), weapon.getAttack());
		}
		return false;
	}

	public boolean canUseWeaponOnCell(Object value1, Object value2) throws LeekRunException {
		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		if (value2 == null) {
			target = fight.getMap().getCell(integer(value1));
		} else {
			target = fight.getMap().getCell(integer(value2));
			weapon = Weapons.getWeapon(integer(value1));
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
	public int getWeaponFail(int id) {
		return 0;
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

	public ArrayLeekValue getWeaponEffects(int id) throws LeekRunException {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return null;
		ArrayLeekValue retour = new ArrayLeekValue();
		for (EffectParameters e : template.getAttack().getEffects()) {
			ArrayLeekValue effect = new ArrayLeekValue();
			effect.push(this, e.getId());
			effect.push(this, e.getValue1());
			effect.push(this, e.getValue1() + e.getValue2());
			effect.push(this, e.getTurns());
			effect.push(this, e.getTargets());
			effect.push(this, e.getModifiers());
			retour.push(this, effect);
		}
		return retour;
	}

	public ArrayLeekValue getWeaponPassiveEffects(int id) throws LeekRunException {
		Weapon template = id == -1 ? (mEntity.getWeapon() == null ? null : mEntity.getWeapon()) : Weapons.getWeapon(id);
		if (template == null)
			return null;
		ArrayLeekValue retour = new ArrayLeekValue();
		for (EffectParameters e : template.getPassiveEffects()) {
			ArrayLeekValue effect = new ArrayLeekValue();
			effect.push(this, e.getId());
			effect.push(this, e.getValue1());
			effect.push(this, e.getValue1() + e.getValue2());
			effect.push(this, e.getTurns());
			effect.push(this, e.getTargets());
			effect.push(this, e.getModifiers());
			retour.push(this, effect);
		}
		return retour;
	}

	// ---- Fonctions Chip ----

	public int useChip(int chip_id, int leek_id) throws LeekRunException {

		int success = -1;
		Entity target = fight.getEntity(leek_id);
		Chip chip = mEntity.getChip(chip_id);

		if (chip == null) {
			Chip ct = Chips.getChip(chip_id);
			if (ct == null) {
				addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id) });
			} else {
				addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id), ct.getName() });
			}
		}
		if (target != null && chip != null && !target.isDead()) {
			success = fight.useChip(mEntity, target.getCell(), chip);
		}
		return success;
	}

	public int useChipOnCell(int chip_id, int cell_id) throws LeekRunException {

		int success = -1;
		Cell target = fight.getMap().getCell(cell_id);
		Chip template = mEntity.getChip(chip_id);

		if (template == null) {
			Chip ct = Chips.getChip(chip_id);
			if (ct == null) {
				addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id) });
			} else {
				addSystemLog(FarmerLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(chip_id), ct.getName() });
			}
		}
		if (target != null && template != null) {
			success = fight.useChip(mEntity, target, template);
		}
		return success;
	}

	public boolean canUseChipOnCell(int chip_id, int cell_id) throws LeekRunException {
		Cell target = fight.getMap().getCell(cell_id);
		Chip template = mEntity.getChip(chip_id);
		if (target != null && template != null && mEntity.getCell() != null) {
			return Pathfinding.canUseAttack(mEntity.getCell(), target, template.getAttack());
		}
		return false;
	}

	public boolean canUseChip(int chip_id, int leek_id) throws LeekRunException {
		Entity target = fight.getEntity(leek_id);
		Chip template = mEntity.getChip(chip_id);
		if (target != null && template != null && target.getCell() != null && mEntity.getCell() != null) {
			return Pathfinding.canUseAttack(mEntity.getCell(), target.getCell(), template.getAttack());
		}
		return false;
	}

	public Object getChipTargets(int chip_id, int cell_id) throws LeekRunException {

		Cell target = fight.getMap().getCell(cell_id);
		Chip template = Chips.getChip(chip_id);
		if (target != null && template != null) {
			ArrayLeekValue retour = new ArrayLeekValue();
			List<Entity> entities = template.getAttack().getWeaponTargets(fight, mEntity, fight.getMap().getCell(cell_id));
			for (Entity l : entities) {
				retour.push(this, l.getFId());
			}
			return retour;
		}
		return null;
	}

	public Object getCurrentCooldown(Object chip_id, Object v) throws LeekRunException {
		if (v == null) {
			Chip chipTemplate = Chips.getChip(integer(chip_id));
			return fight.getCooldown(mEntity, chipTemplate);
		}
		if (v instanceof Number) {
			var l = fight.getEntity(((Number) v).intValue());
			if (l != null) {
				Chip chipTemplate = Chips.getChip(integer(chip_id));
				return fight.getCooldown(l, chipTemplate);
			}
		}
		return null;
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

	public Object getChipMinRange(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return null;
		}
		return chip.getAttack().getMinRange();
	}

	public Object getChipMaxRange(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return null;
		}
		return chip.getAttack().getMaxRange();
	}

	// Deprecated : always 0
	public int getChipFail(int id) {
		return 0;
	}

	public Object getChipCost(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return null;
		}
		return chip.getCost();
	}

	public Object getC(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null) {
			return null;
		}
		return chip.getCost();
	}

	public boolean isInlineChip(int id) {
		Chip chip = Chips.getChip(id);
		if (chip == null)
			return false;
		return chip.getAttack().getLaunchType() == Attack.LAUNCH_TYPE_LINE;
	}

	public ArrayLeekValue getChipEffects(int id) throws LeekRunException {
		Chip chip = Chips.getChip(id);
		if (chip == null)
			return null;
		ArrayLeekValue retour = new ArrayLeekValue();
		for (EffectParameters e : chip.getAttack().getEffects()) {
			ArrayLeekValue effect = new ArrayLeekValue();
			effect.push(this, e.getId());
			effect.push(this, e.getValue1());
			effect.push(this, e.getValue1() + e.getValue2());
			effect.push(this, e.getTurns());
			effect.push(this, e.getTargets());
			effect.push(this, e.getModifiers());
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

	public Object getPathLength(Object c1, Object c2, Object leeks_to_ignore) throws LeekRunException {

		Cell cell1 = fight.getMap().getCell(integer(c1));
		if (cell1 == null)
			return null;
		Cell cell2 = fight.getMap().getCell(integer(c2));
		if (cell2 == null)
			return null;

		List<Cell> ignore = new ArrayList<Cell>();

		if (leeks_to_ignore instanceof ArrayLeekValue) {
			putCells(ignore, (ArrayLeekValue) leeks_to_ignore);
		}
		if (cell1 == cell2)
			return 0;

		List<Cell> path = fight.getMap().getPathBeetween(this, cell1, cell2, ignore);
		if (path == null)
			return null;
		return path.size();
	}

	public Object getPath(Object c1, Object c2, Object leeks_to_ignore) throws LeekRunException {

		Cell cell1 = fight.getMap().getCell(integer(c1));
		if (cell1 == null)
			return null;
		Cell cell2 = fight.getMap().getCell(integer(c2));
		if (cell2 == null)
			return null;

		List<Cell> ignore = new ArrayList<Cell>();

		if (leeks_to_ignore instanceof ArrayLeekValue) {
			for (var value : (ArrayLeekValue) leeks_to_ignore) {
				Cell l = fight.getMap().getCell(integer(value));
				if (l == null)
					continue;
				ignore.add(l);
			}
		} else if (leeks_to_ignore instanceof Number) {
			logs.addLog(FarmerLog.WARNING,
					"Attention, la fonction getPath(Cell start, Cell end, Leek leek_to_ignore) va disparaitre, il faut désormais utiliser un tableau de cellules à ignorer.");
			Entity l = fight.getEntity(integer(leeks_to_ignore));
			if (l != null && l.getCell() != null) {
				ignore.add(l.getCell());
			}
		}
		if (cell1 == cell2)
			return new ArrayLeekValue();

		List<Cell> path = fight.getMap().getPathBeetween(this, cell1, cell2, ignore);
		if (path == null)
			return null;
		ArrayLeekValue retour = new ArrayLeekValue();
		for (int i = 0; i < path.size(); i++) {
			retour.put(this, i, path.get(i).getId());
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

	public Object getCellFromXY(int x, int y) {
		Cell cell = fight.getMap().getCell(x + fight.getMap().getWidth() - 1, y);
		if (cell == null)
			return null;
		return cell.getId();
	}

	public Object getCellX(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return null;
		return cell.getX() - fight.getMap().getWidth() + 1;
	}

	public Object getCellY(int c) {
		Cell cell = fight.getMap().getCell(c);
		if (cell == null)
			return null;
		return cell.getY();
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

	public int getNearestEnemy() throws LeekRunException {
		if (mEntity.getCell() == null)
			return -1;
		List<Entity> entities = fight.getEnemiesEntities(mEntity.getTeam());

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

	public int getFarthestEnemy() throws LeekRunException {
		if (mEntity.getCell() == null)
			return -1;
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

	public String getTime() {
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		return df.format(fight.getDate()).toString();
	}

	public int getTimestamp() {
		return (int) (fight.getDate().getTime() / 1000);
	}

	public String getDate() {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		return df.format(fight.getDate()).toString();
	}

	public ArrayLeekValue getAllChips() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		int i = 0;
		for (Chip chip : Chips.getTemplates().values()) {
			retour.put(this, i++, chip.getId());
		}
		return retour;
	}

	public ArrayLeekValue getAllWeapons() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		int i = 0;
		for (Weapon weapon : Weapons.getTemplates().values()) {
			retour.put(this, i++, weapon.getId());
		}
		return retour;
	}

	public ArrayLeekValue getAllEffects() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		for (int i = 0; i < Effect.effects.length; ++i) {
			retour.put(this, i, i + 1);
		}
		return retour;
	}

	public ArrayLeekValue getAliveEnemies() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		int nb = 0;
		for (Entity e : fight.getAllEntities(false)) {
			if (e.getTeam() != mEntity.getTeam()) {
				retour.put(this, nb, e.getFId());
				nb++;
			}
		}
		return retour;
	}

	public int getNumAliveEnemies() throws LeekRunException {
		short count = 0;
		for (Entity e : fight.getAllEntities(false)) {
			if (e.getTeam() != mEntity.getTeam()) {
				count++;
			}
		}
		return count;
	}

	public ArrayLeekValue getDeadEnemies() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		int nb = 0;
		for (Entity e : fight.getAllEntities(true)) {
			if (e.getTeam() != mEntity.getTeam() && e.isDead()) {
				retour.put(this, nb, e.getFId());
				nb++;
			}
		}
		return retour;
	}

	public int getNumDeadEnemies() throws LeekRunException {
		short count = 0;
		for (Entity e : fight.getAllEntities(true)) {
			if (e.getTeam() != mEntity.getTeam() && e.isDead()) {
				count++;
			}
		}
		return count;
	}

	public ArrayLeekValue getEnemies() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		int nb = 0;
		for (Entity l : fight.getEnemiesEntities(mEntity.getTeam(), true)) {
			retour.put(this, nb, l.getFId());
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

	public int getNumEnemies() throws LeekRunException {
		return fight.getEnemiesEntities(mEntity.getTeam(), true).size();
	}

	public Object getAlliedTurret() {
		if (fight.getType() == Fight.TYPE_TEAM) {
			for (Entity e : fight.getTeamEntities(mEntity.getTeam(), true)) {
				if (e.getType() == Entity.TYPE_TURRET) return e.getFId();
			}
		}
		return null;
	}

	public Object getEnemyTurret() {
		if (fight.getType() == Fight.TYPE_TEAM) {
			for (Entity e : fight.getEnemiesEntities(mEntity.getTeam(), true)) {
				if (e.getType() == Entity.TYPE_TURRET) return e.getFId();
			}
		}
		return null;
	}

	public int getNearestAlly() throws LeekRunException {
		if (mEntity.getCell() == null)
			return -1;
		List<Entity> entities = fight.getTeamEntities(mEntity.getTeam());
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

	public int getFarthestAlly() throws LeekRunException {
		if (mEntity.getCell() == null) return -1;
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

	public ArrayLeekValue getAliveAllies() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		for (Entity l : fight.getTeamEntities(mEntity.getTeam())) {
			retour.push(this, l.getFId());
		}
		return retour;
	}

	public int getNumAliveAllies() throws LeekRunException {
		return fight.getTeamEntities(mEntity.getTeam()).size();
	}

	public ArrayLeekValue getDeadAllies() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		for (Entity l : fight.getTeamEntities(mEntity.getTeam(), true)) {
			if (l.isDead()) {
				retour.push(this, l.getFId());
			}
		}
		return retour;
	}

	public int getNumDeadAllies() throws LeekRunException {
		short nb = 0;
		for (Entity l : fight.getTeamEntities(mEntity.getTeam(), true)) {
			if (l.isDead())
				nb++;
		}
		return nb;
	}

	public ArrayLeekValue getAllies() throws LeekRunException {
		ArrayLeekValue retour = new ArrayLeekValue();
		for (Entity l : fight.getTeamEntities(mEntity.getTeam(), true)) {
			retour.push(this, l.getFId());
		}
		return retour;
	}

	public int getNumAllies() throws LeekRunException {
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

	public Object getWeaponTargets(Object value1, Object value2) throws LeekRunException {

		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2 == null) {
			target = fight.getMap().getCell(integer(value1));
		} else {
			weapon = Weapons.getWeapon(integer(value1));
			target = fight.getMap().getCell(integer(value2));
		}

		if (weapon == null)
			return null;
		if (target != null && mEntity.getCell() != null) {
			ArrayLeekValue retour = new ArrayLeekValue();
			List<Entity> leeks = weapon.getAttack().getWeaponTargets(fight, mEntity, target);
			for (Entity l : leeks) {
				retour.push(this, l.getFId());
			}
			return retour;
		}
		return null;
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
	 * @throws LeekRunException
	 */
	public Object getWeaponArea(Object value1, Object value2, Object value3) throws LeekRunException {
		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2 == null) {
			target = fight.getMap().getCell(integer(value1));
		} else {
			weapon = Weapons.getWeapon(integer(value1));
			target = fight.getMap().getCell(integer(value2));
		}

		Cell start_cell = mEntity.getCell();
		if (value3 != null) {
			start_cell = fight.getMap().getCell(integer(value3));
		}

		if (target == null)
			return null;
		// On récupère l'arme
		if (weapon == null)
			return null;
		// On vérifie que la cellule de départ existe
		if (start_cell == null)
			return null;

		var retour = new ArrayLeekValue();

		// On récupère les cellules touchées
		List<Cell> cells = weapon.getAttack().getTargetCells(start_cell, target);
		// On les met dans le tableau
		for (Cell cell : cells) {
			retour.push(this, cell.getId());
		}

		return retour;
	}

	public int getCellToUseWeapon(Object value1, Object value2, Object value3) throws LeekRunException {
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		Entity target = null;

		if (value2 == null) {
			target = fight.getEntity(integer(value1));
		} else {
			weapon = Weapons.getWeapon(integer(value1));
			target = fight.getEntity(integer(value2));
		}
		int cell = -1;
		if (target != null && target.getCell() != null && weapon != null) {

			ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
			if (value3 instanceof ArrayLeekValue) {
				putCells(cells_to_ignore, (ArrayLeekValue) value3);
			} else {
				cells_to_ignore.add(mEntity.getCell());
			}
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

	public int getCellToUseWeaponOnCell(Object value1, Object value2, Object value3) throws LeekRunException {

		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2 == null) {
			target = fight.getMap().getCell(integer(value1));
		} else {
			weapon = Weapons.getWeapon(integer(value1));
			target = fight.getMap().getCell(integer(value2));
		}
		int retour = -1;
		if (target != null && weapon != null) {

			ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
			if (value3 instanceof ArrayLeekValue) {
				putCells(cells_to_ignore, (ArrayLeekValue) value3);
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

	public int getCellToUseChip(Object chip, Object t, Object value3) throws LeekRunException {

		Entity target = fight.getEntity(integer(t));
		int cell = -1;
		if (target == null)
			return cell;
		Chip template = Chips.getChip(integer(chip));
		if (template == null)
			return cell;
		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof ArrayLeekValue) {
			putCells(cells_to_ignore, (ArrayLeekValue) value3);
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

	public int getCellToUseChipOnCell(Object chip, Object cell, Object value3) throws LeekRunException {

		int retour = -1;
		Cell target = fight.getMap().getCell(integer(cell));
		if (target == null)
			return integer(cell);
		Chip template = Chips.getChip(integer(chip));
		if (template == null)
			return integer(cell);

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof ArrayLeekValue) {
			putCells(cells_to_ignore, (ArrayLeekValue) value3);
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

	public int moveToward(int leek_id, int pm_to_use) throws LeekRunException {
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
					used_pm = fight.moveEntity(mEntity, path.subList(0, Math.min(path.size(), pm)));
				}
			}
		}
		return used_pm;
	}

	public int moveTowardCell(int cell_id, int pm_to_use) throws LeekRunException {
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
					used_pm = fight.moveEntity(mEntity, path.subList(0, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public int moveTowardLeeks(ArrayLeekValue leeks, int pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < leeks.size(); i++) {
				int value = integer(leeks.get(this, i));
				Entity l = fight.getEntity(value);
				if (l != null && !l.isDead())
					targets.add(l.getCell());
			}
			if (targets.size() != 0) {
				List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), targets);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path.subList(0, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public int moveTowardCells(ArrayLeekValue cells, int pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < cells.size(); i++) {
				int value = integer(cells.get(this, i).getValue());
				Cell c = fight.getMap().getCell(value);
				if (c != null)
					targets.add(c);
			}
			if (targets.size() != 0) {
				List<Cell> path = Pathfinding.getAStarPath(this, mEntity.getCell(), targets);
				if (path != null) {
					used_pm = fight.moveEntity(mEntity, path.subList(0, Math.min(pm, path.size())));
				}
			}
		}
		return used_pm;
	}

	public int moveAwayFrom(int leek_id, int pm_to_use) throws LeekRunException {
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

	public int moveAwayFromCell(int cell_id, int pm_to_use) throws LeekRunException {
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

	public int moveAwayFromLeeks(ArrayLeekValue leeks, int pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < leeks.size(); i++) {
				int value = integer(leeks.get(this, i).getValue());
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

	public int moveAwayFromCells(ArrayLeekValue leeks, int pm_to_use) throws LeekRunException {
		int pm = pm_to_use == -1 ? mEntity.getMP() : pm_to_use;
		if (pm > mEntity.getMP())
			pm = mEntity.getMP();
		int used_pm = 0;
		if (pm > 0) {
			List<Cell> targets = new ArrayList<Cell>();
			for (int i = 0; i < leeks.size(); i++) {
				int value = integer(leeks.get(this, i).getValue());
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

	public int moveAwayFromLine(int cell1, int cell2, int pm_to_use) throws LeekRunException {
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

	public int moveTowardLine(int cell1, int cell2, int pm_to_use) throws LeekRunException {
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
			if (target != null && target2 != null && mEntity.getCell() != null) {
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
		fight.log(new ActionLama(mEntity));
		fight.statistics.lama(mEntity);
	}

	public boolean sendTo(int target, int type, Object message) {
		if (target == mEntity.getId()) {
			return false;
		}
		Entity l = fight.getEntity(target);
		if (l == null) {
			return false;
		}
		if (l.getAI() != null)
			l.getAI().addMessage(new LeekMessage(mEntity.getFId(), type, message));
		return true;
	}

	public void sendAll(int type, Object message) {
		for (Entity l : fight.getTeamEntities(mEntity.getTeam())) {
			if (l.getId() == mEntity.getId())
				continue;
			if (l.getAI() != null)
				l.getAI().addMessage(new LeekMessage(mEntity.getFId(), type, message));
		}
	}

	public Object getMessages(int target_leek) throws LeekRunException {

		// On récupere le leek ciblé
		Entity l = mEntity;
		if (target_leek != -1 && target_leek != l.getFId()) {
			l = fight.getEntity(target_leek);
			if (l == null || mEntity.getTP() < 1) {
				return null;
			}
			mEntity.useTP(1);
			fight.log(new ActionLoseTP(mEntity, 1));
		}

		// On crée le tableau de retour
		EntityAI lia = l.getAI();
		ArrayLeekValue messages = new ArrayLeekValue();

		// On y ajoute les messages
		if (lia != null) {
			for (LeekMessage message : lia.mMessages) {
				ArrayLeekValue m = new ArrayLeekValue();
				m.put(this, 0, message.mAuthor);
				m.put(this, 1, message.mType);
				m.put(this, 2, LeekOperations.clone(this, message.mMessage));
				messages.push(this, m);
			}
		}
		return messages;
	}

	public Object getEffects(Object value) throws LeekRunException {
		Entity l = null;
		if (value == null) {
			l = mEntity;
		} else if (value instanceof Number) {
			l = fight.getEntity(integer(value));
		}
		if (l == null) {
			return null;
		}
		ArrayLeekValue retour = new ArrayLeekValue();
		int i = 0;
		for (Effect effect : l.getEffects()) {
			retour.put(this, i, effect.getLeekValue(this));
			i++;
		}
		return retour;
	}

	public Object getLaunchedEffects(Object value) throws LeekRunException {
		Entity l = null;
		if (value == null) {
			l = mEntity;
		} else if (value instanceof Number) {
			l = fight.getEntity(integer(value));
		}
		if (l == null) {
			return null;
		}
		ArrayLeekValue retour = new ArrayLeekValue();
		int i = 0;
		for (Effect effect : l.getLaunchedEffects()) {
			retour.put(this, i, effect.getLeekValue(this));
			i++;
		}
		return retour;
	}

	public Object getPassiveEffects(Object value) throws LeekRunException {
		Entity l = null;
		if (value == null) {
			l = mEntity;
		} else if (value instanceof Number) {
			l = fight.getEntity(integer(value));
		}
		if (l == null) {
			return null;
		}
		ArrayLeekValue retour = new ArrayLeekValue();
		for (EffectParameters e : l.getPassiveEffects()) {
			ArrayLeekValue effect = new ArrayLeekValue();
			effect.push(this, e.getId());
			effect.push(this, e.getValue1());
			effect.push(this, e.getValue1() + e.getValue2());
			effect.push(this, e.getTurns());
			effect.push(this, e.getTargets());
			effect.push(this, e.getModifiers());
			retour.push(this, effect);
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
	 * @throws LeekRunException
	 */
	public Object getChipArea(Object value1, Object value2, Object value3) throws LeekRunException {

		Cell start_cell = mEntity.getCell();
		if (value3 != null) {
			start_cell = fight.getMap().getCell(integer(value3));
		}
		// On vérifie que la cellule de départ existe

		if (start_cell == null)
			return null;
		// On récupère la cellule
		Cell c = fight.getMap().getCell(integer(value2));
		if (c == null || mEntity.getCell() == null)
			return null;
		// On récupère le sort
		Chip template = Chips.getChip(integer(value1));

		if (template == null)
			return null;

		var retour = new ArrayLeekValue();
		// On récupère les cellules touchées
		List<Cell> cells = template.getAttack().getTargetCells(start_cell, c);
		// On les met dans le tableau
		if (cells != null) {
			for (Cell cell : cells) {
				retour.push(this, cell.getId());
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
	 * @throws LeekRunException
	 */
	public Object getCellsToUseWeapon(Object value1, Object value2, Object value3) throws LeekRunException {
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();
		Entity target = null;

		if (value2 == null) {
			target = fight.getEntity(integer(value1));
		} else {
			weapon = Weapons.getWeapon(integer(value1));
			target = fight.getEntity(integer(value2));
		}

		if (target == null || target.getCell() == null || weapon == null || mEntity.getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof ArrayLeekValue) {
			putCells(cells_to_ignore, (ArrayLeekValue) value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target.getCell(), cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, cell.getId());
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
	 * @throws LeekRunException
	 */
	public Object getCellsToUseWeaponOnCell(Object value1, Object value2, Object value3) throws LeekRunException {

		Cell target = null;
		Weapon weapon = (mEntity.getWeapon() == null) ? null : mEntity.getWeapon();

		if (value2 == null) {
			target = fight.getMap().getCell(integer(value1));
		} else {
			weapon = Weapons.getWeapon(integer(value1));
			target = fight.getMap().getCell(integer(value2));
		}

		if (target == null || weapon == null || mEntity.getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof ArrayLeekValue) {
			putCells(cells_to_ignore, (ArrayLeekValue) value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(weapon.getAttack(), target, cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, cell.getId());
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
	 * @throws LeekRunException
	 */
	public Object getCellsToUseChip(Object chip_id, Object target_leek_id, Object value3) throws LeekRunException {

		Entity target = fight.getEntity(integer(target_leek_id));
		// On récupère le sort
		Chip template = Chips.getChip(integer(chip_id));
		if (target == null || target.getCell() == null || template == null || mEntity.getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof ArrayLeekValue) {
			putCells(cells_to_ignore, (ArrayLeekValue) value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target.getCell(), cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, cell.getId());
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
	 * @throws LeekRunException
	 */
	public Object getCellsToUseChipOnCell(Object chip_id, Object target_cell_id, Object value3) throws LeekRunException {

		Cell target = fight.getMap().getCell(integer(target_cell_id));
		// On récupère le sort
		Chip template = Chips.getChip(integer(chip_id));
		if (target == null || template == null || mEntity.getCell() == null)
			return null;

		ArrayList<Cell> cells_to_ignore = new ArrayList<Cell>();
		if (value3 instanceof ArrayLeekValue) {
			putCells(cells_to_ignore, (ArrayLeekValue) value3);
		} else
			cells_to_ignore.add(mEntity.getCell());
		List<Cell> possible = Pathfinding.getPossibleCastCellsForTarget(template.getAttack(), target, cells_to_ignore);

		ArrayLeekValue retour = new ArrayLeekValue();
		if (possible != null) {
			for (Cell cell : possible) {
				retour.push(this, cell.getId());
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
	public Object getNearestEnemyTo(int leek_id) {

		List<Entity> entities = fight.getEnemiesEntities(mEntity.getTeam());

		Entity entity = fight.getEntity(leek_id);
		if (entity == null || entity.getCell() == null)
			return null;
		int dist = -1;
		Entity nearest = null;
		for (Entity l : entities) {
			if (l.isDead())
				continue;
			if (entity == l)
				continue;
			if (l.getCell() == null)
				continue;
			int d = Pathfinding.getDistance2(entity.getCell(), l.getCell());
			if (d < dist || dist == -1) {
				dist = d;
				nearest = l;
			}
		}
		return nearest == null ? null : nearest.getFId();
	}

	/**
	 * Retourne l'ennemi le plus proche de la cellule fourni en paramètre
	 *
	 * @param cell_id
	 *            Cellule cible
	 * @return Ennemi le plus proche
	 */
	public Object getNearestEnemyToCell(int cell_id) {

		List<Entity> entities = fight.getEnemiesEntities(mEntity.getTeam());

		Cell cell = fight.getMap().getCell(cell_id);
		if (cell == null)
			return null;
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
		return nearest == null ? null : nearest.getFId();
	}

	/**
	 * Retourne l'allié le plus proche du leek fournis en paramètre
	 *
	 * @param leek_id
	 *            Leek cible
	 * @return Allié le plus proche
	 */
	public Object getNearestAllyTo(int leek_id) {
		List<Entity> entities = fight.getTeamEntities(mEntity.getTeam() == 2 ? 2 : 1);
		Entity entity = fight.getEntity(leek_id);
		if (entity == null || entity.getCell() == null)
			return null;
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
		return nearest == null ? null : nearest.getFId();
	}

	/**
	 * Retourne l'allié le plus proche de la cellule fournie en paramètre
	 *
	 * @param cell_id
	 *            Cellule cible
	 * @return C le plus proche
	 */
	public Object getNearestAllyToCell(int cell_id) {
		List<Entity> entities = fight.getTeamEntities(mEntity.getTeam() == 2 ? 2 : 1);
		Cell cell = fight.getMap().getCell(cell_id);
		if (cell == null)
			return null;
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

		return nearest == null ? null : nearest.getFId();
	}

	public Object lineOfSight(Object start, Object end, Object ignore) throws LeekRunException {

		Cell s = fight.getMap().getCell(integer(start));
		Cell e = fight.getMap().getCell(integer(end));

		if (s == null || e == null)
			return null;

		if (ignore instanceof Number) {

			Entity l = fight.getEntity(integer(ignore));
			List<Cell> cells = new ArrayList<Cell>();
			if (l != null && l.getCell() != null)
				cells.add(l.getCell());
			return Pathfinding.verifyLoS(s, e, null, cells);

		} else if (ignore instanceof ArrayLeekValue) {

			List<Cell> cells = new ArrayList<Cell>();
			if (mEntity.getCell() != null)
				cells.add(mEntity.getCell());
			for (var value : (ArrayLeekValue) ignore) {
				if (value.getValue() instanceof Number) {
					Entity l = fight.getEntity(integer(value));
					if (l != null && l.getCell() != null) {
						cells.add(l.getCell());
					}
				}
			}
			return Pathfinding.verifyLoS(s, e, null, cells);

		} else {

			List<Cell> cells = new ArrayList<Cell>();
			cells.add(mEntity.getCell());
			return Pathfinding.verifyLoS(s, e, null, cells);
		}
	}

	public Object getObstacles() throws LeekRunException {
		Cell[] cells = fight.getMap().getObstacles();
		ArrayLeekValue retour = new ArrayLeekValue();
		if (cells == null)
			return retour;

		// On ajoute les caces
		for (Cell c : cells)
			retour.push(this, c.getId());

		return retour;
	}

	public Object mark(Object cell, Object color, Object duration) throws LeekRunException {
		int d = 1;
		int col = 1;
		int[] cel = null;
		if (cell instanceof Number) {
			int id = integer(cell);
			if (fight.getMap().getCell(id) == null)
				return false;
			cel = new int[] { integer(cell) };
		} else if (cell instanceof ArrayLeekValue) {
			cel = new int[((ArrayLeekValue) cell).size()];
			int i = 0;
			for (var value : (ArrayLeekValue) cell) {
				if (fight.getMap().getCell(integer(value)) == null)
					continue;
				cel[i] = integer(value);
				i++;
			}
			if (i == 0)
				return false;
		} else
			return false;

		if (color instanceof Number)
			col = integer(color);
		if (duration instanceof Number)
			d = integer(duration);

		logs.addCell(cel, col, d);

		return true;
	}

	public void clearMarks() throws LeekRunException {
		logs.addClearCells();
	}

	public Object markText(Object cell, Object text, Object color, Object duration) throws LeekRunException {
		int d = 1;
		int col = 0xffffff;
		int[] cel = null;
		if (cell instanceof Number) {
			int id = integer(cell);
			if (fight.getMap().getCell(id) == null)
				return false;
			cel = new int[] { integer(cell) };
		} else if (cell instanceof ArrayLeekValue) {
			cel = new int[((ArrayLeekValue) cell).size()];
			int i = 0;
			for (var value : (ArrayLeekValue) cell) {
				if (fight.getMap().getCell(integer(value)) == null)
					continue;
				cel[i] = integer(value);
				i++;
			}
			if (i == 0)
				return false;
		} else
			return false;

		if (color instanceof Number)
			col = integer(color);
		if (duration instanceof Number)
			d = integer(duration);

		String userText = string(text);
		String finalText = userText.substring(0, Math.min(userText.length(), 10));

		logs.addCellText(cel, finalText, col, d);

		return true;
	}

	public void pause() {
		logs.addPause();
	}

	public Object show(Object cell, Object color) throws LeekRunException {
		int cell_id = 1;
		int col = 0xFFFFFF;
		if (cell instanceof Number)
			cell_id = ((Number) cell).intValue();
		else
			return false;
		if (fight.getMap().getCell(cell_id) == null)
			return false;

		if (color instanceof Number)
			col = ((Number) color).intValue();

		if (mEntity.getTP() < 1) {
			return false;
		}
		if (mEntity.showsTurn >= Entity.SHOW_LIMIT_TURN) {
			return false;
		}
		mEntity.useTP(1);
		mEntity.showsTurn++;
		fight.log(new ActionLoseTP(mEntity, 1));

		fight.log(new ActionShowCell(mEntity, cell_id, col));
		fight.statistics.show(mEntity, cell_id);

		return true;
	}

	public int color(Object red, Object green, Object blue) throws LeekRunException {
		return ((integer(red) & 255) << 16) | ((integer(green) & 255) << 8) | (integer(blue) & 255);
	}

	public Object listen() throws LeekRunException {
		ArrayLeekValue values = new ArrayLeekValue();
		for (Entity l : fight.getAllEntities(false)) {
			if (l == null || l == mEntity || l.getAI() == null)
				continue;
			for (String say : l.getAI().getSays()) {
				ArrayLeekValue s = new ArrayLeekValue();
				s.push(this, l.getFId());
				s.push(this, say);
				values.push(this, s);
			}
		}
		return values;
	}

	public boolean isWeapon(int id) {
		Integer i = Items.getType(id);
		if (i == null) {
			return false;
		}
		return Items.getType(id) == Items.TYPE_WEAPON;
	}

	public boolean isChip(int id) {
		Integer i = Items.getType(id);
		if (i == null) {
			return false;
		}
		return Items.getType(id) == Items.TYPE_CHIP;
	}

	public Object getWeaponLaunchType(int weapon_id) throws LeekRunException {
		Weapon template = null;
		if (weapon_id == -1) {
			template = mEntity.getWeapon();
		} else {
			template = Weapons.getWeapon(integer(weapon_id));
		}
		if (template == null)
			return null;
		return (int) template.getAttack().getLaunchType();
	}

	public Object getChipLaunchType(Object chip_id) throws LeekRunException {
		Chip template = Chips.getChip(integer(chip_id));
		if (template == null)
			return null;
		return (int) template.getAttack().getLaunchType();
	}

	public Object getAIName(Object value) throws LeekRunException {
		if (value == null) {
			return mEntity.getAIName();
		}
		if (value instanceof Number) {
			Entity l = fight.getEntity(integer(value));
			if (l != null)
				return l.getAIName();
		}
		return null;
	}

	public Object getTeamName(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getTeamName();
		if (value instanceof Number) {
			Entity l = fight.getEntity(integer(value));
			if (l != null && l.getTeamName() != null)
				return l.getTeamName();
		}
		return null;
	}

	public Object getFarmerName(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getFarmerName();
		if (value instanceof Number) {
			Entity l = fight.getEntity(integer(value));
			if (l != null)
				return l.getFarmerName();
		}
		return null;
	}

	public Object getFarmerCountry(Object value) throws LeekRunException {
		if (value == null) {
			return mEntity.getFarmerCountry();
		}
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getFarmerCountry();
		}
		return null;
	}

	public Object getFarmerId(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getFarmer();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getFarmer();
		}
		return null;
	}

	public Object getTeamId(Object value) throws LeekRunException {
		if (value == null)
			return mEntity.getTeamId();
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getTeamId();
		}
		return null;
	}

	public Object getWeaponArea(Object value) throws LeekRunException {
		if (value instanceof Number) {
			Weapon weapon = Weapons.getWeapon(integer(value));
			if (weapon != null) {
				return weapon.getAttack().getArea();
			}
		}
		return null;
	}

	public Object getChipArea(Object value) throws LeekRunException {
		if (value instanceof Number) {
			Chip template = Chips.getChip(integer(value));
			if (template != null) {
				return template.getAttack().getArea();
			}
		}
		return null;
	}

	public Object getAIId(Object value) throws LeekRunException {
		if (value == null)
			return id;
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null)
				return l.getAI().getId();
		}
		return null;
	}

	public ArrayLeekValue getRegisters() throws LeekRunException {
		Map<String, String> registers;
		if (mEntity.isSummon()) {
			registers = mEntity.getSummoner().getAllRegisters();
		} else {
			registers = mEntity.getAllRegisters();
		}
		ArrayLeekValue array = new ArrayLeekValue();
		for (Entry<String, String> e : registers.entrySet()) {
			array.put(this, e.getKey(), e.getValue());
		}
		return array;
	}

	public Object getRegister(Object key) throws LeekRunException {
		String keyString = string(key);
		String register;
		if (mEntity.isSummon()) {
			register = mEntity.getSummoner().getRegister(keyString);
		} else {
			register = mEntity.getRegister(keyString);
		}
		if (register == null) {
			return null;
		}
		return register;
	}

	public boolean setRegister(Object key, Object value) throws LeekRunException {
		String keyString = string(key);
		String valueString = string(value);
		boolean r;
		if (mEntity.isSummon()) {
			r = mEntity.getSummoner().setRegister(keyString, valueString);
		} else {
			r = mEntity.setRegister(keyString, valueString);
		}
		return r;
	}

	public void deleteRegister(Object key) throws LeekRunException {
		String keyString = string(key);
		if (mEntity.isSummon()) {
			mEntity.getSummoner().deleteRegister(keyString);
		} else {
			mEntity.deleteRegister(keyString);
		}
	}

	public int summon(Object chip, Object cell, Object ai) throws LeekRunException {

		int success = -1;

		Cell target = fight.getMap().getCell(integer(cell));
		if (target == null)
			return -1;

		if (!(ai instanceof FunctionLeekValue))
			return -1;

		Chip template = mEntity.getChip(integer(chip));
		if (template == null) {
			Chip ct = Chips.getChip(integer(chip));
			if (ct == null)
				addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(integer(chip)) });
			else
				addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(integer(chip)), ct.getName() });
			return -1;
		}

		if (target != null && template != null) {
			success = fight.summonEntity(mEntity, target, template, (FunctionLeekValue) ai);
		}
		return success;
	}

	public Object getEntityTurnOrder(Object value) throws LeekRunException {
		if (value == null)
			return fight.getOrder().getEntityTurnOrder(mEntity);
		if (value instanceof Number) {
			Entity l = fight.getEntity(((Number) value).intValue());
			if (l != null && !l.isDead())
				return fight.getOrder().getEntityTurnOrder(l);
		}
		return null;
	}

	public int reborn(Object entity, Object cell) throws LeekRunException {

		int success = -1;

		Cell target = fight.getMap().getCell(integer(cell));
		if (target == null) {
			return -1;
		}

		Entity l = fight.getEntity(integer(entity));
		if (l == null || !l.isDead()) {
			return FightConstants.USE_RESURRECT_INVALID_ENTIITY.getIntValue();
		}

		Chip template = mEntity.getChip(FightConstants.CHIP_RESURRECTION.getIntValue());
		if (template == null) {

			Chip ct = Chips.getChip(FightConstants.CHIP_RESURRECTION.getIntValue());

			if (ct == null)
				addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(FightConstants.CHIP_RESURRECTION) });
			else
				addSystemLog(LeekLog.WARNING, FarmerLog.CHIP_NOT_EXISTS, new String[] { String.valueOf(FightConstants.CHIP_RESURRECTION), ct.getName() });
			return -1;
		}

		if (target != null && template != null) {
			success = fight.resurrectEntity(mEntity, target, template, l);
		}
		return success;
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

	@Override
	public Object runIA() throws LeekRunException {
		return null;
	}
}
