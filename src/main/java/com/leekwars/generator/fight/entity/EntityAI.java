package com.leekwars.generator.fight.entity;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;

import com.leekwars.generator.action.ActionAIError;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.effect.EffectParameters;
import com.leekwars.generator.entity.Say;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.State;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.scenario.EntityInfo;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;
import leekscript.compiler.LeekScriptException;
import leekscript.compiler.Options;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;
import leekscript.runner.Session;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.GenericArrayLeekValue;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.AILog;
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

	protected long mIARunTime = 0;
	protected long mIACpuRunTime = 0;

	protected String ai_name = "";

	protected final List<LeekMessage> mMessages = new ArrayList<LeekMessage>();
	protected final List<Say> mSays = new ArrayList<>();

	protected boolean valid = false;
	private boolean isFirstRuntimeError = true;
	private boolean staticInitialized = false;

	public EntityAI(int instructions, int version) {
		super(instructions, version);
	}

	public EntityAI(Entity entity, LeekLog logs) {
		super(0, LeekScript.LATEST_VERSION, logs);
		setEntity(entity);
	}

	public static AIFile resolve(Generator generator, EntityInfo entityInfo, Entity entity) {

		// No AI equipped : user error
		if (entityInfo.ai == null && entityInfo.ai_path == null) {
			((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.NO_AI_EQUIPPED);
			return null;
		}

		// Resolve first
		AIFile file = null;
		try {
			if (entityInfo.ai_path != null) {
				file = LeekScript.getNativeFileSystem().getRoot().resolve(entityInfo.ai_path);
				file.setVersion(entityInfo.ai_version, entityInfo.ai_strict);
			} else {
				// Accès au dossier ?
				var folder = LeekScript.getFileSystem().getFolderById(entityInfo.ai_folder, entityInfo.aiOwner);
				if (folder == null) {
					throw new FileNotFoundException();
				}
				file = folder.resolve(entityInfo.ai);
			}
		} catch (FileNotFoundException e) {
			// Failed to resolve, not normal
			generator.exception(e, (Fight) entity.getFight());
			((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA, new String[] { "Failed to resolve AI" });
		}
		return file;
	}

	/**
	 * Build an entity AI from parameters
	 * Add the correct errors in farmer logs
	 */
	public static EntityAI build(Generator generator, AIFile file, Entity entity) {

		if (file == null) {
			return new EntityAI(entity, (LeekLog) entity.getLogs());
		}

		Log.i(TAG, "Compile AI " + file.getPath() + " (id " + file.getId() + ")...");
		EntityAI ai = null;
		try {
			// Synchro sur le fichier pour ne pas compiler deux fois la même IA en parallèle
			synchronized (file) {
				file.setJavaClass("AI_" + file.getId());
				file.setRootClass("com.leekwars.generator.fight.entity.EntityAI");
				var options = new Options(file.getVersion(), file.isStrict(), generator.use_leekscript_cache, true, null, true);
				ai = (EntityAI) file.compile(options);
			}

			Log.i(TAG, "AI " + file.getPath() + " compiled!");
			ai.valid = true;
			ai.setEntity(entity);
			ai.setLogs((LeekLog) entity.getLogs());
			// System.out.println("Coeurs = " + entity.getCores() + " RAM = " + entity.getRAM());
			ai.setMaxRAM(Math.min(50, entity.getRAM()) * 8_000_000);
			ai.setMaxOperations(entity.getCores() * 1_000_000);
			return ai;

		} catch (LeekScriptException e) {
			// Java compilation error : server error
			if (e.getType() == Error.CODE_TOO_LARGE) {
				((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.CODE_TOO_LARGE, new String[] { e.getLocation() });
			} else if (e.getType() == Error.CODE_TOO_LARGE_FUNCTION) {
				((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.CODE_TOO_LARGE_FUNCTION, new String[] { e.getLocation() });
			} else {
				generator.exception(e, (Fight) entity.getFight(), entity.getFarmer(), file);
				((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA, new String[] { e.getLocation() });
			}
			return new EntityAI(entity, (LeekLog) entity.getLogs());

		} catch (LeekCompilerException e) {
			// Analyze error : AI is not valid, user error, no need to log it
			((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.INVALID_AI, new String[] { e.getMessage() });
			return new EntityAI(entity, (LeekLog) entity.getLogs());

		} catch (Exception e) {
			// Other error : server error
			generator.exception(e, (Fight) entity.getFight(), entity.getFarmer(), file);
			((LeekLog) entity.getLogs()).addSystemLog(LeekLog.SERROR, Error.COMPILE_JAVA);
			return new EntityAI(entity, (LeekLog) entity.getLogs());
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
		this.fight = (Fight) entity.getFight();
	}

	public void addSystemLog(int type, Error error, String[] parameters) {
		addSystemLog(type, error.ordinal(), parameters, Thread.currentThread().getStackTrace());
	}

	public void addSystemLog(int type, int error) {
		addSystemLog(type, error, new String[0], Thread.currentThread().getStackTrace());
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
		if (type == AILog.WARNING)
			type = AILog.SWARNING;
		else if (type == AILog.ERROR)
			type = AILog.SERROR;
		else if (type == AILog.STANDARD)
			type = AILog.SSTANDARD;

		logs.addSystemLog(type, this.getErrorMessage(elements), key, parameters);
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
		return (LeekLog) logs;
	}

	public void addMessage(LeekMessage leekMessage) {
		if (mMessages.size() > 200) {
			return;
		}
		mMessages.add(leekMessage);
	}

	public List<Say> getSays() {
		return mSays;
	}

	public void setFight(Fight fight) {
		this.fight = fight;
		((LeekLog) logs).setLogs(fight.getState().getActions());
	}

	public void runTurn(int turn) {

		long startTime = System.nanoTime();

		try {

			resetCounter();
			mEntity = mInitialEntity;
			if (!staticInitialized) {
				staticInit();
				staticInitialized = true;
			}
			runIA(null);

		} catch (StackOverflowError e) { // On suppose que c'est normal, ça vient de l'utilisateur

			// e.printStackTrace(System.out);
			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, Error.STACKOVERFLOW, e.getStackTrace());
			fight.getState().statistics.stackOverflow(mEntity);
			fight.getState().statistics.error(mEntity);
			// Pas de rethrow

		} catch (ArithmeticException e) { // On suppose que c'est normal, ça vient de l'utilisateur

			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { e.getMessage() }, e.getStackTrace());
			fight.getState().statistics.error(mEntity);
			// Pas de rethrow

		} catch (ConcurrentModificationException e) { // On suppose que c'est normal, ça vient de l'utilisateur

			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, Error.MODIFICATION_DURING_ITERATION, new String[0], e.getStackTrace());
			fight.getState().statistics.error(mEntity);
			// Pas de rethrow

		} catch (LeekRunException e) { // Exception de l'utilisateur, normales

			handleLeekRunException(e);

		} catch (OutOfMemoryError e) { // Plus de RAM, Erreur critique, on tente de sauver les meubles

			fight.log(new ActionAIError(mEntity));
			fight.getState().statistics.error(mEntity);
			// Premierement on coupe l'IA incriminée
			valid = false;
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { "Out Of Memory" }, e.getStackTrace());
			System.out.println("Out Of Memory , Fight : " + fight.getId());
			fight.generator.exception(e, fight, mEntity.getFarmer(), getFile());
			throw e; // On rethrow tel quel

		} catch (RuntimeException e) { // Autre erreur, là c'est pas l'utilisateur

			// On regarde la cause si c'est pas une LeekRunException imbriquée
			// Exemple arraySort() relance une Runtime qui peut contenir une LeekRun
			if (e.getCause() instanceof LeekRunException) {
				handleLeekRunException((LeekRunException) e.getCause());
				return;
			}

			// IllegalArgumentException
			if (e instanceof IllegalArgumentException) {
				// Erreur de sort() incohérent : erreur utilisateur
				if (e.getMessage().equals("Comparison method violates its general contract!")) {
					fight.getState().statistics.error(mEntity);
					fight.log(new ActionAIError(mEntity));
					addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { e.getMessage() }, e.getStackTrace());
					return;
				}
			}

			fight.getState().statistics.error(mEntity);
			fight.log(new ActionAIError(mEntity));

			var error = throwableToError(e);
			try {
				addSystemLog(LeekLog.ERROR, error.type.ordinal(), error.parameters, e);
			} catch (LeekRunException e1) {
				fight.generator.exception(e1, fight, mEntity.getFarmer(), getFile());
			}
			// On signale l'erreur si elle est inconnue
			if (error.type == Error.UNKNOWN_ERROR && isFirstRuntimeError) {
				fight.generator.exception(e, fight, mEntity.getFarmer(), getFile());
				isFirstRuntimeError = false;
			}
			// throw e; // On rethrow tel quel

		} catch (Throwable e) { // Autre erreur, là c'est pas l'utilisateur

			System.out.println("[EntityAI] Unknown error (Throwable)");
			e.printStackTrace(System.out);
			fight.getState().statistics.error(mEntity);
			fight.log(new ActionAIError(mEntity));
			System.out.println("Erreur importante dans l'IA " + id + "  " + e.getMessage());
			e.printStackTrace();
			addSystemLog(LeekLog.ERROR, Error.AI_INTERRUPTED, new String[] { "Generator Error" }, e.getStackTrace());
			fight.generator.exception(e, fight, mEntity.getFarmer(), getFile());
			throw new RuntimeException("Erreur importante dans l'IA " + id + "  " + e.getMessage(), e);
		}

		mSays.clear();
		mMessages.clear();

		long endTime = System.nanoTime();
		mIARunTime += (endTime - startTime);
	}

	public void handleLeekRunException(LeekRunException e) {

		if (e.getError() == Error.ENTITY_DIED) {
			// OK, c'est normal
		} else {

			// e.printStackTrace(System.out);
			fight.log(new ActionAIError(mEntity));
			addSystemLog(LeekLog.ERROR, e.getError(), e.getParameters() == null ? new String[] { e.getMessage() } : e.getParameters(), e.getStackTrace());
			fight.getState().statistics.error(mEntity);

			if (e.getError() == Error.TOO_MUCH_OPERATIONS) {
				fight.getState().statistics.tooMuchOperations(mEntity);
				addSystemLog(LeekLog.STANDARD, Error.HELP_PAGE_LINK, new String[] { "too_much_ops" });
			} else if (e.getError() == Error.OUT_OF_MEMORY) {
				valid = false; // Si plus de RAM, IA désactivée pour tout le combat
			}
			// Pas de rethrow
		}
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
				var l = getState().getMap().getCell(integer(value));
				if (l == null) continue;
				ignore.add(l);
			}
		} else {
			for (var value : (LegacyArrayLeekValue) leeks_to_ignore) {
				Cell l = getState().getMap().getCell(integer(value.getValue()));
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
		return runIA(null);
	}

	@Override
	public Object runIA(Session session) throws LeekRunException {
		return null;
	}

	public List<LeekMessage> getMessages() {
		return mMessages;
	}

	public State getState() {
		return this.fight.getState();
	}

	public GenericArrayLeekValue getFeatureArray(EffectParameters feature) throws LeekRunException {
		var effect = newArray();
		effect.push(this, (long) feature.getId());
		effect.push(this, feature.getValue1());
		effect.push(this, feature.getValue1() + feature.getValue2());
		effect.push(this, (long) feature.getTurns());
		effect.push(this, (long) feature.getTargets());
		effect.push(this, (long) feature.getModifiers());
		return effect;
	}

	public GenericArrayLeekValue getEffectArray(Effect effect) throws LeekRunException {
		var retour = newArray();
		retour.push(this, (long) effect.getId());
		retour.push(this, (long) effect.value);
		retour.push(this, (long) effect.getCaster().getFId());
		retour.push(this, (long) effect.getTurns());
		retour.push(this, effect.isCritical());
		retour.push(this, effect.getAttack() == null ? 0l : (long) effect.getAttack().getItemId());
		retour.push(this, (long) effect.getTarget().getFId());
		retour.push(this, (long) effect.modifiers);
		return retour;
	}

	@Override
	public Date getDate() {
		return getState().getDate();
	}
}
