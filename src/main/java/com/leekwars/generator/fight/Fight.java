package com.leekwars.generator.fight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.action.Action;
import com.leekwars.generator.action.ActionAIError;
import com.leekwars.generator.action.ActionEndTurn;
import com.leekwars.generator.action.ActionEntityTurn;
import com.leekwars.generator.chips.Chip;
import com.leekwars.generator.effect.Effect;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.state.Entity;
import com.leekwars.generator.state.Order;
import com.leekwars.generator.state.State;
import com.leekwars.generator.fight.entity.BulbAI;
import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.FarmerLog;
import com.leekwars.generator.leek.LeekLog;

import leekscript.common.Error;
import leekscript.compiler.AIFile;
import leekscript.runner.values.FunctionLeekValue;

public class Fight {

	public final static String TAG = Fight.class.getSimpleName();

	// Maximum number of turns
	public final static int MAX_TURNS = 64;

	// Fight full types (type + context)
	public static final int TYPE_SOLO_GARDEN = 1;
	public static final int TYPE_SOLO_TEST = 2;
	public static final int TYPE_NORMAL_WHAT = 3;
	public static final int TYPE_TEAM_GARDEN = 4;
	public static final int TYPE_SOLO_CHALLENGE = 5;
	public static final int TYPE_FARMER_GARDEN = 6;
	public static final int TYPE_SOLO_TOURNAMENT = 7;
	public static final int TYPE_TEAM_TEST = 8;
	public static final int TYPE_FARMER_TOURNAMENT = 9;
	public static final int TYPE_TEAM_TOURNAMENT = 10;
	public static final int TYPE_FARMER_CHALLENGE = 11;
	public static final int TYPE_FARMER_TEST = 12;
	public static final int FULL_TYPE_BATTLE_ROYALE = 15;

	// Fight contexts
	public final static int CONTEXT_TEST = 0;
	public final static int CONTEXT_CHALLENGE = 1;
	public final static int CONTEXT_GARDEN = 2;
	public final static int CONTEXT_TOURNAMENT = 3;
	public final static int CONTEXT_BATTLE_ROYALE = 5;

	// Fight types
	public final static int TYPE_SOLO = 0;
	public final static int TYPE_FARMER = 1;
	public final static int TYPE_TEAM = 2;
	public final static int TYPE_BATTLE_ROYALE = 3;
	public final static int TYPE_BOSS = 4;

	// Flags
	public final static int FLAG_STATIC = 1;
	public final static int FLAG_PERFECT = 2;

	// Summon limit
	public final static int SUMMON_LIMIT = 8;

	public final static int MAX_LOG_COUNT = 5000;

	private int mWinteam = -1;
	public Generator generator;
	private int mId;
	private int mBoss;
	private int mStartFarmer = -1;
	private int max_turns = MAX_TURNS;
	public long executionTime = 0;
	FightListener listener;
	private State state = new State();


	public Fight(Generator generator) {
		this(generator, null);
	}

	public Fight(Generator generator, FightListener listener) {

		this.generator = generator;
		this.listener = listener;
	}

	public void addFlag(int team, int flag) {
		state.getTeams().get(team).addFlag(flag);
	}

	public HashSet<Integer> getFlags(int team) {
		return state.getTeams().get(team).getFlags();
	}

	public int getWinner() {
		return mWinteam;
	}

	public void setTeamID(int team, int id) {
		if (team < state.getTeams().size()) {
			state.getTeams().get(team).setID(id);
		}
	}

	public void setStartFarmer(int startFarmer) {
		mStartFarmer = startFarmer;
	}

	public int getStartFarmer() {
		return mStartFarmer;
	}

	public int getTeamID(int team) {
		if (team < state.getTeams().size()) {
			return state.getTeams().get(team).getID();
		}
		return -1;
	}

	public int getId() {
		return mId;
	}

	public int getBoss() {
		return mBoss;
	}

	public void log(Action log) {
		state.getActions().log(log);
	}

	public List<Entity> getTeamLeeks(int team) {
		List<Entity> leeks = new ArrayList<Entity>();
		if (team < state.getTeams().size()) {
			for (Entity e : state.getTeams().get(team).getEntities()) {
				if (!e.isDead() && e.getType() == Entity.TYPE_LEEK)
					leeks.add(e);
			}
		}
		return leeks;
	}

