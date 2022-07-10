package com.leekwars.generator.fight.entity;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.action.ActionAIError;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.scenario.EntityInfo;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.common.Error;

public class EntityAI extends AI {

	private static final String TAG = EntityAI.class.getSimpleName();

	public static class LeekMessage {

		private final long mAuthor;
		private final long mType;
		private final Object mMessage;

		public LeekMessage(long author, long type, Object message) {
			mAuthor = author;
			mType = type;
			mMessage = message;
		}

		public long getAuthor() {
			return mAuthor;
		}

		public long getType() {
			return mType;
		}

		public Object getMessage() {
			return mMessage;
		}

		public GenericArrayLeekValue getArray(EntityAI ai) throws LeekRunException {
			var m = ai.newArray();
			m.push(ai, (long) mAuthor);
			m.push(ai, (long) mType);
			m.pushNoClone(ai, LeekOperations.clone(ai, mMessage));
			return m;
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
	private boolean isFirstRuntimeError = false;

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
				file.setVersion(entityInfo.ai_version);
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
			file.setJavaClass("AI_" + file.getId());
			file.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
			EntityAI ai = (EntityAI) file.compile(generator.use_leekscript_cache);

			Log.i(TAG, "AI " + file.getPath() + " compiled!");
			ai.valid = true;
			ai.setEntity(entity);
			ai.setLogs(entity.getLogs());
			return ai;

		} catch (LeekScriptException e) {
			// Java compilation error : server error
			if (e.getType() == Error.CODE_TOO_LARGE) {
				entity.getLogs().addSystemLog(LeekLog.SERROR, Error.CODE_TOO_LARGE, new String[] { e.getMessage() });
			} else if (e.getType() == Error.CODE_TOO_LARGE_FUNCTION) {
				entity.getLogs().addSystemLog(LeekLog.SERROR, Error.CODE_TOO_LARGE_FUNCTION, new String[] { e.getMessage() });
			} else {
				generator.exception(e, entity.fight, file.getId());
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
		opsNoCheck(AI.ERROR_LOG_COST);
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

	public long getIARunTime() {
		return mIARunTime;
	}

	public long getIACpuRunTime() {
		return mIACpuRunTime / 1000000;
	}

	public boolean isValid() {
		return valid;
	}

	public LeekLog getLogs() {
		return logs;
	}

	public void addMessage(LeekMessage leekMessage) {
		if (mMessages.size() > 200) {
			return;
		}
		mMessages.add(leekMessage);
	}

	public List<String> getSays() {
		return mSays;
	}

	public void setFight(Fight fight) {
		this.fight = fight;
		logs.setLogs(fight.getActions());
	}

	public void runTurn(int turn) {

		long startTime = System.nanoTime();

		mSays.clear();

		try {

			mEntity = mInitialEntity;
			if (turn == 1) {
				staticInit();
			}
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

		} catch (ConcurrentModificationException e) { // On suppose que c'est normal, ça vient de l'utilisateur

			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, Error.MODIFICATION_DURING_ITERATION, new String[0], e.getStackTrace());
			fight.statistics.error(mEntity);
			// Pas de rethrow

		} catch (LeekRunException e) { // Exception de l'utilisateur, normales

			if (e.getError() == Error.ENTITY_DIED) {
				// OK, c'est normal
			} else {

				// e.printStackTrace(System.out);
				fight.log(new ActionAIError(mEntity));
				addSystemLog(LeekLog.ERROR, e.getError(), new String[] { e.getMessage() }, e.getStackTrace());
				fight.statistics.error(mEntity);

				if (e.getError() == Error.TOO_MUCH_OPERATIONS) {
					fight.statistics.tooMuchOperations(mEntity);
				}
				// Pas de rethrow
			}

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

			// e.printStackTrace(System.out);
			fight.statistics.error(mEntity);
			fight.log(new ActionAIError(mEntity));
			System.out.println("Erreur importante dans l'IA " + id + "  " + e.getMessage());
			e.printStackTrace(System.out);
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { "Generator Error" }, e.getStackTrace());
			if (isFirstRuntimeError) {
				fight.generator.exception(e, fight, id);
				isFirstRuntimeError = false;
			}
			// throw e; // On rethrow tel quel

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

	public Entity getEntity() {
		return mEntity;
	}



	public Fight getFight() {
		return fight;
	}

	public void putCells(List<Cell> ignore, GenericArrayLeekValue leeks_to_ignore) throws LeekRunException {
		if (leeks_to_ignore instanceof ArrayLeekValue) {
			for (var value : (ArrayLeekValue) leeks_to_ignore) {
				var l = fight.getMap().getCell(integer(value));
				if (l == null) continue;
				ignore.add(l);
			}
		} else {
			for (var value : (LegacyArrayLeekValue) leeks_to_ignore) {
				Cell l = fight.getMap().getCell(integer(value.getValue()));
				if (l == null) continue;
				ignore.add(l);
			}
		}
	}

	public boolean isTest() {
		return mEntity.getId() < 0;
	}

	@Override
	public Object runIA() throws LeekRunException {
		return null;
	}

	public int getBirthTurn() {
		return mBirthTurn;
	}

	public List<LeekMessage> getMessages() {
		return mMessages;
	}
}