	public List<Entity> getTeamEntities(int team) {
		return state.getTeams().get(team).getEntities();
	}

	public Entity getEntity(int id) {
		return state.getEntity(id);
	}

	public Entity getEntity(long id) {
		return state.getEntity(id);
	}

	private boolean canStartFight() {
		if (state.getTeams().size() < 2) return false;
		return true;
	}

	public void startFight(boolean drawCheckLife) throws Exception {

		initFight();

		for (var entity : state.getEntities().values()) {

			// Build AI after the fight is ready (static init)
			var ai = EntityAI.build(this.generator, (AIFile) entity.getAIFile(), entity);
			entity.setAI(ai);
			((EntityAI) ai).setFight(this);
			((EntityAI) ai).init();
			((EntityAI) ai).getRandom().seed(state.getSeed());

			// Check all entities characteristics
			state.statistics.init(entity);
			state.statistics.characteristics(entity);

			// Start fight for entity
			entity.startFight();
		}

		Log.i(TAG, "Turn 1");

		// On lance les tours
		while (state.getOrder().getTurn() <= max_turns && state.getState() == State.STATE_RUNNING) {

			startTurn();

			if (state.getOrder().current() == null) {
				finishFight();
				break;
			}
		}
		// Si match nul
		if (state.getOrder().getTurn() == Fight.MAX_TURNS + 1) {
			finishFight();
		}

		// On supprime toutes les invocations
		var entities = state.getAllEntities(true);
		for (Entity e : entities) {
			if (e.isSummon())
				state.removeInvocation(e, true);
		}

		// Calcul de l'équipe gagnante
		computeWinner(drawCheckLife);

		state.statistics.endFight(state.getEntities().values());
		if (listener != null) {
			listener.newTurn(this);
		}
		state.getActions().addOpsAndTimes(state.statistics);
	}

	public void computeWinner(boolean drawCheckLife) {
		mWinteam = -1;
		int alive = 0;
		for (int t = 0; t < state.getTeams().size(); ++t) {
			if (!state.getTeams().get(t).isDead() && !state.getTeams().get(t).containsChest()) {
				alive++;
				mWinteam = t;
			}
		}
		if (alive != 1) {
			mWinteam = -1;
		}
		// Égalité : on regarde la vie des équipes
		if (mWinteam == -1 && drawCheckLife) {
			if (state.getTeams().get(0).getLife() > state.getTeams().get(1).getLife()) {
				mWinteam = 0;
			} else if (state.getTeams().get(1).getLife() > state.getTeams().get(0).getLife()) {
				mWinteam = 1;
			}
		}
	}

	public void initFight() throws FightException {

		if (state.getTeams().size() < 2) {
			throw new FightException(FightException.NOT_ENOUGHT_PLAYERS);
		}
		if (state.getTeams().get(0).size() == 0 || state.getTeams().get(1).size() == 0) {
			if (state.getContext() == Fight.CONTEXT_TOURNAMENT) {
				// Si c'est un tournoi faut un gagnant
				if (state.getTeams().get(0).size() == 0) {
					mWinteam = 1;
				} else {
					mWinteam = 0;
				}
			}
			throw new FightException(FightException.NOT_ENOUGHT_PLAYERS);
		}

		if (!canStartFight()) {
			throw new FightException(FightException.CANT_START_FIGHT);
		}

		// Init the state
		this.state.init();
	}

	public void finishFight() {
		state.setState(State.STATE_FINISHED);
	}

	public void startTurn() throws Exception {

		var current = state.getOrder().current();
		if (current == null) {
			return;
		}

		state.getActions().log(new ActionEntityTurn(current));
		if (listener != null) {
			listener.newTurn(this);
		}
		// Log.i(TAG, "Start turn of " + current.getName());

		current.startTurn();

		if (!current.isDead()) {

			var ai = (EntityAI) current.getAI();
			if (ai != null) {
				if (ai.isValid()) {
					ai.setEntity(current);

					// System.out.println("Run " + current.getName() + " ai...");
					long startTime = System.nanoTime();
					ai.runTurn(state.getOrder().getTurn());
					long endTime = System.nanoTime();

					state.statistics.addTimes(current, endTime - startTime, ai.operations());
					executionTime += endTime - startTime;
					current.addOperations(ai.operations());
				} else {
					// Add 'crash' action if AI is invalid
					if (getTurn() == 1) {
						((LeekLog) current.getLogs()).addSystemLog(LeekLog.SERROR, Error.INVALID_AI, new String[] { "Invalid AI" });
					}
					log(new ActionAIError(current));
					state.statistics.error(current);
				}
			} else {
				// Pas d'IA équipée : juste un warning
				((LeekLog) current.getLogs()).addSystemLog(LeekLog.SWARNING, Error.NO_AI_EQUIPPED);
			}
			current.endTurn();
			state.getActions().log(new ActionEndTurn(current));
		}
		state.endTurn();
	}

	public int useChip(Entity caster, Cell target, Chip template) {

		// Invocation mais sans IA
		if (template.getAttack().getEffectParametersByType(Effect.TYPE_SUMMON) != null) {
			((EntityAI) caster.getAI()).addSystemLog(LeekLog.WARNING, FarmerLog.BULB_WITHOUT_AI);
			((EntityAI) caster.getAI()).addSystemLog(LeekLog.STANDARD, Error.HELP_PAGE_LINK, new String[] { "summons" });
			return summonEntity(caster, target, template, null);
		}

		return state.useChip(caster, target, template);
	}

	public int summonEntity(Entity caster, Cell target, Chip template, FunctionLeekValue value) {

		int result = state.summonEntity(caster, target, template);

		// On assigne l'ia de l'invocation
		if (result > 0) {
			var summon = state.getLastEntity();
			summon.setFight(this);
			summon.setBirthTurn(getTurn());
			summon.setAI(new BulbAI(summon, (EntityAI) caster.getAI(), value));
		}

		return result;
	}

	public boolean generateCritical(Entity caster) {
		return state.getRandom().getDouble() < ((double) caster.getAgility() / 1000);
	}

	public int getTurn() {
		return state.getOrder().getTurn();
	}

	public Order getOrder() {
		return state.getOrder();
	}

	public void setId(int f) {
		mId = f;
	}

	public void setBoss(int b) {
		mBoss = b;
	}

	public static int getFightContext(int type) {
		if (type == TYPE_SOLO_GARDEN || type == TYPE_TEAM_GARDEN || type == TYPE_FARMER_GARDEN) {
			return CONTEXT_GARDEN;
		} else if (type == TYPE_SOLO_TEST || type == TYPE_TEAM_TEST || type == TYPE_FARMER_TEST) {
			return CONTEXT_TEST;
		} else if (type == TYPE_TEAM_TOURNAMENT || type == TYPE_SOLO_TOURNAMENT || type == TYPE_FARMER_TOURNAMENT) {
			return CONTEXT_TOURNAMENT;
		} else if (type == FULL_TYPE_BATTLE_ROYALE) {
			return CONTEXT_BATTLE_ROYALE;
		}
		return CONTEXT_CHALLENGE;
	}

	public static int getFightType(int type) {
		if (type == TYPE_SOLO_GARDEN || type == TYPE_SOLO_CHALLENGE || type == TYPE_SOLO_TOURNAMENT || type == TYPE_SOLO_TEST) {
			return Fight.TYPE_SOLO;
		} else if (type == TYPE_FARMER_GARDEN || type == TYPE_FARMER_TOURNAMENT || type == TYPE_FARMER_CHALLENGE || type == TYPE_FARMER_TEST) {
			return Fight.TYPE_FARMER;
		} else if (type == FULL_TYPE_BATTLE_ROYALE) {
			return Fight.TYPE_BATTLE_ROYALE;
		}
		return Fight.TYPE_TEAM;
	}

	public void setMaxTurns(int max_turns) {
		this.max_turns = max_turns;
	}

	public int getDuration() {
		return state.getOrder().getTurn();
	}

	public State getState() {
		return this.state;
	}

	public void setStatisticsManager(StatisticsManager statisticsManager) {
		this.state.setStatisticsManager(statisticsManager);
		statisticsManager.setGeneratorFight(this);
	}
}
